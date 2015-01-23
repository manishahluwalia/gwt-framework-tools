package gh.ma.gwt.tools

abstract class ToolsBase {
    abstract String getCommandName()
    
    abstract CliBuilder getCliBuilder()
    
    abstract boolean processOptions(OptionAccessor options, String[] args)
    
    abstract void run() 
}
