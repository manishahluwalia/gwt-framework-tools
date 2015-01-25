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
class MakePlace extends CodeWriter {

    @Parameters(commandDescription="Make a place, view iface, view impl and activity and wire it up to the app")
    class CommandObj {
        @Parameter(names=["-c","--config"], description="The groovy-style config file to use.", required=true)
        String config

        @Parameter(names="--prettyName", description="The pretty name of the place")
        String prettyName

        @Parameter(description="<placeName> <basePlace>")
        List<String> args
    }
    CommandObj commandObj = new CommandObj()
    def getCommandObj() {
        return commandObj
    }

    // These should be read from config file, but can be overridden on CLI
    String templateBaseFolder
    String placeTemplateClass
    String viewIfaceTemplateClass
    String viewImplTemplateClass
    String uiBinderTemplateFile
    String activityTemplateClass
    String srcBaseFolder = "src/main/java"
    String placePackageName
    String viewIfacePackageName
    String viewImplPackageName
    String uiBinderPackageName
    String activityPackageName
    String placeBaseClassPackageName

    // Get from config values
    String placeHistoryMapper
    String placeHistoryMapperReplaceToken
    String placeHistoryMapperTemplate
    String viewMasterIface
    String viewMasterIfaceReplaceToken
    String viewMasterIfaceTemplate
    String viewMasterImpl
    String viewMasterImplReplaceToken
    String viewMasterImplTemplate
    

    void run() {

        int numArgs = commandObj.args ? commandObj.args.size() : 0
        if (2!=numArgs) {
            throw new ParameterException("Incorrect number of arguments")
        }

        ConfigObject config = readConfigFile(commandObj.config)

        getRequiredPropertiesFromConfig(this, config, [
            "templateBaseFolder",
            "placeTemplateClass", "viewIfaceTemplateClass", "viewImplTemplateClass",
            "uiBinderTemplateFile", "activityTemplateClass",
            
            "placePackageName", "viewIfacePackageName", "viewImplPackageName", "uiBinderPackageName",
            "activityPackageName", "placeBaseClassPackageName",
            
            "placeHistoryMapper", "placeHistoryMapperTemplate", "placeHistoryMapperReplaceToken",
            "viewMasterIface", "viewMasterIfaceReplaceToken", "viewMasterIfaceTemplate",
            "viewMasterImpl", "viewMasterImplReplaceToken", "viewMasterImplTemplate",
            ])
        getOptionalPropertiesFromConfig(this, config, [
            "srcBaseFolder", 
            ])
        
        VelocityEngine ve = new VelocityEngine();
        ve.init();

        def (String placeName, String placeBaseName) = commandObj.args

        // If not overridden on cli, use these
        String placeClassName = placeName
        String viewIfaceClassName = placeName.capitalize() + "View"
        String viewImplVarName = placeName + "View"
        String viewImplClassName = viewIfaceClassName + "Impl"
        String activityClassName = placeName.capitalize() + "Activity"
        String placePrettyName = commandObj.prettyName ? commandObj.prettyName : placeName.capitalize();
        String viewUiBinderClassName = viewIfaceClassName + "UiBinder"
        String uiBinderFileName = viewIfaceClassName

        def context = [
            "placeClassName" : placeClassName,
            "viewIfaceClassName" : viewIfaceClassName,
            "viewImplClassName" : viewImplClassName,
            "viewImplVarName" : viewImplVarName,
            "activityClassName" : activityClassName,
            "placePrettyName" : placePrettyName,
            "placeBaseClassName" : placeBaseName,
            "uiBinderFileName" : uiBinderFileName,

            "placePackageName" : placePackageName,
            "viewIfacePackageName" : viewIfacePackageName,
            "viewImplPackageName" : viewImplPackageName,
            "activityPackageName" : activityPackageName,
            "placeBaseClassPackageName" : placeBaseClassPackageName,
            //"PackageName" : PackageName,

            "viewUiBinderClassName" : viewUiBinderClassName,
        ]
        log.debug("Got / derived: {}", context);
        
        VelocityContext ctx = new VelocityContext(context);

        writeFile(ve, ctx, getFullPathFromBaseFolderAndFullClass(templateBaseFolder, placeTemplateClass), getFullPathFromBaseFolderPackageAndClass(srcBaseFolder, placePackageName,placeClassName))
        writeFile(ve, ctx, getFullPathFromBaseFolderAndFullClass(templateBaseFolder, viewIfaceTemplateClass), getFullPathFromBaseFolderPackageAndClass(srcBaseFolder, viewIfacePackageName,viewIfaceClassName))
        writeFile(ve, ctx, getFullPathFromBaseFolderAndFullClass(templateBaseFolder, viewImplTemplateClass), getFullPathFromBaseFolderPackageAndClass(srcBaseFolder, viewImplPackageName,viewImplClassName))
        writeFile(ve, ctx, getFullPathFromBaseFolderAndFullClass(templateBaseFolder, activityTemplateClass), getFullPathFromBaseFolderPackageAndClass(srcBaseFolder, activityPackageName,activityClassName))
        writeFile(ve, ctx, getFullPathFromBaseFolderAndFullClass(templateBaseFolder, uiBinderTemplateFile, ".ui.xml"), getFullPathFromBaseFolderPackageAndClass(srcBaseFolder, uiBinderPackageName,uiBinderFileName,".ui.xml"))

        modifySourceFile(ve, ctx, srcBaseFolder, placeHistoryMapper, placeHistoryMapperReplaceToken, placeHistoryMapperTemplate)
        modifySourceFile(ve, ctx, srcBaseFolder, viewMasterIface, viewMasterIfaceReplaceToken, viewMasterIfaceTemplate)
        modifySourceFile(ve, ctx, srcBaseFolder, viewMasterImpl, viewMasterImplReplaceToken, viewMasterImplTemplate)
    }
}
