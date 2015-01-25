package gh.ma.gwt.tools

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException
import com.beust.jcommander.Parameters;

import groovy.util.logging.Slf4j;
import groovyjarjarcommonscli.CommandLineParser;
import groovyjarjarcommonscli.GnuParser
import groovyjarjarcommonscli.Options


@Slf4j
abstract class CodeWriter {
    ConfigObject readConfigFile(String filePath) {
        File configFile = new File(filePath)
        if (!configFile.exists()) {
            throw new ParameterException("Missing config file: " + filePath)
        }
        ConfigObject config = new ConfigSlurper().parse(configFile.toURI().toURL())
        log.debug("Read config: {}",config.dump())
        return config
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
    
    void writeFile(VelocityEngine ve, VelocityContext ctx, String templateFilePath,String outputFilePath) {
        String outfileFullPath = outputFilePath
        log.info("Creating {} from {}", outfileFullPath, templateFilePath);
        Template template = ve.getTemplate(templateFilePath, "UTF-8")
        Writer writer = new FileWriter(outfileFullPath);
        template.merge(ctx, writer);
        writer.flush();
        writer.close();
    }

    void modifySourceFile(VelocityEngine ve, VelocityContext ctx, String srcBaseFolder, String sourceClass, String tokenToReplace, String template) {
        String sourceFileFullPath = srcBaseFolder + "/" + sourceClass.replace('.' as char, '/' as char) + ".java"
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
    
    String getFullPathFromBaseFolderAndFullClass(String srcBaseFolder, String clazz, String extension=".java") {
        return srcBaseFolder + "/" + clazz.replace('.' as char,'/' as char) + extension
    }
    
    String getFullPathFromBaseFolderPackageAndClass(String srcBaseFolder, String packageName, String className, String extension=".java") {
        return srcBaseFolder + "/" + packageName.replace('.' as char,'/' as char)+"/"+ className + extension
    }

}
