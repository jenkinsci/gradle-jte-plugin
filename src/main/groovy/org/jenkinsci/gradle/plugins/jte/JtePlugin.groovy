package org.jenkinsci.gradle.plugins.jte

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.jenkinsci.gradle.plugins.jpi.JpiPlugin

/**
 * This is the main plugin extension class
 */
class JtePlugin implements Plugin<Project>{
    @Override
    void apply(Project project){
        addJteDependency(project)
        def extension = JteExtension.configure(project)
        // create a task "jte" that builds the plugin code and then use
        // the gradle-jpi-plugin to build the artifact
        project.plugins.apply(JpiPlugin)
//        project.plugins.apply('org.jenkins-ci.jpi')
        project.tasks.register('jte', JteTask){ JteTask t ->
            t.extension = extension
            t.project = project
            finalizedBy("jpi")
        }
    }

    /**
     * registers JTE as a dependency so that the generated plugin can compile
     * @param project
     */
    void addJteDependency(Project project){
        project.getGradle().addListener(new DependencyResolutionListener() {
            @Override
            void beforeResolve(ResolvableDependencies resolvableDependencies) {
                project.dependencies.add("implementation", 'org.jenkins-ci.plugins:templating-engine:2.5.2')
                project.getGradle().removeListener(this)
            }

            @Override
            void afterResolve(ResolvableDependencies resolvableDependencies) {}
        })
    }

}