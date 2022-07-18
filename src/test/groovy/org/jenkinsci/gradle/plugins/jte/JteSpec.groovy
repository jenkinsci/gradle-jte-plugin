package org.jenkinsci.gradle.plugins.jte

import org.gradle.testkit.runner.BuildResult
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.Rule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Specification
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class JteSpec extends Specification {

    TestUtil test = new TestUtil()
    @Rule JenkinsRule jenkins = new JenkinsRule()

    def "Installing the generated plugin registers the extension"(){
        given: "there's a project using this gradle plugin"
        test.createStep("exampleLibrary", "example", "void call(){}")

        when: "the jte task is invoked"
        BuildResult result = test.runJteTask()

        then: "the task succeeds and produces the generated plugin artifact"
        assert result.task(":jte").outcome == SUCCESS
        File plugin = new File(test.projectDir, "build/libs/${test.pluginShortName}.hpi")
        assert plugin.exists()

        when: "the generated plugin is installed and jenkins restarted"
        jenkins.pluginManager.dynamicLoad(plugin)

        then: "Jenkins can now find the JTE library providing plugin"
        assert test.getPlugin(jenkins)
    }

    def "Library steps from generated plugin work in pipeline"(){
        given: "there's a project using this gradle plugin with two libraries"
        test.createStep("exampleLibrary", "step1", "void call(){ println 'running step 1' }")
        test.createStep("anotherLibrary", "step2", "void call(){ println 'running step 2' }")

        when: "the plugin is installed"
        test.runJteTask()
        File plugin = new File(test.projectDir, "build/libs/${test.pluginShortName}.hpi")
        jenkins.pluginManager.dynamicLoad(plugin)

        and: "a governance tier has the generated plugin as a library source"
        test.addLibrarySource(jenkins)

        and: "a pipeline loads the libraries and calls the steps"
        WorkflowJob job = test.createJob(jenkins,
            'libraries{ exampleLibrary; anotherLibrary }',
            'step1(); step2()'
        )
        WorkflowRun run = job.scheduleBuild2(0).get()

        then: "the pipeline succeeds"
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("running step 1", run)
        jenkins.assertLogContains("running step 2", run)
    }

    def "Library resources from generated plugin work in pipeline"(){
        given: "there's a project using this gradle plugin with two libraries"
        test.createResource("exampleLibrary", "file.txt", "resource text")
        test.createStep("exampleLibrary", "step", "void call(){ println resource('file.txt') }")


        when: "the plugin is installed"
        test.runJteTask()
        File plugin = new File(test.projectDir, "build/libs/${test.pluginShortName}.hpi")
        jenkins.pluginManager.dynamicLoad(plugin)

        and: "a governance tier has the generated plugin as a library source"
        test.addLibrarySource(jenkins)

        and: "a pipeline loads the libraries and calls the steps"
        WorkflowJob job = test.createJob(jenkins,
            'libraries{ exampleLibrary }',
            'step()'
        )
        WorkflowRun run = job.scheduleBuild2(0).get()

        then: "the pipeline succeeds"
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("resource text", run)
    }

    def "Library classes from generated plugin work in pipeline"(){
        given: "there's a project using this gradle plugin with two libraries"
        test.createClass("exampleLibrary", "Utility.groovy", """
        class Utility{
          static printMessage(def x){
            x.steps.echo "message from class"
          }
        }
        """)
        test.createStep("exampleLibrary", "step", "import Utility; void call(){ Utility.printMessage(this) }")


        when: "the plugin is installed"
        test.runJteTask()
        File plugin = new File(test.projectDir, "build/libs/${test.pluginShortName}.hpi")
        jenkins.pluginManager.dynamicLoad(plugin)

        and: "a governance tier has the generated plugin as a library source"
        test.addLibrarySource(jenkins)

        and: "a pipeline loads the libraries and calls the steps"
        WorkflowJob job = test.createJob(jenkins,
            'libraries{ exampleLibrary }',
            'step()'
        )
        WorkflowRun run = job.scheduleBuild2(0).get()

        then: "the pipeline succeeds"
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("message from class", run)
    }

}
