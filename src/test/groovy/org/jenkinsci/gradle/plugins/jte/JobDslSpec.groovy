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

import com.cloudbees.hudson.plugins.folder.Folder
import hudson.EnvVars
import hudson.FilePath
import javaposse.jobdsl.dsl.ScriptRequest
import javaposse.jobdsl.plugin.JenkinsDslScriptLoader
import javaposse.jobdsl.plugin.JenkinsJobManagement
import javaposse.jobdsl.plugin.ScriptRequestGenerator
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification

class JobDslSpec extends Specification {

    TestUtil test
    @Rule JenkinsRule jenkins = new JenkinsRule()

    void setup(){
        test = new TestUtil(pluginSymbol: "customPluginSymbol")
    }

    def "Folders can be configured with library source using @Symbol name"(){
        given: "There's a library source using Gradle JTE Plugin"
        test.createStep("exampleLibrary", "step", "void call(){ println 'running step from plugin' }")

        when: "the plugin is installed"
        test.runJteTask()
        File plugin = new File(test.projectDir, "build/libs/${test.pluginShortName}.hpi")
        jenkins.pluginManager.dynamicLoad(plugin)

        and: "There's a job DSL Script that creates a folder"
        File workspace = File.createTempDir()
        File script = new File(workspace, "script.groovy")
        script.text = """
        folder("my-test-folder"){
          properties{
            templateConfigFolderProperty{
              tier{
                librarySources{
                  librarySource{
                    libraryProvider{
                      pluginLibraryProvider{
                        plugin{
                          customPluginSymbol()
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
        """

        and: "The job DSL script is processed"
        JenkinsJobManagement jobManagement = new JenkinsJobManagement(System.out, [:], workspace)
        ScriptRequestGenerator scriptGenerator = new ScriptRequestGenerator(new FilePath(workspace), new EnvVars())
        JenkinsDslScriptLoader scriptLoader = new JenkinsDslScriptLoader(jobManagement)
        Set<ScriptRequest> scripts = scriptGenerator.getScriptRequests(
            "*",
            false,
            null,
            true,
            false,
            null
        )
        scriptLoader.runScripts(scripts)

        and: "A job is created within the Folder that uses the libraries"
        Folder folder = jenkins.instance.getItemByFullName("my-test-folder")
        WorkflowJob job = test.createJob(folder, "libraries{ exampleLibrary }", "step()")
        WorkflowRun run = job.scheduleBuild2(0).get()

        then: "Everything works as expected"
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("running step from plugin", run)
    }

}
