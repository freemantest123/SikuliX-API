package org.sikuli.script;

/**
 * Enum that stores the info about the commandline args
 */
public enum CommandArgsEnum {
    /** Shows the help */
    HELP("help", "h", null, "print this help message"),

    /** Starts an interactive session */
    INTERACTIVE("interactive", "i", null, "start interactive Sikuli session with the available ScriptRunner"),

    /** Runs the script */
    RUN("run", "r", "foobar.sikuli", "run script"),

    /** Runs the script as testcase */
    TEST("test", "t", "foobar.sikuli", "runs a unittest with the available ScriptRunner"),

    /** Runs the script with the specified ScriptRunner */
    SCRIPTRUNNER("scriptrunner", null, "scriptrunner", "ScriptRunner that will execute the script"),

    /** Prints all errormessages to stdout */
    STDERR("stderr", "s", null, "print runtime errors to stderr instead of popping up a message box"),

    /** Preloads script in IDE */
    LOAD("load", "l", "one or more foobar.sikuli", "peload scripts in IDE"),

    /** Arguments to be passed to the Script */
    ARGS("args", null, "arguments", "arguments passed to the script as parameters");

    /** Longname of the parameter */
    private String longname;

    /** Shortname of the parameter */
    private String shortname;

    /** The param name */
    private String argname;

    /** The description */
    private String description;

    /**
     * Returns the long name
     * @return Longname of the parameter
     */
    public String longname() {
        return longname;
    }

    /**
     * Returns the short name
     * @return Shortname of the parameter
     */
    public String shortname() {
        return shortname;
    }

    /**
     * Returns the argname
     * @return The argname
     */
    public String argname() {
        return argname;
    }

    /**
     * Description for the param
     */
    public String description() {
        return description;
    }

    /**
     * Private constructor for class CommandArgsEnum.
     * @param longname The long name for the param
     * @param shortname The short name for the param
     * @param argname The argname
     * @param description The description for the Command Args
     */
    private CommandArgsEnum(String longname, String shortname, String argname, String description) {
        this.longname = longname;
        this.shortname = shortname;
        this.argname = argname;
        this.description = description;
    }
}
