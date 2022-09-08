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

import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Optional

/**
 * defines the configurability of the `jte{}` block within
 * the build configuration
 */
abstract class JteExtension{

    static final String DEFAULT_BASE_DIRECTORY = "libraries"
    static final String DEFAULT_JTE_VERSION = "2.0"

    /**
     * Registers the `jte` Extension and defines configuration conventions
     * @param project
     * @return
     */
    static Object configure(Project project){
        // register the `jte` block in the build configuration dsl
        Object extension = project.extensions.create('jte', JteExtension)
        // set the default value of `jte.baseDirectory` to `libraries`
        extension.baseDirectory.convention(project.getLayout().getProjectDirectory().file(DEFAULT_BASE_DIRECTORY))
        extension.jteVersion.convention(DEFAULT_JTE_VERSION)
        return extension
    }

    /**
     * defines where the libraries to bundle into the resulting
     * jenkins plugin can be found
     * @return the value of `baseDirectory` from the build configuration
     */
    abstract RegularFileProperty getBaseDirectory()

    /**
     * defines the value provided to the `@Symbol` annotation on the
     * generated plugin class's descriptor. This value is used to
     * reference the generated plugin via JCasC and Job DSL.
     * @return
     */
    @Optional
    abstract Property<String> getPluginSymbol()

    /**
     * for testing and debugging, this property defines where the
     * generated plugin source code will be generated.
     * @return
     */
    @Optional
    abstract RegularFileProperty getPluginGenerationDirectory()

    @Optional
    abstract Property<String> getJteVersion()

}
