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
        @Parameter(names="--config", description="The config file to use")
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

    // These should be read from cli / config file
    def templatesBaseDir = "src/templates/java/templates/Mvp"
    def srcBaseDir = "src/main/java"
    def placeTemplateFile = '$placeClassName.java'
    def viewIfaceTemplateFile = '$viewIfaceClassName.java'
    def viewImplTemplateFile = '$viewImplClassName.java'
    def uiBinderTemplateFile = '${uiBinderFileName}.ui.xml'
    def activityTemplateFile = '$activityClassName.java'
    def basePackageName = "demo.app.client"
    def placeSubPackage = "places"
    def viewIfaceSubPackage = "view"
    def viewImplSubPackage = "view.impl.views"
    def uiBinderSubPackage = viewImplSubPackage
    def activitySubPackage = "activities"
    def placeBaseClassSubPackage = "framework"

    // Derived from config values, but default can be overridden
    def placePackageName = basePackageName + "." + placeSubPackage
    def viewIfacePackageName = basePackageName + "." + viewIfaceSubPackage
    def viewImplPackageName = basePackageName + "." + viewImplSubPackage
    def uiBinderPackageName = basePackageName + "." + uiBinderSubPackage
    def activityPackageName = basePackageName + "." + activitySubPackage
    def placeBaseClassPackageName = basePackageName + "." + placeBaseClassSubPackage
    //def PackageName = basePackageName + "." + SubPackage

    // Get from config values
    def placeHistoryMapper = "demo.app.client.framework.AppPlaceHistoryMapper"
    def placeHistoryMapperTemplate = ', $placePackageName.$placeClassName .Tokenizer.class'
    def viewMasterIface = "demo.app.client.view.ViewMaster"
    def viewMasterIfaceTemplate = 'public void get$viewIfaceClassName(RunnableWithArg<$viewIfacePackageName.$viewIfaceClassName> callback);'
    def viewMasterImpl = "demo.app.client.view.impl.ViewMasterImpl"
    def viewMasterImplTemplate = '''\
private $viewIfacePackageName.$viewIfaceClassName $viewImplVarName;
@Override
public void get$viewIfaceClassName(RunnableWithArg<$viewIfacePackageName.$viewIfaceClassName> callback)
{
    if (null == $viewImplVarName)
    {
        final Timer timer = new Timer(TimedEvent.VIEW_CREATION, "ViewMasterImpl.$viewIfaceClassName");
        $viewImplVarName = new $viewImplPackageName.$viewImplClassName();
        timer.end();
    }

    callback.run($viewImplVarName);
}
 

'''

    void run() {

        int numArgs = commandObj.args ? commandObj.args.size() : 0
        if (2!=numArgs) {
            throw new ParameterException("Incorrect number of arguments")
        }

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

        log.debug("Got / derived:");
        log.debug("  Place Pretty Name: {}", placePrettyName)
        
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
        VelocityContext ctx = new VelocityContext(context);

        writeFile(ve, ctx, templatesBaseDir+"/"+placeTemplateFile, getSourcePathName(placePackageName,placeClassName))
        writeFile(ve, ctx, templatesBaseDir+"/"+viewIfaceTemplateFile, getSourcePathName(viewIfacePackageName,viewIfaceClassName))
        writeFile(ve, ctx, templatesBaseDir+"/"+viewImplTemplateFile, getSourcePathName(viewImplPackageName,viewImplClassName))
        writeFile(ve, ctx, templatesBaseDir+"/"+activityTemplateFile, getSourcePathName(activityPackageName,activityClassName))
        writeFile(ve, ctx, templatesBaseDir+"/"+uiBinderTemplateFile, getSourcePathName(uiBinderPackageName,uiBinderFileName,".ui.xml"))
        //writeFile(ve, ctx, templatesBaseDir+"/"+TemplateFile, getSourcePathName(PackageName,ClassName))

        modifySourceFile(ve, ctx, placeHistoryMapper, '/* GFT-MAKE-PLACE-TOKENIZER */', placeHistoryMapperTemplate)
        modifySourceFile(ve, ctx, viewMasterIface, '/* GFT-MAKE-PLACE-VIEW-IFACE */', viewMasterIfaceTemplate)
        modifySourceFile(ve, ctx, viewMasterImpl, '/* GFT-MAKE-PLACE-VIEW-IMPL */', viewMasterImplTemplate)
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
