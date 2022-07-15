package org.jenkinsci.gradle.plugins.jte

import org.gradle.testkit.runner.GradleRunner

class TestUtil {
    File projectDir
    File pluginDir
    File buildFile
    GradleRunner jte
    File baseDirectory

    TestUtil(File projectDir, File pluginDir){
        this.projectDir = projectDir
        this.pluginDir = pluginDir
        this.baseDirectory = new File(projectDir, JteExtension.DEFAULT_BASE_DIRECTORY)
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
        File library = new File(baseDirectory, "${libraryName}/steps")
        library.mkdirs()
        File step = new File(library, "${stepName}.groovy")
        step.text = stepText
    }

    static TestUtil setup(){
        File projectDir = File.createTempDir()
        File pluginDir = File.createTempDir()
        return new TestUtil(projectDir, pluginDir)
    }
}
