package org.jenkinsci.gradle.plugins.jte

import com.cloudbees.hudson.plugins.folder.Folder
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
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jenkinsci.plugins.workflow.job.WorkflowJob
import org.jvnet.hudson.test.JenkinsRule

class TestUtil {
    /**
     * The directory where the test gradle project will be
     */
    File projectDir = File.createTempDir()

    /**
     * The directory where the generated plugin source code will be
     */
    File pluginDir = File.createTempDir()

    /**
     * The base directory where the test libraries can be found
     */
    File baseDirectory = new File(projectDir, JteExtension.DEFAULT_BASE_DIRECTORY)

    /**
     * randomly generated plugin short name
     * this is necessary because, despite my best effort,
     * i can't seem to uninstall the plugin after each
     * individual test
     */
    String pluginShortName

    /**
     * The jenkinsVersion that will be passed to the jenkinsPlugin
     * section of build.gradle
     */
    String jenkinsVersion = "2.263.1"

    /**
     * The symbol that will be provided to the jte{} block
     * in the build.gradle file
     */
    String pluginSymbol

    /**
     * Test provided additional text for the build.gradle file
     */
    String buildFileAppends

    BuildResult runJteTask(boolean shouldFail = false){
        pluginShortName = pluginShortName ?: UUID.randomUUID().toString().replaceAll("-","")

        File buildFile = new File(projectDir, "build.gradle")
        buildFile.text = """
        plugins{
          id 'io.jenkins.jte'
          id 'groovy'
        }

        jenkinsPlugin{
          coreVersion = '${jenkinsVersion}'
          shortName = '${pluginShortName}'
          displayName = "custom library providing plugin"
        }
        
        jte{
            pluginGenerationDirectory = file('${pluginDir}')
            ${pluginSymbol ? "pluginSymbol = '${pluginSymbol}'": ""}
            baseDirectory = file("${baseDirectory}")
        }
        
        ${buildFileAppends}
        """

        GradleRunner jte = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("jte")
            .withPluginClasspath()

        return shouldFail ? jte.buildAndFail() : jte.build()

    }

    void appendToBuildFile(String text){
        buildFileAppends = """
        ${buildFileAppends}

        ${text}
        """.stripIndent()
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
        LibraryProvidingPlugin plugin = getPlugin(jenkins)
        if(plugin == null){
            throw new Exception("Could not find generated plugin in jenkins!")
        }
        PluginLibraryProvider provider = new PluginLibraryProvider(plugin)
        LibrarySource source = new LibrarySource(provider)
        TemplateGlobalConfig global = TemplateGlobalConfig.get()
        GovernanceTier tier = global.getTier() ?: new GovernanceTier()
        tier.setLibrarySources([ source ])
        global.setTier(tier)
    }

    /**
     * Creates a JTE pipeline job with the given config and template
     * within the provided owner
     *
     * @param owner either the JenkinsRule or a Folder
     * @param config the Pipeline Configuration for this job
     * @param template the Pipeline Template for this job
     * @return the created job
     */
    WorkflowJob createJob(def owner, String config, String template){
        WorkflowJob job = owner.createProject(WorkflowJob, UUID.randomUUID().toString())
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

}