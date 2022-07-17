package org.jenkinsci.gradle.plugins.jte

import hudson.PluginWrapper
import org.boozallen.plugins.jte.init.governance.GovernanceTier
import org.boozallen.plugins.jte.init.governance.TemplateGlobalConfig
import org.boozallen.plugins.jte.init.governance.config.ConsoleDefaultPipelineTemplate
import org.boozallen.plugins.jte.init.governance.config.ConsolePipelineConfiguration
import org.boozallen.plugins.jte.init.governance.libs.LibraryProvidingPlugin
import org.boozallen.plugins.jte.init.governance.libs.LibrarySource
import org.boozallen.plugins.jte.init.governance.libs.PluginLibraryProvider
import org.boozallen.plugins.jte.job.AdHocTemplateFlowDefinition
import org.boozallen.plugins.jte.job.ConsoleAdHocTemplateFlowDefinitionConfiguration
import org.gradle.testkit.runner.GradleRunner
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jvnet.hudson.test.JenkinsRule

class TestUtil {
    /**
     * The directory where the test gradle project will be
     */
    File projectDir

    /**
     * The directory where the generated plugin source code will be
     */
    File pluginDir

    /**
     * The build.gradle file for the test gradle project
     */
    File buildFile

    /**
     * The 'jte' task to execute
     */
    GradleRunner jte

    /**
     * The base directory where the test libraries can be found
     */
    File baseDirectory

    /**
     * randomly generated plugin short name
     * this is necessary because, despite my best effort,
     * i can't seem to uninstall the plugin after each
     * individual test
     */
    String pluginShortName

    TestUtil(File projectDir, File pluginDir){
        this.projectDir = projectDir
        this.pluginDir = pluginDir
        this.baseDirectory = new File(projectDir, JteExtension.DEFAULT_BASE_DIRECTORY)
        jte = GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments("jte")
                .withPluginClasspath()
        pluginShortName = UUID.randomUUID().toString().replaceAll("-","")
        buildFile = new File(projectDir, "build.gradle")
        buildFile << """
        plugins{
          id 'org.jenkins-ci.jte'
        }

        jenkinsPlugin{
          coreVersion = '2.263.1'
          shortName = '${pluginShortName}'
          displayName = "custom library providing plugin"
        }
        """
    }

    void setBaseDirectory(String path){
        baseDirectory = new File(projectDir, path)
        baseDirectory.mkdirs()
    }

    void createStep(String libraryName, String stepName, String stepText){
        if(!baseDirectory.exists()) baseDirectory.mkdirs()
        File steps = new File(baseDirectory, "${libraryName}/steps")
        steps.mkdirs()
        File s = new File(steps, "${stepName}.groovy")
        s.text = stepText
    }

    void createResource(String libraryName, String path, String text){
        if(!baseDirectory.exists()) baseDirectory.mkdirs()
        File resources = new File(baseDirectory, "${libraryName}/resources")
        resources.mkdirs()
        File r = new File(resources, path)
        r.text = text
    }

    void createClass(String libraryName, String path, String text){
        if(!baseDirectory.exists()) baseDirectory.mkdirs()
        File src = new File(baseDirectory, "${libraryName}/src")
        src.mkdirs()
        File s = new File(src, path)
        s.text = text
    }

    void addLibrarySource(JenkinsRule jenkins){
        PluginLibraryProvider provider = new PluginLibraryProvider(getPlugin(jenkins))
        LibrarySource source = new LibrarySource(provider)
        TemplateGlobalConfig global = TemplateGlobalConfig.get()
        GovernanceTier tier = global.getTier() ?: new GovernanceTier()
        tier.setLibrarySources([ source ])
        global.setTier(tier)
    }

    WorkflowJob createJob(JenkinsRule jenkins, String config, String template){
        WorkflowJob job = jenkins.createProject(WorkflowJob)
        def pipelineConfig = new ConsolePipelineConfiguration(true, config)
        def pipelineTemplate = new ConsoleDefaultPipelineTemplate(true, template)
        def templateConfiguration = new ConsoleAdHocTemplateFlowDefinitionConfiguration(pipelineTemplate, pipelineConfig)
        def definition = new AdHocTemplateFlowDefinition(templateConfiguration)
        job.setDefinition(definition)
        return job
    }

    /**
     * Finds this tests LibraryProvidingPlugin. This is needed
     * because there may be multiple LibraryProvidingPlugins and
     * we need to find this test's plugin based on the randomly
     * generated plugin short name
     */
    LibraryProvidingPlugin getPlugin(JenkinsRule jenkins){
        List plugins = PluginLibraryProvider.DescriptorImpl.getLibraryProvidingPlugins()
        def p = plugins.find{c ->
            PluginWrapper w = jenkins.pluginManager.whichPlugin(c.getClass())
            w.getShortName() == pluginShortName
        }
        return p ? p.getClass().getDeclaringClass().newInstance() : null
    }

    static TestUtil setup(){
        File projectDir = File.createTempDir()
        File pluginDir = File.createTempDir()
        return new TestUtil(projectDir, pluginDir)
    }
}