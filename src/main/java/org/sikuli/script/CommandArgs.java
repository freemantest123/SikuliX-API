/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.script;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class CommandArgs {

  Options _options;
  String _callerType;

  public static boolean isIDE(String callerType) {
    return ("IDE".equals(callerType));
  }

  public static boolean isScript(String callerType) {
    return ("SCRIPT".equals(callerType));
  }

  public static boolean isOther(String callerType) {
    return (!isIDE(callerType) && !isScript(callerType));
  }

  public CommandArgs(String type) {
    if (!isIDE(type) && !isScript(type)) {
      Debug.error("Commandline Parser not configured for " + type);
      _callerType = "OTHER";
    } else {
      _callerType = type;
    }
    init();
  }

  public CommandLine getCommandLine(String[] args) {
    CommandLineParser parser = new PosixParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(_options, args, true);
    } catch (ParseException exp) {
      Debug.error(exp.getMessage());
    }
    return cmd;
  }

  /**
   * Adds all options to the Options object
   */
  @SuppressWarnings("static-access")
  private void init() {
    _options = new Options();
    _options.addOption(CommandArgsEnum.HELP.shortname(), CommandArgsEnum.HELP.longname(), false, CommandArgsEnum.HELP.description());
    if (isIDE(_callerType)) {
      _options.addOption(CommandArgsEnum.STDERR.shortname(), CommandArgsEnum.STDERR.longname(), false, CommandArgsEnum.STDERR.description());
      _options.addOption(
              OptionBuilder.withLongOpt(CommandArgsEnum.LOAD.longname())
              .withDescription(CommandArgsEnum.LOAD.description())
              .hasOptionalArgs()
              .withArgName(CommandArgsEnum.LOAD.argname())
              .create(CommandArgsEnum.LOAD.shortname().charAt(0)));
    }
    if (isScript(_callerType)) {
      _options.addOption(
              CommandArgsEnum.INTERACTIVE.shortname(),
              CommandArgsEnum.INTERACTIVE.longname(),
              false,
              CommandArgsEnum.INTERACTIVE.description());
      _options.addOption(
              OptionBuilder.withLongOpt(CommandArgsEnum.TEST.longname())
              .hasArg()
              .withArgName(CommandArgsEnum.TEST.argname())
              .withDescription(CommandArgsEnum.TEST.description())
              .create(CommandArgsEnum.TEST.shortname().charAt(0)));
      _options.addOption(
              OptionBuilder.withLongOpt(CommandArgsEnum.RUN.longname())
              .hasArg()
              .withArgName(CommandArgsEnum.RUN.argname())
              .withDescription(CommandArgsEnum.RUN.description())
              .create(CommandArgsEnum.RUN.shortname().charAt(0)));
      _options.addOption(
              OptionBuilder.withLongOpt(CommandArgsEnum.SCRIPTRUNNER.longname())
              .hasArg()
              .withArgName(CommandArgsEnum.SCRIPTRUNNER.argname())
              .withDescription(CommandArgsEnum.SCRIPTRUNNER.description())
              .create(CommandArgsEnum.SCRIPTRUNNER.shortname().charAt(0)));
      _options.addOption(
              OptionBuilder.withLongOpt(CommandArgsEnum.ARGS.longname())
              .withArgName(CommandArgsEnum.ARGS.argname())
              .withDescription(CommandArgsEnum.ARGS.description())
              .create());
    }
  }

  /**
   * Prints the help
   */
  public void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    if (isScript(_callerType)) {
      formatter.printHelp(80, "\n",
              "----- Running Sikuli script using sikuli-script.jar "
              + "---------------------------",
              _options,
              "-----\n<foobar.sikuli>\n"
              + "path relative to current working directory or absolute path\n"
              + "though deprecated: so called executables .skl can be used too\n"
              + "-------------------------------------------------------------",
              true);
    } else if (isIDE(_callerType)) {
      formatter.printHelp("Sikuli-IDE", _options, true);
    } else {
      formatter.printHelp("--?????--", _options, true);
    }
  }
}
