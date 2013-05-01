/*
 * Copyright 2010-2011, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2012
 */
package org.sikuli.script;

import java.awt.*;
import java.io.*;
import javax.swing.*;
import org.apache.commons.cli.CommandLine;
import org.python.util.jython;

public class SikuliScript {

  private static CommandLine cmdLine;
	public static boolean runningInteractive = false;

  public SikuliScript() throws AWTException {
  }

  public static void main(String[] args) {
    int exitCode = 0;

    Settings.showJavaInfo();

    CommandArgs cmdArgs = new CommandArgs("SCRIPT");
    cmdLine = cmdArgs.getCommandLine(args);

    //TODO downward compatibel
    if (args.length > 0 && !args[0].startsWith("-")) {
			String[] pyargs = CommandArgs.getPyArgs(cmdLine);
			if (! pyargs[0].endsWith(".sikuli") && ! pyargs[0].endsWith(".skl")) {
				Debug.error("No runnable script found: " + pyargs[0]);
				exitCode = -2;
			} else{
				SikuliScriptRunner runner = new SikuliScriptRunner(pyargs);
				exitCode = runner.runPython(null);
			}
      Debug.info("You are using deprecated command line argument syntax!");
      if (Settings.InfoLogs) {
        cmdArgs.printHelp();
      }
      System.exit(exitCode);
    }

    if (cmdLine != null) {
      if (cmdLine.hasOption("h")) {
        cmdArgs.printHelp();
        return;
      }
      if (cmdLine.hasOption("i")) {
        String[] sargs = CommandArgs.getPyArgs(cmdLine);
        String[] jy_args = null;
        String[] iargs = {"-i", "-c",
					"from sikuli import *; SikuliScript.runningInteractive = True; "
					+ "print \"Hello, this is your interactive Sikuli (rules for interactive Python apply)\\n"
					+ "use the UP/DOWN arrow keys to walk through the input history\\n"
					+ "help()<enter> will output some basic Python information\\n"
					+ "shelp()<enter> will output some basic Sikuli information\\n"
					+ "... use ctrl-d to end the session\""};
        if (sargs.length > 0) {
          jy_args = new String[sargs.length + iargs.length];
          System.arraycopy(iargs, 0, jy_args, 0, iargs.length);
          System.arraycopy(sargs, 0, jy_args, iargs.length, sargs.length);
        } else {
          jy_args = iargs;
        }

				jython.main(jy_args);
        return;
      }
      if (cmdLine.hasOption("run")) {
        SikuliScriptRunner runner = new SikuliScriptRunner(CommandArgs.getPyArgs(cmdLine), "SCRIPT");
        System.exit(runner.runPython(null));
      } else if (cmdLine.hasOption("test")) {
				Debug.error("Sorry, support for option -t (test) not yet available - use X-1.0rc3");
	      System.exit(-2);
			}
		}
		Debug.error("Nothing to do! No valid arguments on commandline!");
		cmdArgs.printHelp();
  }

  public static void shelp() {
		if (SikuliScript.runningInteractive) {
			System.out.println("**** this might be helpful ****");
			System.out.println(
				"-- execute a line of code by pressing <enter>\n" +
				"-- separate more than one statement on a line using ;\n" +
				"-- Unlike the iDE, this command window will not vanish, when using a Sikuli feature\n" +
				"   so take care, that all you need is visible on the screen\n" +
				"-- to create an image interactively:\n" +
				"img = capture()\n" +
				"-- use a captured image later:\n" +
				"click(img)"
				);
		}
	}

	public static void setShowActions(boolean flag) {
    Settings.ShowActions = flag;
    if (flag) {
      if (Settings.MoveMouseDelay < 1f) {
        Settings.MoveMouseDelay = 1f;
      }
    }
  }

  public static String input(String msg) {
    return (String) JOptionPane.showInputDialog(msg);
  }

  public static String input(String msg, String preset) {
    return (String) JOptionPane.showInputDialog(msg, preset);
  }

  public static int switchApp(String appName) {
    if (App.focus(appName) != null) {
      return 0;
    }
    return -1;
  }

  public static int openApp(String appName) {
    if (App.open(appName) != null) {
      return 0;
    }
    return -1;
  }

  public static int closeApp(String appName) {
    return App.close(appName);
  }

  public static void popup(String message, String title) {
    JOptionPane.showMessageDialog(null, message,
            title, JOptionPane.PLAIN_MESSAGE);
  }

  public static void popup(String message) {
    popup(message, "Sikuli");
  }

  public static String run(String cmdline) {
//TODO: improve run command
    String lines = "";
    try {
      String line;
      Process p = Runtime.getRuntime().exec(cmdline);
      BufferedReader input =
              new BufferedReader(new InputStreamReader(p.getInputStream()));
      while ((line = input.readLine()) != null) {
        lines = lines + '\n' + line;
      }
    } catch (Exception err) {
      err.printStackTrace();
    }
    return lines;
  }
}
