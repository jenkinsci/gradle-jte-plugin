package org.jenkinsci.gradle.plugins.jte

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.FAILED

class FunctionalTestSpec extends Specification {
    File projectDir
    File pluginDir
    GradleRunner jte
    File buildFile

    void setup(){
        projectDir = File.createTempDir()
        pluginDir = File.createTempDir()
        jte = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("jte")
                .withPluginClasspath()
        buildFile = new File(projectDir, "build.gradle")
        buildFile << """
        plugins{
          id 'org.jenkins-ci.jte'
        }

        jenkinsPlugin{
          coreVersion = '2.263.1'
          shortName = 'my-libraries'
        }
        """
    }

    def "missing libraries directory throws exception"(){
        when:
        def result = jte.buildAndFail()
        then:
        result.task(":jte").outcome == FAILED
        result.output =~ /baseDirectory .* does not exist/
    }

    def "baseDirectory is not a directory throws exception"(){
        when:
        File f = new File(projectDir, "libraries")
        f.createNewFile()
        def result = jte.buildAndFail()
        then:
        result.task(":jte").outcome == FAILED
        result.output =~ /baseDirectory .* is not a directory/
    }

    def "libraries at default location get included in plugin source directory"() {
        given:
        buildFile << """
        jte{
          pluginGenerationDirectory = file('${pluginDir}')
        }
        """
        File library = new File(projectDir, "libraries/exampleLibrary/steps")
        library.mkdirs()
        File step = new File(library, "example.groovy")
        step.text = "void call(){}"
        when:
        def result = jte.build()
        then:
        result.task(":jte").outcome == SUCCESS
        new File(pluginDir, "src/main/resources/libraries/exampleLibrary/steps/example.groovy").exists()
    }

    def "user provided libraries location get included in plugin source directory"(){
        given:
        buildFile << """
        jte{
          pluginGenerationDirectory = file('${pluginDir}')
          baseDirectory = file("nested/location")
        }
        """
        File library = new File(projectDir, "nested/location/exampleLibrary/steps")
        library.mkdirs()
        File step = new File(library, "example.groovy")
        step.text = "void call(){}"
        when:
        def result = jte.build()
        then:
        result.task(":jte").outcome == SUCCESS
        new File(pluginDir, "src/main/resources/libraries/exampleLibrary/steps/example.groovy").exists()
    }

    def "when pluginSymbol not provided, @Symbol not present in source file"(){
        buildFile << """
        jte{
          pluginGenerationDirectory = file('${pluginDir}')
        }
        """
        new File(projectDir, "libraries").mkdirs()
        when:
        def result = jte.build()
        then:
        result.task(":jte").outcome == SUCCESS
        File source = new File(pluginDir, "src/main/groovy/LibrarySourcePlugin.groovy")
        assert source.exists()
        assert !source.text.contains("@Symbol")
    }

    def "when pluginSymbol is provided, @Symbol is present in source file"(){
        buildFile << """
        jte{
          pluginGenerationDirectory = file('${pluginDir}')
          pluginSymbol = "myCustomName"
        }
        """
        new File(projectDir, "libraries").mkdirs()
        when:
        def result = jte.build()
        then:
        result.task(":jte").outcome == SUCCESS
        File source = new File(pluginDir, "src/main/groovy/LibrarySourcePlugin.groovy")
        assert source.exists()
        assert source.text.contains("@Symbol('myCustomName')")
    }

}