package org.jenkinsci.gradle.plugins.jte

import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.FAILED

class FunctionalTestSpec extends Specification {

    TestUtil test

    void setup(){
        test = TestUtil.setup()
    }

    def "missing libraries directory throws exception"(){
        when:
        def result = test.jte.buildAndFail()
        then:
        result.task(":jte").outcome == FAILED
        result.output =~ /baseDirectory .* does not exist/
    }

    def "baseDirectory is not a directory throws exception"(){
        when:
        File f = new File(test.projectDir, JteExtension.DEFAULT_BASE_DIRECTORY)
        f.createNewFile()
        def result = test.jte.buildAndFail()
        then:
        result.task(":jte").outcome == FAILED
        result.output =~ /baseDirectory .* is not a directory/
    }

    def "libraries at default location get included in plugin source directory"() {
        given:
        test.buildFile << """
        jte{
          pluginGenerationDirectory = file('${test.pluginDir}')
        }
        """
        test.createStep("exampleLibrary", "example", "void call(){}")
        when:
        def result = test.jte.build()
        then:
        result.task(":jte").outcome == SUCCESS
        new File(test.pluginDir, "src/main/resources/libraries/exampleLibrary/steps/example.groovy").exists()
    }

    def "user provided libraries location get included in plugin source directory"(){
        given:
        String baseDirectory = "nested/location"
        test.buildFile << """
        jte{
          pluginGenerationDirectory = file('${test.pluginDir}')
          baseDirectory = file("${baseDirectory}")
        }
        """
        test.setBaseDirectory(baseDirectory)
        test.createStep("exampleLibrary", "example", "void call(){}")
        when:
        def result = test.jte.build()
        then:
        result.task(":jte").outcome == SUCCESS
        new File(test.pluginDir, "src/main/resources/libraries/exampleLibrary/steps/example.groovy").exists()
    }

    def "when pluginSymbol not provided, @Symbol not present in source file"(){
        given:
        test.buildFile << """
        jte{
          pluginGenerationDirectory = file('${test.pluginDir}')
        }
        """
        test.baseDirectory.mkdirs()
        when:
        def result = test.jte.build()
        then:
        result.task(":jte").outcome == SUCCESS
        File source = new File(test.pluginDir, "src/main/groovy/LibrarySourcePlugin.groovy")
        assert source.exists()
        assert !source.text.contains("@Symbol")
    }

    def "when pluginSymbol is provided, @Symbol is present in source file"(){
        given:
        test.buildFile << """
        jte{
          pluginGenerationDirectory = file('${test.pluginDir}')
          pluginSymbol = "myCustomName"
        }
        """
        test.baseDirectory.mkdirs()
        when:
        def result = test.jte.build()
        then:
        result.task(":jte").outcome == SUCCESS
        File source = new File(test.pluginDir, "src/main/groovy/LibrarySourcePlugin.groovy")
        assert source.exists()
        assert source.text.contains("@Symbol('myCustomName')")
    }

}