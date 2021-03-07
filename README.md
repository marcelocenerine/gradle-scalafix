[![Build Status](https://travis-ci.com/cosmicsilence/gradle-scalafix.svg?branch=master)](https://travis-ci.com/cosmicsilence/gradle-scalafix)
[![Download](https://api.bintray.com/packages/cosmicsilence/maven/gradle-scalafix/images/download.svg)](https://bintray.com/cosmicsilence/maven/gradle-scalafix/_latestVersion)
[![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)

## gradle-scalafix
This plugin allows you to run [Scalafix](https://scalacenter.github.io/scalafix/) (a refactoring and linting tool for Scala)
on Gradle projects. It supports both syntactic and semantic rules and lets you load your own custom rules via Gradle dependencies.

&nbsp;
## Usage

*Make sure you are using Gradle version 4.9 or later (except 5.0).*

To use the Scalafix plugin, please include the following snippet in your build script:

```groovy
plugins {
    id 'scala' // <-- required
    id 'io.github.cosmicsilence.scalafix' version '<version>'
}
```


&nbsp;
## Selecting Rules

By default, Scalafix won't run any rules unless they are specified via configuration file or command line argument. In 
order to decide what rules to pick and how to configure them, please check out Scalafix's [built-in 
rules](https://scalacenter.github.io/scalafix/docs/rules/overview.html) as well as custom rules provided by the 
community (e.g. [scaluzzi](https://github.com/vovapolu/scaluzzi)).

##### 1. Via config file:
This is the recommended approach if you want to repeatedly run Scalafix as part of your build and day-to-day development.
For that you need to create a file named `.scalafix.conf` in the root directory of your project and enter the configuration 
for the rules you want to have enabled. The configuration uses the HOCON syntax and is well documented on the Scalafix 
[website](https://scalacenter.github.io/scalafix/docs/users/configuration.html). If you are using external/custom rules,
please take a look at the documentation provided by them. Below is a basic example of using one of the Scalafix's built-in 
rules so you can quickly get started:


```
rules = [
  DisableSyntax
]

DisableSyntax.noNulls = true
DisableSyntax.noVars = true
```

> **TIP:** Multi-modules Gradle projects are allowed to define different combinations of Scalafix rules/settings for each individual 
sub-project. This can be achieved by having a `.scalafix.conf` file defined in the sub-project folder. The Scalafix
plugin will first try to locate the config file at the sub-project level and, if not found, at the root project level. It
is also possible to inform a custom path via the plugin extension in the build script. See the [extension](#extension)
section for more details.


##### 2. Via command line argument:
There are times when it may be desirable to run a single or a subset of Scalafix rules without having to touch the configuration
file. For those one-off scenarios, you can inform the rule(s) you want to run using the `scalafix.rules` property:

```
./gradlew scalafix -Pscalafix.rules=DisableSyntax
```

>**TIP:** Use a comma character as delimiter when informing multiple rules.


#### Required Compiler Options
Some Scalafix rules (e.g. `RemoveUnused`) require additional options to be passed into the Scala compiler. That can 
be achieved as following:

```
tasks.withType(ScalaCompile) {
    scalaCompileOptions.additionalParameters = [ "-Ywarn-unused" ]
}

```


&nbsp;
## Tasks

The following Gradle tasks are created when the Scalafix plugin is applied to a project:

| Name                       | Description          |
|:---------------------------|----------------------|
|*`scalafix`*                |Runs rewrite and linter rules for all source sets. Rewrite rules may modify files in-place whereas linter rules will print diagnostics to Gradle's output.|
|*`scalafix<SourceSet>`*     |Same as above, but for a single source set (e.g. *`scalafixMain`*, *`scalafixTest`*, *`scalafixFoo`*).|
|*`checkScalafix`*           |Checks that source files of all source sets are compliant to rewrite and linter rules. Any violation is printed to Gradle's output and the task exits with an error. No source file gets modified. This task is automatically triggered by the `check` task.|
|*`checkScalafix<SourceSet>`*|Same as above, but for a single source set (e.g. *`checkScalafixMain`*, *`checkScalafixTest`*, *`checkScalafixBar`*).|

>**NOTE:** If the **SemanticDB** Scala compiler plugin is enabled (see the [extension](#extension) section for more details),
any of these tasks will trigger partial or complete compilation of Scala source files.


&nbsp;
<a name="extension"></a>
## Extension
The plugin defines an extension with the namespace `scalafix`. The following properties can be configured:

| Property name            | Type                 | Default value | Description |
|:-------------------------|----------------------|---------------|-------------|
|`configFile`              |`RegularFileProperty` |\<empty\>      |Used to inform a different location/name for the Scalafix configuration file. If not informed, the plugin will search for a `.scalafix.conf` in the current project directory and (if not found) in the root project directory. |
|`configFile`              |`String`              |\<empty\>      |Same as above. |
|`includes`                |`SetProperty<String>` |\<empty\>      |[Ant-like pattern](https://ant.apache.org/manual/dirtasks.html) to filter what Scala source files should be processed by Scalafix. Filter is applied to package portion of the source file path. By default all files are included. |
|`excludes`                |`SetProperty<String>` |\<empty\>      |[Ant-like pattern](https://ant.apache.org/manual/dirtasks.html) to exclude Scala source files from being processed by Scalafix. Filter is applied to package portion of the source file path. By default no files are excluded. |
|`ignoreSourceSets`        |`SetProperty<String>` |\<empty\>      |Name of source sets to which the Scalafix plugin should not be applied (by default this plugin is applied to all source sets defined in the project). This option can be used (e.g.) to ignore source sets that point to the same source files of other source sets (which would cause them to be processed twice). Be careful with plugin application ordering. E.g. when using this plugin together with scoverage, scoverage plugin should be applied first.|
|`autoConfigureSemanticdb` |`Boolean`             |`true`         |Used to indicate whether the Scalafix plugin should auto-configure the SemanticDB compiler plugin. This is mandatory to run semantic rules. If set to `true` (default), the Scalafix Gradle tasks will require the corresponding Scala compiler tasks to run prior to them. The Scalafix plugin provides only the minimum required configuration to get SemanticDB set up (`-Xplugin:`, `-P:semanticdb:sourceroot:` and `-Yrangepos`). If you need to use more advanced settings, please consult the [Scalafix docs](https://scalacenter.github.io/scalafix/docs/users/installation.html#exclude-files-from-semanticdb) or the [SemanticDB docs](https://scalameta.org/docs/semanticdb/guide.html#scalac-compiler-plugin). Any additional SemanticDB options can be informed through Gradle's `ScalaCompile` task as shown earlier. If your project only uses syntactic rules, then it's recommended that this property is set to `false` to shorten the running time. |


Example:
```
scalafix {
    configFile = file("config/myscalafix.conf")
    includes = ["/com/**/*.scala"]
    excludes = ["**/generated/**"]
    ignoreSourceSets = ["scoverage"]
    autoConfigureSemanticdb = false
}
```


&nbsp;
## Loading Custom Rules
One of the nice things about the Scalafix tool is that it is extensible. That means that you can implement your own custom
rules (or maybe simply reuse rules somebody else has already done) and load them into Scalafix to run in your projects.
The Gradle Scalafix plugin lets you take advantage of that feature by allowing custom rules to be informed as regular
dependencies in your Gradle build script using the `scalafix` configuration. Example:

```
dependencies {
    scalafix "com.github.vovapolu:scaluzzi_${scalaBinaryVersion}:0.1.18"
    scalafix "com.github.liancheng:organize-imports_${scalaBinaryVersion}:0.5.0"
}
```


&nbsp;
## Maintainers

- Felix Sanjuan - [`@fsanjuan`](https://github.com/fsanjuan)
- Marcelo Cenerino - [`@marcelocenerine`](https://github.com/marcelocenerine)
