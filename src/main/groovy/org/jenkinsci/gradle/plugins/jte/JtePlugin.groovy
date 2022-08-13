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

import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.jenkinsci.gradle.plugins.jpi.JpiPlugin

/**
 * This is the main plugin extension class
 */
class JtePlugin implements Plugin<Project>{

    @Override
    void apply(Project project){
        Object extension = JteExtension.configure(project)
        addJteDependency(project, extension)
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
    void addJteDependency(Project project, Object extension) {
        project.configurations.getByName("implementation").withDependencies { dependencies ->
            String jteVersion = "2.0"
            if (extension.jteVersion.isPresent()) {
                jteVersion = extension.jteVersion.get()
            }
            checkVersion(jteVersion)
            String gav = "org.jenkins-ci.plugins:templating-engine:${jteVersion}"
            Dependency templatingEngine = project.dependencies.create(gav)
            templatingEngine.because('''Added by io.jenkins.jte plugin using `jte.jteVersion`''')
            dependencies.add(templatingEngine)
        }
    }

    void checkVersion(String jteVersion){
        String major = jteVersion.split('\\.').first()
        // ensure the major version is actually a number
        if(!major.isNumber()){
            throw new GradleException("jteVersion '${jteVersion}' is not valid.")
        }
        // ensure the major version is >= 2
        if(major.toInteger() < 2){
            throw new GradleException("jteVersion must be greater than release 2.0")
        }
        // ensure the specified version is actually a JTE release
        if(!isValidJteVersion(jteVersion)){
            throw new GradleException("jteVersion '${jteVersion}' is not a JTE release.")
        }
    }

    /**
     * fetches the released versions in artifactory and ensure
     * the provided version is valid
     * @param version
     * @return true if the version is valid, false otherwise
     */
    Boolean isValidJteVersion(String version){
        URL url = "https://repo.jenkins-ci.org/artifactory/api/search/versions?g=org.jenkins-ci.plugins&a=templating-engine".toURL()
        Object response = new JsonSlurper().parse(url)
        Set<String> versions = response.results.collect{ entry -> entry.version }
        return version in versions
    }

}
