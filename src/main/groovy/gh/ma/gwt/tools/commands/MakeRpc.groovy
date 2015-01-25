package gh.ma.gwt.tools.commands

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException
import com.beust.jcommander.Parameters;

import gh.ma.gwt.tools.CodeWriter;
import groovy.util.logging.Slf4j;
import groovyjarjarcommonscli.CommandLineParser;
import groovyjarjarcommonscli.GnuParser
import groovyjarjarcommonscli.Options


@Slf4j
class MakeRpc extends CodeWriter {

    @Parameters(commandDescription="Make an rpc interface shell (sync and async), implementation shell and wire it up to the app")
    class CommandObj {
        @Parameter(names=["-c","--config"], description="The groovy-style config file to use.", required=true)
        String config
        
        @Parameter(names=["-a","--authenticated"], description="Indicates that the authenticated remote servlet base class (defined in the config file) should be used. Implies -x")
        boolean isAuthenticated

        @Parameter(names=["-x","--xsrf-safe"], description="Indicates that the XSRF-safe remote servlet base class (defined in the config file) should be used")
        boolean isXsrfProtected
        
        @Parameter(description="<rpcServiceName e.g. CalendarService> <serviceUrlSuffix e.g. v0/calendar> [<baseServlet>]")
        List<String> args
    }
    CommandObj commandObj = new CommandObj()
    def getCommandObj() {
        return commandObj
    }

    // These should be read from config file, but can be overridden on CLI
    String templateBaseFolder
    String rpcIfaceTemplateClass
    String rpcIfaceAsyncTemplateClass
    String rpcImplTemplateClass
    String srcBaseFolder = "src/main/java"
    String rpcInterfacePackageName
    String rpcImplPackageName
    String rpcBaseClassPackageName

    // Get from config
    String defaultBaseClass
    String xsrfSafeBaseClass
    String authenticatedBaseClass
    
    // Get from config values
    String ginjectorClass
    String ginjectorReplaceToken
    String ginjectorTemplate
    String ginModuleClass
    String ginModuleReplaceToken
    String ginModuleTemplate
    String servletModuleClass
    String servletModuleReplaceToken
    String servletModuleTemplate
    

    void run() {

        log.trace("Args: {}", commandObj.args)

        int numArgs = commandObj.args ? commandObj.args.size() : 0
        if (2>numArgs || 3<numArgs) {
            throw new ParameterException("Incorrect number of arguments")
        }

        ConfigObject config = readConfigFile(commandObj.config)

        getRequiredPropertiesFromConfig(this, config, [
            "templateBaseFolder",
            "rpcIfaceTemplateClass", "rpcIfaceAsyncTemplateClass", "rpcImplTemplateClass",
 
            "rpcInterfacePackageName", "rpcImplPackageName", "rpcBaseClassPackageName",

            "defaultBaseClass", "xsrfSafeBaseClass", "authenticatedBaseClass",
            
            "ginjectorClass", "ginjectorReplaceToken", "ginjectorTemplate",
            "ginModuleClass", "ginModuleReplaceToken", "ginModuleTemplate",
            "servletModuleClass", "servletModuleReplaceToken", "servletModuleTemplate",
            ])
        getOptionalPropertiesFromConfig(this, config, [
            "srcBaseDir", 
            ])
        
        VelocityEngine ve = new VelocityEngine();
        ve.init();

        def (String rpcServiceName, String serviceUrlSuffix, String rpcBaseClassName) = commandObj.args

        if (!rpcBaseClassName) {
            if (commandObj.isAuthenticated) {
                rpcBaseClassName = authenticatedBaseClass
            } else if (commandObj.isXsrfProtected) {
                rpcBaseClassName = xsrfSafeBaseClass
            } else {
                rpcBaseClassName = defaultBaseClass
            }
        }
        
        // If not overridden on cli, use these
        String rpcServiceNameAsync = rpcServiceName + "Async" // As per GWT rules
        String rpcServiceImplClass = rpcServiceName + "Impl"

        def context = [
            "rpcInterfacePackageName" : rpcInterfacePackageName,
            "rpcImplPackageName" : rpcImplPackageName,
            "rpcBaseClassPackageName" : rpcBaseClassPackageName,
            
            "rpcServiceNameAsync" : rpcServiceNameAsync,
            "rpcServiceImplClass" : rpcServiceImplClass,
            
            "rpcServiceName" : rpcServiceName,
            "rpcBaseClassName" : rpcBaseClassName,
            "serviceUrlSuffix" : serviceUrlSuffix,
        ]
        log.debug("Got / derived: {}", context);
        
        VelocityContext ctx = new VelocityContext(context);

        writeFile(ve, ctx, getFullPathFromBaseFolderAndFullClass(templateBaseFolder, rpcIfaceTemplateClass), getFullPathFromBaseFolderPackageAndClass(srcBaseFolder, rpcInterfacePackageName, rpcServiceName))
        writeFile(ve, ctx, getFullPathFromBaseFolderAndFullClass(templateBaseFolder, rpcIfaceAsyncTemplateClass), getFullPathFromBaseFolderPackageAndClass(srcBaseFolder, rpcInterfacePackageName, rpcServiceNameAsync))
        writeFile(ve, ctx, getFullPathFromBaseFolderAndFullClass(templateBaseFolder, rpcImplTemplateClass), getFullPathFromBaseFolderPackageAndClass(srcBaseFolder, rpcImplPackageName, rpcServiceImplClass))
        
        modifySourceFile(ve, ctx, srcBaseFolder, ginjectorClass, ginjectorReplaceToken, ginjectorTemplate)
        modifySourceFile(ve, ctx, srcBaseFolder, ginModuleClass, ginModuleReplaceToken, ginModuleTemplate)
        modifySourceFile(ve, ctx, srcBaseFolder, servletModuleClass, servletModuleReplaceToken, servletModuleTemplate)
    }
}
