/*
    Copyright (c) 2018-2022 Booz Allen Hamilton

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package org.jenkinsci.gradle.plugins.jte

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.nio.file.Path

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
    void configure(){
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
        Path librarySourceClassFile = Files.createTempFile("LibrarySourcePlugin", ".groovy")
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
        String uuid = UUID.randomUUID().toString().replaceAll("-", "")
        return "jte.generated.a${uuid}" // package names have to start with a letter
    }

}
