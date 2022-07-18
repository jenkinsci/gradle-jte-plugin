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
