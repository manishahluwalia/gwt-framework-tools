package gh.ma.gwt.tools

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException
import com.beust.jcommander.Parameters;

import gh.ma.gwt.tools.MakePlace.CommandObj;
import groovy.util.logging.Slf4j;
import groovyjarjarcommonscli.CommandLineParser;
import groovyjarjarcommonscli.GnuParser
import groovyjarjarcommonscli.Options


@Slf4j
class MakePlace {

    @Parameters(commandDescription="Make a place, view iface, view impl and activity and wire it up to the app")
    class CommandObj {
        @Parameter(names="--config", description="The groovy-style config file to use.", required=true)
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
    String placeTemplateFilePath
    String viewIfaceTemplateFilePath
    String viewImplTemplateFilePath
    String uiBinderTemplateFilePath
    String activityTemplateFilePath
    String srcBaseDir = "src/main/java"
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

        File configFile = new File(commandObj.config)
        if (!configFile.exists()) {
            throw new ParameterException("Missing config file: " + commandObj.config)
        }
        ConfigObject config = new ConfigSlurper().parse(configFile.toURI().toURL())
        log.debug("Read config: {}",config.dump())

        getRequiredPropertiesFromConfig(this, config, [
            "placeTemplateFilePath", "viewIfaceTemplateFilePath", "viewImplTemplateFilePath",
            "uiBinderTemplateFilePath", "activityTemplateFilePath",
            
            "placePackageName", "viewIfacePackageName", "viewImplPackageName", "uiBinderPackageName",
            "activityPackageName", "placeBaseClassPackageName",
            
            "placeHistoryMapper", "placeHistoryMapperTemplate", "placeHistoryMapperReplaceToken",
            "viewMasterIface", "viewMasterIfaceReplaceToken", "viewMasterIfaceTemplate",
            "viewMasterImpl", "viewMasterImplReplaceToken", "viewMasterImplTemplate",
            ])
        getOptionalPropertiesFromConfig(this, config, [
            "srcBaseDir", 
            ])
        
        VelocityEngine ve = new VelocityEngine();
        ve.init();

        String placeName = commandObj.args.get(0)
        String placeBaseName = commandObj.args.get(1)

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

        writeFile(ve, ctx, placeTemplateFilePath, getSourcePathName(placePackageName,placeClassName))
        writeFile(ve, ctx, viewIfaceTemplateFilePath, getSourcePathName(viewIfacePackageName,viewIfaceClassName))
        writeFile(ve, ctx, viewImplTemplateFilePath, getSourcePathName(viewImplPackageName,viewImplClassName))
        writeFile(ve, ctx, activityTemplateFilePath, getSourcePathName(activityPackageName,activityClassName))
        writeFile(ve, ctx, uiBinderTemplateFilePath, getSourcePathName(uiBinderPackageName,uiBinderFileName,".ui.xml"))
        //writeFile(ve, ctx, templatesBaseDir+"/"+TemplateFile, getSourcePathName(PackageName,ClassName))

        modifySourceFile(ve, ctx, placeHistoryMapper, placeHistoryMapperReplaceToken, placeHistoryMapperTemplate)
        modifySourceFile(ve, ctx, viewMasterIface, viewMasterIfaceReplaceToken, viewMasterIfaceTemplate)
        modifySourceFile(ve, ctx, viewMasterImpl, viewMasterImplReplaceToken, viewMasterImplTemplate)
    }
    
    void getRequiredPropertiesFromConfig(Object destination, ConfigObject source, List<String> propNames) {
        propNames.each { p ->
            getRequiredPropertyFromConfig(this, source, p)
        }
    }
    
    void getOptionalPropertiesFromConfig(Object destination, ConfigObject source, List<String> propNames) {
        propNames.each { p ->
            getOptionalPropertyFromConfig(this, source, p)
        }
    }

    void getRequiredPropertyFromConfig(Object destination, ConfigObject source, List<String> propNames) {
        propNames.each { p ->
            getRequiredPropertiesFromConfig(this, source, p)
        }
    }
    
    void getOptionalPropertyFromConfig(Object destination, ConfigObject source, String propertyName) {
        if (source.containsKey(propertyName)) {
            destination[propertyName] = source[propertyName]
        }
    }
    
    void getRequiredPropertyFromConfig(Object destination, ConfigObject source, String propertyName) {
        if (!source.containsKey(propertyName)) {
            throw new ParameterException("Required config setting " + propertyName + " missing in config file");
        }
        destination[propertyName] = source[propertyName]
    }
    
    void writeFile(VelocityEngine ve, VelocityContext ctx, String templateFilePath, String outputFilePath) {
        String outfileFullPath = srcBaseDir + "/" + outputFilePath
        log.info("Creating {} from {}", outfileFullPath, templateFilePath);
        Template template = ve.getTemplate(templateFilePath, "UTF-8")
        Writer writer = new FileWriter(outfileFullPath);
        template.merge(ctx, writer);
        writer.flush();
        writer.close();
    }

    void modifySourceFile(VelocityEngine ve, VelocityContext ctx, String sourceClass, String tokenToReplace, String template) {
        String sourceFileFullPath = srcBaseDir + "/" + sourceClass.replace('.' as char, '/' as char) + ".java"
        log.info("Modifying {}", sourceFileFullPath)

        def replacement = new StringWriter()
        ve.evaluate(ctx, replacement, sourceFileFullPath, new StringReader(template))
        log.debug("  Replacing {}", tokenToReplace);
        log.debug("  Replacing with {}", replacement.buf.toString());

        File sourceFile = new File(sourceFileFullPath)
        StringBuilder modifiedText = new StringBuilder()
        sourceFile.eachLine { line ->
            int index = line.indexOf(tokenToReplace)
            log.trace("     line {} index: {}", line, index)
            if (index!=-1) {
                String before = line.substring(0, index)
                String after = line.substring(index+tokenToReplace.length())
                replacement.buf.toString().split('\n').each { l ->
                    modifiedText.append(before)
                    modifiedText.append(l)
                    modifiedText.append(after)
                    modifiedText.append('\n')
                }
            }
            modifiedText.append(line)
            modifiedText.append('\n')
        }

        log.trace("New file:");
        log.trace("{}", modifiedText.toString())

        sourceFile.write(modifiedText.toString())
    }

    String getSourcePathName(String packageName, String className, String extension=".java") {
        return packageName.replace('.' as char,'/' as char)+"/"+ className + extension
    }

}
