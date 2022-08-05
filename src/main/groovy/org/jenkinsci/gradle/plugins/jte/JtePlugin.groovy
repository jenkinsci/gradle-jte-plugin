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
        Object extension = JteExtension.configure(project)
        // create a task "jte" that builds the plugin code and then use
        // the gradle-jpi-plugin to build the artifact
        project.plugins.apply(JpiPlugin)
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
