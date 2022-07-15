package org.jenkinsci.gradle.plugins.jte

import org.boozallen.plugins.jte.init.governance.GovernanceTier
import org.boozallen.plugins.jte.init.governance.TemplateGlobalConfig
import org.boozallen.plugins.jte.init.governance.config.ConsoleDefaultPipelineTemplate
import org.boozallen.plugins.jte.init.governance.config.ConsolePipelineConfiguration
import org.boozallen.plugins.jte.init.governance.libs.LibrarySource
import org.boozallen.plugins.jte.job.AdHocTemplateFlowDefinition
import org.boozallen.plugins.jte.job.ConsoleAdHocTemplateFlowDefinitionConfiguration
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jenkinsci.plugins.workflow.job.WorkflowRun
import org.junit.ClassRule
import org.jvnet.hudson.test.JenkinsRule
import spock.lang.Shared
import spock.lang.Specification
import org.boozallen.plugins.jte.init.governance.libs.PluginLibraryProvider
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class IntegrationTestSpec extends Specification {

    TestUtil test
    @Shared @ClassRule JenkinsRule jenkins = new JenkinsRule()

    void setup(){
        test = TestUtil.setup()
    }

    def "Installing the generated plugin registers the extension"(){
        given: "there's a project using this gradle plugin"
        test.createStep("exampleLibrary", "example", "void call(){}")

        when: "the jte task is invoked"
        def result = test.jte.build()

        then: "the task succeeds and produces the generated plugin artifact"
        result.task(":jte").outcome == SUCCESS
        File plugin = new File(test.projectDir, "build/libs/my-libraries.hpi")
        assert plugin.exists()

        expect: "Jenkins does not find any JTE library providing plugins"
        assert PluginLibraryProvider.DescriptorImpl.getLibraryProvidingPlugins().isEmpty()

        when: "the generated plugin is installed"
        jenkins.getInstance().getPluginManager().dynamicLoad(plugin)

        then: "Jenkins can now find the JTE library providing plugin"
        List plugins = PluginLibraryProvider.DescriptorImpl.getLibraryProvidingPlugins()
        assert plugins.size() == 1
        assert plugins.first().getDisplayName() == "custom library providing plugin"
    }

    def "Libraries from generated plugin works in pipeline"(){
        given: "there's a project using this gradle plugin with two libraries"
        test.createStep("exampleLibrary", "step1", "void call(){ println 'running step 1' }")
        test.createStep("anotherLibrary", "step2", "void call(){ println 'running step 2' }")

        when: "the plugin is installed"
        test.jte.build()
        File plugin = new File(test.projectDir, "build/libs/my-libraries.hpi")
        jenkins.getInstance().getPluginManager().dynamicLoad(plugin)
        List plugins = PluginLibraryProvider.DescriptorImpl.getLibraryProvidingPlugins()

        and: "a governance tier has the generated plugin as a library source"
        PluginLibraryProvider provider = new PluginLibraryProvider(plugins.first().getClass().getDeclaringClass().newInstance())
        LibrarySource source = new LibrarySource(provider)
        TemplateGlobalConfig global = TemplateGlobalConfig.get()
        GovernanceTier tier = global.getTier() ?: new GovernanceTier()
        tier.setLibrarySources([ source ])
        global.setTier(tier)

        and: "a pipeline loads the libraries and calls the steps"
        WorkflowJob job = jenkins.createProject(WorkflowJob)
        def pipelineConfig = new ConsolePipelineConfiguration(true, 'libraries{ exampleLibrary; anotherLibrary }')
        def pipelineTemplate = new ConsoleDefaultPipelineTemplate(true, 'step1(); step2()')
        def templateConfiguration = new ConsoleAdHocTemplateFlowDefinitionConfiguration(pipelineTemplate, pipelineConfig)
        def definition = new AdHocTemplateFlowDefinition(templateConfiguration)
        job.setDefinition(definition)
        WorkflowRun run = job.scheduleBuild2(0).get()

        then: "the pipeline succeeds"
        jenkins.assertBuildStatusSuccess(run)
        jenkins.assertLogContains("running step 1", run)
        jenkins.assertLogContains("running step 2", run)
    }

}
