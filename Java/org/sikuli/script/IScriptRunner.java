package org.sikuli.script;

/**
 * Interface for ScriptRunners like Jython.
 */
public interface IScriptRunner {

    /**
     * Can be used to initialize the ScriptRunner
     * @param param Parameters to be passed to the ScriptRunner
     */
    public void init(String[] param);

    /**
     * Executes the Script.
     * @param param Parameters to be passed to the ScriptRunner
     * @return exitcode for the script execution
     */
    public int run(String[] param);

    /**
     * Starts an interactive session with the scriptrunner.
     * @param param Parameters to be passed to the ScriptRunner
     * @return exitcode of the interactive session
     */
    public int runInteractive(String[] param);
}
