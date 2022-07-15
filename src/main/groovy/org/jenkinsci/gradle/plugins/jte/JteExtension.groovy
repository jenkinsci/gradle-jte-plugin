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
    /**
     * defines where the libraries to bundle into the resulting
     * jenkins plugin can be found
     * @return the value of `baseDirectory` from the build configuration
     */
    abstract RegularFileProperty getBaseDirectory()

    static final DEFAULT_BASE_DIRECTORY = "libraries"

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

    /**
     * Registers the `jte` Extension and defines configuration conventions
     * @param project
     * @returnf
     */
    static def configure(Project project){
        // register the `jte` block in the build configuration dsl
        def extension = project.extensions.create('jte', JteExtension)
        // set the default value of `jte.baseDirectory` to `libraries`
        extension.baseDirectory.convention(project.getLayout().getProjectDirectory().file(DEFAULT_BASE_DIRECTORY))
        return extension
    }
}