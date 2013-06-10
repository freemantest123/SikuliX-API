package org.sikuli.script;

import java.io.File;

/**
 * Interface for ScriptRunners like Jython.
 */
public interface IScriptRunner {

  /**
   * Can be used to initialize the ScriptRunner. This method is called at the beginning of program
   * execution. The given parameters can be used to parse any ScriptRunner specific custom options.
   *
   * @param args All arguments that were passed to the main-method
   */
  public void init(String[] args);

  /**
   * Executes the Script.
   *
   * @param scriptfile File containing the script
   * @param imagedirectory Directory containing the images
   * @param scriptArgs Arguments to be passed directly to the script with --args
   * @return exitcode for the script execution
   */
  public int runScript(File scriptfile, File imagedirectory, String[] scriptArgs);

  /**
   * Executes the Script as Test.
   *
   * @param scriptfile File containing the script
   * @param imagedirectory Directory containing the images
   * @param scriptArgs Arguments to be passed directly to the script with --args
   * @return exitcode for the script execution
   */
  public int runTest(File scriptfile, File imagedirectory, String[] scriptArgs);

  /**
   * Starts an interactive session with the scriptrunner.
   *
   * @param scriptArgs Arguments to be passed directly to the script with --args
   * @return exitcode of the interactive session
   */
  public int runInteractive(String[] scriptArgs);

  /**
   * Gets the scriptrunner specific help text to print on stdout.
   *
   * @return A helping description about how to use the scriptrunner
   */
  public String getCommandLineHelp();

  /**
   * Gets the help text that is shown if the user runs "shelp()" in interactive mode
   *
   * @return The helptext
   */
  public String getInteractiveHelp();

  /**
   * Gets the name of the ScriptRunner. Should be unique. This value is needed to distinguish
   * between different ScriptRunners.
   *
   * @return Name to identify the ScriptRunner
   */
  public String getName();

  /**
   * returns the list of possible script file endings, first is the default
   * @return
   */
  public String[] getFileEndings();

  /**
   * Is executed before Sikuli closes. Can be used to cleanup the ScriptRunner
   */
  public void close();
}
