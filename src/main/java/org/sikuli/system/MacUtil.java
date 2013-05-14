/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.system;

import java.awt.Rectangle;
import java.awt.Window;
import javax.swing.JOptionPane;
import org.sikuli.script.Region;
import org.sikuli.script.Debug;
import org.sikuli.script.FileManager;

public class MacUtil implements OSUtil {

  private static boolean _askedToEnableAX = false;

  static {
    FileManager.loadLibrary("MacUtil");
  }

	@Override
  public int switchApp(String appName) {
    return openApp(appName);
  }

	@Override
  public int switchApp(int pid, int num) {
    return -1;
  }

  // ignore winNum on Mac
	@Override
  public int switchApp(String appName, int winNum) {
    return openApp(appName);
  }

	@Override
  public int openApp(String appName) {
    Debug.history("openApp: \"" + appName + "\"");
    if (_openApp(appName)) {
      return 0;
    }
    return -1;
  }

	@Override
  public int closeApp(String appName) {
    Debug.history("closeApp: \"" + appName + "\"");
    try {
      String cmd[] = {"sh", "-c",
        "ps aux |  grep \"" + appName + "\" | awk '{print $2}' | xargs kill"};
      Debug.history("closeApp: " + appName);
      Process p = Runtime.getRuntime().exec(cmd);
      p.waitFor();
      return p.exitValue();
    } catch (Exception e) {
      return -1;
    }
  }

	@Override
  public int closeApp(int pid) {
    return -1;
  }

  private void checkAxEnabled(String name) {
    if (!isAxEnabled()) {
      Debug.error(name + " requires Accessibility API to be enabled!");
      if (_askedToEnableAX) {
        return;
      }
      int ret = JOptionPane.showConfirmDialog(null,
              "You need to enable Accessibility API to use the function \""
              + name + "\".\n"
              + "Should I open te System Preferences for you?",
              "Accessibility API not enabled",
              JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
      if (ret == JOptionPane.YES_OPTION) {
        openAxSetting();
        JOptionPane.showMessageDialog(null,
                "Check \"Enable access for assistant devices\""
                + "in the System Preferences\n and then close this dialog.",
                "Enable Accessibility API", JOptionPane.INFORMATION_MESSAGE);
      }
      _askedToEnableAX = true;
    }
  }

	@Override
  public Region getWindow(String appName, int winNum) {
    checkAxEnabled("getWindow");
    int pid = getPID(appName);
    return getWindow(pid, winNum);
  }

	@Override
  public Region getWindow(String appName) {
    return getWindow(appName, 0);
  }

	@Override
  public Region getWindow(int pid) {
    return getWindow(pid, 0);
  }

	@Override
  public Region getWindow(int pid, int winNum) {
    Rectangle rect = getRegion(pid, winNum);
    if (rect != null) {
      return Region.create(rect);
    }
    return null;

  }

	@Override
  public Region getFocusedWindow() {
    checkAxEnabled("getFocusedWindow");
    Rectangle rect = getFocusedRegion();
    if (rect != null) {
      return Region.create(rect);
    }
    return null;
  }

	@Override
  public native void bringWindowToFront(Window win, boolean ignoreMouse);

  public static native boolean _openApp(String appName);

  public static native int getPID(String appName);

  public static native Rectangle getRegion(int pid, int winNum);

  public static native Rectangle getFocusedRegion();

  public static native boolean isAxEnabled();

  public static native void openAxSetting();
}
