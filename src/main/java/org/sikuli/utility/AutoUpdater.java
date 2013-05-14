/*
 * Copyright 2010-2011, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2012
 */
package org.sikuli.utility;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.event.*;
import org.sikuli.script.Debug;
import org.sikuli.script.FileManager;
import org.sikuli.script.Settings;

public class AutoUpdater {

  private String version, details;
  private int beta = 0;
  private String server = "";
  public boolean newMajor = false;
  public boolean newBeta = false;
  public boolean notAvailable = false;

  public String getServer() {
    return server;
  }

  public String getVersion() {
    if (newMajor) {
      return version;
    } else if (newBeta) {
      return version + "-Beta" + beta;
    }
    return "";
  }

  public String getDetails() {
    return details;
  }

  public boolean checkUpdate() {
    for (String s : Settings.ServerList) {
      try {
        if (checkUpdate(s)) {
          if (
          (isNewer(version, Settings.SikuliVersion) && beta == 0) ||
          (!isNewer(version, Settings.SikuliVersion)) && beta == 0 && Settings.SikuliVersionBetaN > 0)
          {
            Debug.log(3, "A new major version is available: " + version);
            newMajor = true;
          }
          if ( (isNewer(version, Settings.SikuliVersion) && beta > 0) ||
               (!isNewer(version, Settings.SikuliVersion) && beta > Settings.SikuliVersionBetaN)) {
            Debug.log(3, "A new beta version is available: " + version + "-Beta" + beta);
            newBeta = true;
          }
        }
      } catch (Exception e) {
        Debug.log(3, "No version info available at " + s);
        notAvailable = true;
      }
      if (newMajor || newBeta) {
        return true;
      }
    }
    return false;
  }

  private boolean checkUpdate(String s) throws IOException, MalformedURLException {
    URL url = new URL(s + "/latestversion");
    url.openConnection();
    URLConnection conn = url.openConnection();
    BufferedReader in = new BufferedReader(
            new InputStreamReader(conn.getInputStream()));
    String line;
    if ((line = in.readLine()) != null) {
      String[] vinfo = line.trim().split(" ");
      version = vinfo[0];
      if (vinfo.length > 1) {
        beta = Integer.parseInt(vinfo[1]);
      }
      details = "";
      if ((line = in.readLine()) != null) {
        if (line.startsWith("DOWNLOAD")) {
          server = line.split(" ")[1];
          details += "Pls. download at: " + server + "<br />";
          details += "-------------------------------------------------------------------------";
          details += "<br /><br />";
        } else {
          details += line;
        }
      }
      while ((line = in.readLine()) != null) {
        details += line;
      }
      return true;
    }
    return false;
  }

  private boolean isNewer(String v1, String v2) {
    return v1.compareTo(v2) > 0;
  }

  public void showUpdateFrame(String title, String text) {
    UpdateFrame f = new UpdateFrame(title, text, server);
  }
}

class UpdateFrame extends JFrame {
  public UpdateFrame(String title, String text, String server) {
    setTitle(title);
    setSize(300, 200);
    setLocationRelativeTo(getRootPane());
    Container cp = getContentPane();
    cp.setLayout(new BorderLayout());
    final JEditorPane p = new JEditorPane("text/html", text);
    p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    p.setEditable(false);
    p.addHyperlinkListener(new HyperlinkListener() {
      public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED)) {
          try {
            FileManager.openURL(e.getURL().toString());
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        }
      }
    });
    cp.add(new JScrollPane(p), BorderLayout.CENTER);
    JPanel buttonPane = new JPanel();
    JButton btnOK = new JButton("ok");
    btnOK.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        UpdateFrame.this.dispose();
      }
    });
    JButton btnGo = new JButton("download");
    btnGo.setToolTipText(server);
    btnGo.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        FileManager.openURL(((JButton) ae.getSource()).getToolTipText());
        UpdateFrame.this.dispose();
      }
    });
    buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
    buttonPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
    buttonPane.add(Box.createHorizontalGlue());
    buttonPane.add(btnGo);
    buttonPane.add(btnOK);
    buttonPane.add(Box.createHorizontalGlue());
    getRootPane().setDefaultButton(btnOK);

    cp.add(buttonPane, BorderLayout.PAGE_END);
    cp.doLayout();
    pack();

    setVisible(true);
    btnOK.requestFocus();
  }
}
