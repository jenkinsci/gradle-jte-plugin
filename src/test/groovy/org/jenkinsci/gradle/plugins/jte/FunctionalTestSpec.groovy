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

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.FAILED

import org.gradle.testkit.runner.BuildResult
import spock.lang.Specification

class FunctionalTestSpec extends Specification {

    TestUtil test = new TestUtil()

    def "missing libraries directory throws exception"(){
        when:
        BuildResult result = test.runJteTask(true)

        then:
        result.task(":jte").outcome == FAILED
        result.output =~ /baseDirectory .* does not exist/
    }

    def "baseDirectory is not a directory throws exception"(){
        given:
        File f = new File(test.projectDir, JteExtension.DEFAULT_BASE_DIRECTORY)
        f.createNewFile()

        when:
        BuildResult result = test.runJteTask(true)

        then:
        result.task(":jte").outcome == FAILED
        result.output =~ /baseDirectory .* is not a directory/
    }

    def "libraries at default location get included in plugin source directory"() {
        given:
        test.createStep("exampleLibrary", "example", "void call(){}")

        when:
        BuildResult result = test.runJteTask()

        then:
        result.task(":jte").outcome == SUCCESS
        new File(test.pluginDir, "src/main/resources/libraries/exampleLibrary/steps/example.groovy").exists()
    }

    def "user provided libraries location get included in plugin source directory"(){
        given:
        test.setBaseDirectory("nested/location")
        test.createStep("exampleLibrary", "example", "void call(){}")

        when:
        BuildResult result = test.runJteTask()

        then:
        result.task(":jte").outcome == SUCCESS
        new File(test.pluginDir, "src/main/resources/libraries/exampleLibrary/steps/example.groovy").exists()
    }

    def "when pluginSymbol not provided, @Symbol not present in source file"(){
        given:
        test.createStep("exampleLibrary", "step", "void call(){}")

        when:
        BuildResult result = test.runJteTask()

        then:
        result.task(":jte").outcome == SUCCESS
        File source = new File(test.pluginDir, "src/main/groovy/LibrarySourcePlugin.groovy")
        assert source.exists()
        assert !source.text.contains("@Symbol")
    }

    def "when pluginSymbol is provided, @Symbol is present in source file"(){
        given:
        test.createStep("exampleLibrary", "step", "void call(){}")
        test.setPluginSymbol("myCustomName")

        when:
        BuildResult result = test.runJteTask()
        println test.projectDir

        then:
        result.task(":jte").outcome == SUCCESS
        File source = new File(test.pluginDir, "src/main/groovy/LibrarySourcePlugin.groovy")
        assert source.exists()
        assert source.text.contains("@Symbol('myCustomName')")
    }

}
