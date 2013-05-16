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
    private static IScriptRunner runner = null;

    /**
     * Finds a ScriptRunner implementation to execute the script.
     * @param name Name of the ScriptRunner, if more than one runner is available. If there is only one ScriptRunner available, this parameter can be null.
     * @return The found ScriptRunner. If none or <b> more than one </b> matching ScriptRunner is found, null is returned.
     */
    private static IScriptRunner getScriptRunner(String name) {
        ServiceLoader<IScriptRunner> loader = ServiceLoader.load(IScriptRunner.class);

        Iterator<IScriptRunner> scriptRunnerIterator = loader.iterator();

        while (scriptRunnerIterator.hasNext()) {
            IScriptRunner currentRunner = scriptRunnerIterator.next();

            // if there is a name, return the runner with the matching name
            if (name != null && currentRunner.getName().toLowerCase().equals(name.toLowerCase())) {
                return currentRunner;
            }
            // no name and/or no match, runner is saved temporary
            if (runner == null) {
                runner = currentRunner;
            } else {
                // more than one ScriptRunner was found, but names didn't match, null is returned because we cannot decide which one to use
                return null;
            }
        }
        return runner;
    }

    /**
     * Main method
     * @param args passed arguments
     */
    public static void main(String[] args) {

        Settings.showJavaInfo();

        CommandArgs cmdArgs = new CommandArgs("SCRIPT");
        CommandLine cmdLine = cmdArgs.getCommandLine(args);

        IScriptRunner runner = null;

        if (cmdLine != null) { // Load the specified scriptrunner if possible
            runner = getScriptRunner(cmdLine.getOptionValue(CommandArgsEnum.SCRIPTRUNNER.longname()));
        }

        if (runner == null) { // Check if a scriptrunner could be loaded
            Debug.error("None or more than one ScriptRunner found! Please check if a ScriptRunner is available and specify the ScriptRunner if more than one is available.");
            System.exit(1);
        }

        runner.init(args); // init scriptrunner

        if (cmdLine == null) { // check if any commandline args were loaded and print std help and runner specific help
            Debug.error("Nothing to do! No valid arguments on commandline!");
            cmdArgs.printHelp();
            System.out.println(runner.getCommandLineHelp());
            System.exit(1);
        }

        // print help
        if (cmdLine.hasOption(CommandArgsEnum.HELP.shortname())) {
            cmdArgs.printHelp();
            System.out.println(runner.getCommandLineHelp());
            System.exit(1);
        }

        // start interactive session
        if (cmdLine.hasOption(CommandArgsEnum.INTERACTIVE.shortname())) {
            int exitCode = runner.runInteractive(cmdLine.getOptionValues(CommandArgsEnum.ARGS.longname()));
            runner.close();
            System.exit(exitCode);
        }

        // start script execution
        if (cmdLine.hasOption(CommandArgsEnum.RUN.shortname())) {
            File runFile = new File(cmdLine.getOptionValue(CommandArgsEnum.RUN.longname()));
            if (!runFile.exists() || (runFile.isDirectory() && !runFile.getName().endsWith(".sikuli"))) {
                Debug.error("Script File "+runFile.getAbsolutePath()+" does not exist or is a directory but does not have a name ending with .sikuli");
                System.exit(1);
            }

            File imagePath = resolveImagePath(runFile);
            ImageLocator.setBundlePath(imagePath.getAbsolutePath());

            int exitCode = runner.runScript(runFile, imagePath, cmdLine.getOptionValues(CommandArgsEnum.ARGS.longname()));
            runner.close();
            System.exit(exitCode);
        }

        // start script as testcase
        if (cmdLine.hasOption(CommandArgsEnum.TEST.shortname())) {
            File runFile = new File(cmdLine.getOptionValue(CommandArgsEnum.RUN.longname()));
            if (!runFile.exists() || (runFile.isDirectory() && runFile.getName().endsWith(".sikuli"))) {
                Debug.error("Script File does not exist or is a directory but does not have a name ending with .sikuli");
                System.exit(1);
            }

            File imagePath = resolveImagePath(runFile);
            ImageLocator.setBundlePath(imagePath.getAbsolutePath());

            int exitCode = runner.runTest(runFile, resolveImagePath(runFile), cmdLine.getOptionValues(CommandArgsEnum.ARGS.longname()));
            runner.close();
            System.exit(exitCode);
        }
    }

    /**
     * Returns the directory that contains the images used by the ScriptRunner.
     * @param scriptFile The file containing the script.
     * @return The directory containing the images.
     */
    public static File resolveImagePath(File scriptFile) {
        if (!scriptFile.isDirectory()) {
            return scriptFile.getParentFile();
        }

        return scriptFile;
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
}
