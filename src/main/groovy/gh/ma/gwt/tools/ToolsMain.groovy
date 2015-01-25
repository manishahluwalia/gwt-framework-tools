package gh.ma.gwt.tools

import org.slf4j.LoggerFactory;

import gh.ma.gwt.tools.commands.MakePlace;
import gh.ma.gwt.tools.commands.MakeRpc;
import groovy.util.logging.Slf4j;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

@Slf4j
class ToolsMain {

    def commands = [
        "help" : new HelpCommand(),
        "makePlace" : new MakePlace(),
        "makeRpc" : new MakeRpc()
    ]

    @Parameters(commandDescription="ToolsMain")
    class Args {
        @Parameter(description="command arguments")
        List<String> args;

        @Parameter(names="--debug", description="Turn on debug logging")
        boolean debug

        @Parameter(names="--trace", description="Turn on trace logging", hidden=true)
        boolean trace
    }

    class HelpCommand {
        @Parameters(commandDescription ="Show help")
        class CommandObj {
            @Parameter(description="<help targets...>")
            List<String> targets;
        }
        CommandObj commandObj = new CommandObj()
        def getCommandObj() {
            return commandObj
        }
        def run() {
            if (!commandObj.targets || 0==commandObj.targets.size()) {
                jc.usage()
            } else {
                commandObj.targets.each { t ->
                    def cmd = commands[t]
                    if (cmd) {
                        jc.usage(t)
                    } else {
                        log.error("Unknown help target: {}. Ignoring", t)
                    }
                }
            }
        }
    }

    JCommander jc
    Args mainArgs = new Args()
    ToolsMain() {
        jc = new JCommander(mainArgs)
        jc.setProgramName("")
        
        commands.each { cmdName, cmdObj ->
            jc.addCommand(cmdName, cmdObj.getCommandObj())
        }
    }

    void run(args) {
        try {
            jc.parse(args)
        } catch (ParameterException e) {
            log.error("Incorrect usage")
            jc.usage()
            throw e
        }

        if (mainArgs.trace) {
            ((Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.TRACE)
            log.trace("Turning on trace logging")
        } else if (mainArgs.debug) {
            ((Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.DEBUG)
            log.debug("Turning on debug logging")
        }

        def cmdName = jc.getParsedCommand()
        if (null==cmdName) {
            log.error("Invalid or missing command name")
            jc.usage()
            throw new RuntimeException()
        }

        def cmd = commands[cmdName]
        try {
            cmd.run()
        } catch (ParameterException e) {
            log.error("Incorrect usage: {}", e.getMessage())
            jc.usage(cmdName)
            throw e
        }
    }

    static main(args) {
        new ToolsMain().run(args)
    }

}
