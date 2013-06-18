/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.ServiceLoader;
import javax.swing.JOptionPane;
import org.apache.commons.cli.CommandLine;

/**
 * Contains the main class
 */
public class SikuliScript {

  /**
   * The ScriptRunner that is used to execute the script.
   */
  private static IScriptRunner runner;
  private static File imagePath;
  private static Boolean runAsTest;

  /**
   * Main method
   *
   * @param args passed arguments
   */
  public static void main(String[] args) {

    Settings.showJavaInfo();

    for (int i = 0; i < args.length; i++) {
      Debug.log(2, "arg %d: %s", i + 1, args[i]);
    }
    CommandArgs cmdArgs = new CommandArgs("SCRIPT");
    CommandLine cmdLine = cmdArgs.getCommandLine(args);

    if (cmdLine == null || cmdLine.getOptions().length == 0) {
      Debug.error("Did not find any valid options on command line!");
      System.exit(1);
    }

    // check if any commandline args were loaded and print std help and runner specific help
    if (cmdLine == null) {
      Debug.error("Nothing to do! No valid arguments on commandline!");
      cmdArgs.printHelp();
      System.exit(1);
    }

    if (cmdLine.hasOption(CommandArgsEnum.SCRIPTRUNNER.shortname())) {
      runner = getScriptRunner(cmdLine.getOptionValue(CommandArgsEnum.SCRIPTRUNNER.longname()),
                                   null, args);
    } else {
      runner = null;
    }

    // print help
    if (cmdLine.hasOption(CommandArgsEnum.HELP.shortname())) {
      cmdArgs.printHelp();
      if (runner != null) {
        System.out.println(runner.getCommandLineHelp());
      }
      System.exit(1);
    }

//TODO    if (cmdLine.hasOption(CommandArgsEnum.IMAGEPATH.shortname())) {
    if (false) {
//      imagePath = getScriptRunner(cmdLine.getOptionValue(CommandArgsEnum.IMAGEPATH.longname()), null, args);
    } else {
      imagePath = null;
    }

    // start interactive session
    if (cmdLine.hasOption(CommandArgsEnum.INTERACTIVE.shortname())) {
      int exitCode = 0;
      if (runner == null) {
        //TODO want to overload the -i option
        //(-i is Jython -i xxx is xxx interactive, together with -r/-t it defines the runner
        runner = getScriptRunner("jython", null, args);
      }
      exitCode = runner.runInteractive(args);
      runner.close();
      ScreenHighlighter.closeAll();
      System.exit(exitCode);
    }

    // start script execution
    String givenScriptName = null;
    runAsTest = false;
    if (cmdLine.hasOption(CommandArgsEnum.RUN.shortname())) {
      givenScriptName = cmdLine.getOptionValue(CommandArgsEnum.RUN.longname());
    } else if (cmdLine.hasOption(CommandArgsEnum.TEST.shortname())) {
      givenScriptName = cmdLine.getOptionValue(CommandArgsEnum.TEST.longname());
      runAsTest = true;
    }
    if (givenScriptName != null) {
      File script = FileManager.getScriptFile(new File(givenScriptName), runner, args);
      if (script == null) {
        System.exit(1);
      }
      if (imagePath == null) {
        imagePath = FileManager.resolveImagePath(script);
      }
      ImageLocator.setBundlePath(imagePath.getAbsolutePath());
      int exitCode = runAsTest ?
                       runner.runTest(script, imagePath,
                       cmdLine.getOptionValues(CommandArgsEnum.ARGS.longname()), null) :
                       runner.runScript(script, imagePath,
                       cmdLine.getOptionValues(CommandArgsEnum.ARGS.longname()), null);
      runner.close();
      System.exit(exitCode);
    } else {
      Debug.error("Nothing to do according to the given commandline options!");
      cmdArgs.printHelp();
      if (runner != null) {
        System.out.println(runner.getCommandLineHelp());
      }
      System.exit(1);
    }
  }

  public static IScriptRunner setRunner(IScriptRunner _runner) {
    runner = _runner;
    return runner;
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
    return JOptionPane.showInputDialog(msg);
  }

  public static String input(String msg, String preset) {
    return JOptionPane.showInputDialog(msg, preset);
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

  /**
   * Prints the interactive help from the ScriptRunner.
   */
  public static void shelp() {
    System.out.println(runner.getInteractiveHelp());
  }

  /**
   * Finds a ScriptRunner implementation to execute the script.
   *
   * @param name Name of the ScriptRunner, might be null (then type is used)
   * @param ending fileending of script to run
   * @return first ScriptRunner with matching name or file ending, null if none found
   */
  public static IScriptRunner getScriptRunner(String name, String ending, String[] args) {
    IScriptRunner runner = null;
    ServiceLoader<IScriptRunner> loader = ServiceLoader.load(IScriptRunner.class);
    Iterator<IScriptRunner> scriptRunnerIterator = loader.iterator();
    while (scriptRunnerIterator.hasNext()) {
      IScriptRunner currentRunner = scriptRunnerIterator.next();
      if ((name != null && currentRunner.getName().toLowerCase().equals(name.toLowerCase())) ||
              (ending != null && currentRunner.hasFileEnding(ending) != null)) {
        runner = currentRunner;
        runner.init(args);
        break;
      }
    }
    if (runner == null) {
      Debug.error("Could not load any script runner!");
      System.exit(1);
    }
    return runner;
  }
}
