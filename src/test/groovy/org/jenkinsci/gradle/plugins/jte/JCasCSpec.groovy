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

import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification
import io.jenkins.plugins.casc.ConfigurationAsCode

class JCasCSpec extends Specification {

    TestUtil test = new TestUtil(pluginSymbol: "myCustomPluginSymbol")
    @Rule JenkinsRule jenkins = new JenkinsRule()

    def "JCasC can configure global library source using @Symbol name"(){
        given: "There's a library source using Gradle JTE Plugin"
        test.createStep("exampleLibrary", "step", "void call(){ println 'running step from plugin' }")

        when: "the generated plugin is installed"
        test.runJteTask()
        File plugin = new File(test.projectDir, "build/libs/${test.pluginShortName}.hpi")
        jenkins.pluginManager.dynamicLoad(plugin)

        and: "JCasC is loaded referencing the generated jenkins plugin"
        File jcasc = new File(jenkins.instance.rootDir, ConfigurationAsCode.DEFAULT_JENKINS_YAML_PATH)
        jcasc.text = """
        unclassified:
            templateGlobalConfig:
                tier:
                  configurationProvider: "null"
                  librarySources:
                  - libraryProvider:
                      plugin:
                        plugin: "myCustomPluginSymbol"
        """.stripIndent()
        ConfigurationAsCode.get().configure()

        and: "there's a job loading the libraries"
        WorkflowJob job = test.createJob(jenkins, "libraries{ exampleLibrary }", "step()")
        WorkflowRun run = job.scheduleBuild2(0).get()

        then: "the job succeeds as expected"
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("running step from plugin", run)
    }

}
