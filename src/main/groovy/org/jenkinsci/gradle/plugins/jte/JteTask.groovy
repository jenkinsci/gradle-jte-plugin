package org.jenkinsci.gradle.plugins.jte

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction

/**
 * Implements the `jte` task which creates a directory and
 * then populates it with the source code for the Jenkins
 * Plugin that will be built.
 */
abstract class JteTask extends DefaultTask{
    @Input
    JteExtension extension

    @Input
    Project project

    @TaskAction
    def create(){
        // confirm libraries base directory exists and is a directory
        File baseDirectory =  extension.baseDirectory.get().asFile
        if(!baseDirectory.exists()){
            throw new GradleException("baseDirectory '${baseDirectory}' does not exist")
        } else if(!baseDirectory.isDirectory()){
            throw new GradleException("baseDirectory '${baseDirectory}' is not a directory")
        }

        // determine where to put the generated plugin source code
        File pluginDir
        if(extension.pluginGenerationDirectory.isPresent()){
            pluginDir = extension.pluginGenerationDirectory.get().asFile
            pluginDir.mkdirs()
        } else {
            pluginDir = File.createTempDir()
        }

        // copy the libraries into the plugin's resources directory
        File libraries = new File(pluginDir, "src/main/resources/libraries")
        libraries.mkdirs()
        project.copy{
            from baseDirectory
            into libraries
        }

        // create the source file that registers the plugin as a library source
        File srcDir = new File(pluginDir, "src/main/groovy")
        srcDir.mkdirs()
        InputStream libraryClassFile = getClass().getResourceAsStream("LibrarySourcePlugin.groovy.template")
        String source =  libraryClassFile.readLines().join("\n")
        File librarySourceClassFile = File.createTempFile("LibrarySourcePlugin", ".groovy")
        librarySourceClassFile.text = source
        String symbol
        if (extension.pluginSymbol.isPresent()){
            symbol = extension.pluginSymbol.get()
        }
        project.copy{
            from librarySourceClassFile
            into srcDir
            rename '.*', 'LibrarySourcePlugin.groovy'
            expand(
                packageStr: generatePackageName(),
                pluginSymbol: symbol
            )
        }

        // ensure Groovy plugin is applied and
        // add temporary directory to source sets
        project.plugins.apply(GroovyPlugin)
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer)
        SourceSet main = sourceSets.getByName("main")
        main.groovy.srcDir(new File(pluginDir, "src/main/groovy"))
        main.resources.srcDir(new File(pluginDir, "src/main/resources"))
    }

    /**
     * generates a hopefully unique package name.
     * This is required in case a user has multiple
     * @return
     */
    String generatePackageName(){
        String uuid = UUID.randomUUID().toString().replaceAll("-","")
        return "jte.generated.a${uuid}" // package names have to start with a letter
    }

}