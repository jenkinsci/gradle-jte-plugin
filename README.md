> The Jenkins Templating Engine has been marked for adoption

# Gradle JTE Plugin

This gradle plugin packages a library source for the [Jenkins Templating Engine](https://github.com/jenkinsci/templating-engine-plugin) into a Jenkins Plugin.

## Configuration

This plugin relies on the [Gradle JPI Plugin](https://github.com/jenkinsci/gradle-jpi-plugin) to do a lot of the heavy lifting. As such, you'll need to configure both a `jte{}` block and a `jenkinsPlugin{}` block.

```groovy
jte{
    // the directory where the libraries are located
    // default value: a directory called libraries
    baseDirectory = file("libraries")

    // the identifier for this plugin when configuring its use
    // via Job DSL or JCasC. If left empty, no `@Symbol` will 
    // be configured. 
    // default value: null 
    pluginSymbol = "myCustomSymbol"
    
    // the directory where this plugin will place the generated
    // Jenkins Plugin's source code. This should only be used
    // for testing and debugging. The default behavior is to 
    // create a temporary directory.
    // default value: null
    pluginGenerationDirectory = file("${project.buildDir}/generated-plugin")
    
    // the minimum version of JTE the generated plugin requires
    // must be greater than version 2.0
    jteVersion = '2.0'
}

// for a full list of configuration options, check out the
// gradle-jpi-plugin repository. 
jenkinsPlugin{
    // version of Jenkins core this plugin depends on, must be 1.420 or later
    jenkinsVersion = '1.420'

    // ID of the plugin, defaults to the project name without trailing '-plugin'
    shortName = 'hello-world'

    // human-readable name of plugin
    // JTE note: this will be what populates the drop down for selecting
    //           this plugin as a library source.
    displayName = 'Hello World plugin built with Gradle'
}
```
