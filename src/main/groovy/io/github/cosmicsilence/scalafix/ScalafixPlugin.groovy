package io.github.cosmicsilence.scalafix

import io.github.cosmicsilence.compat.GradleCompat
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import scalafix.interfaces.ScalafixMainMode

import static scalafix.interfaces.ScalafixMainMode.CHECK
import static scalafix.interfaces.ScalafixMainMode.IN_PLACE

/** Gradle plugin for running Scalafix */
class ScalafixPlugin implements Plugin<Project> {

    private static final String EXTENSION = "scalafix"
    private static final String EXT_RULES_CONFIGURATION = "scalafix"
    private static final String SCALAFIX_CLI_CONFIGURATION_PREFIX = "scalafixCli"
    private static final String TASK_GROUP = "scalafix"
    private static final String FIX_TASK = "scalafix"
    private static final String CHECK_TASK = "checkScalafix"
    private static final String SEMANTIC_DB_TASK = "configSemanticDB"
    private static final String RULES_PROPERTY = "scalafix.rules"
    private static final String DEFAULT_CONFIG_FILE = ".scalafix.conf"

    @Override
    void apply(Project project) {
        def configuration = project.configurations.create(EXT_RULES_CONFIGURATION, { Configuration cfg ->
            cfg.description = "Dependencies containing external Scalafix rules"
        })

        def extension = project.extensions.create(EXTENSION, ScalafixExtension, project.objects, project.layout)
        RegularFile defaultConfig = locateDefaultConfigFile(project) ?: locateDefaultConfigFile(project.rootProject)

        if (defaultConfig != null) {
            GradleCompat.setConvention(extension.configFile, defaultConfig)
        }

        project.afterEvaluate {
            if (!project.plugins.hasPlugin(ScalaPlugin)) {
                throw new GradleException("The 'scala' plugin must be applied")
            }

            configureTasks(project, extension, configuration)
        }
    }

    private void configureTasks(Project project, ScalafixExtension extension, Configuration configuration) {
        def fixTask = project.tasks.create(FIX_TASK, {
            group = ScalafixPlugin.TASK_GROUP
            description = 'Runs Scalafix on Scala sources'
        })
        def checkTask = project.tasks.create(CHECK_TASK, {
            group = ScalafixPlugin.TASK_GROUP
            description = "Fails if running Scalafix produces a diff or a linter error message. Won't write to files"
        })
        project.tasks.named('check').configure { it.dependsOn checkTask }

        project.sourceSets.each { SourceSet ss ->
            if (!ScalaSourceSet.isScalaSourceSet(project, ss) || extension.ignoreSourceSets.get().contains(ss.name)) return

            def scalaSourceSet = new ScalaSourceSet(project, ss)
            def configureSemanticDb = project.objects.property(Boolean)
            def semanticDbTaskName = SEMANTIC_DB_TASK + ss.name.capitalize()
            def semanticDbTask = project.tasks.register(semanticDbTaskName, ConfigSemanticDbTask, {
                group = ScalafixPlugin.TASK_GROUP
                description = "Configures the SemanticDB Scala compiler for '${ss.name}'"
                scalaVersion.set(project.provider({ resolveScalaVersion(scalaSourceSet) }))
                semanticDbVersion = extension.semanticdb.version.orNull
                sourceSet = scalaSourceSet
                onlyIf { configureSemanticDb.getOrElse(false) }
            })
            scalaSourceSet.getCompileTask().dependsOn semanticDbTask

            def cliCfg = createScalafixCliConfiguration(project, scalaSourceSet)
            configureScalafixTaskForSourceSet(project, scalaSourceSet, IN_PLACE, fixTask, extension, configuration, cliCfg, configureSemanticDb)
            configureScalafixTaskForSourceSet(project, scalaSourceSet, CHECK, checkTask, extension, configuration, cliCfg, configureSemanticDb)
        }
    }

    private Configuration createScalafixCliConfiguration(Project project, ScalaSourceSet sourceSet) {
        def cfgName = SCALAFIX_CLI_CONFIGURATION_PREFIX + sourceSet.getName().capitalize()
        def cliCfg = project.configurations.create(cfgName, { Configuration cfg ->
            cfg.canBeConsumed = false
            cfg.canBeResolved = true
            cfg.visible = false
            cfg.transitive = true
            cfg.description = "Scalafix CLI dependencies for source set '${sourceSet.getName()}'"
        })
        cliCfg.withDependencies { deps ->
            try {
                def scalaVersion = resolveScalaVersion(sourceSet)
                deps.add(project.dependencies.create(ScalafixProps.getScalafixCliArtifactCoordinates(scalaVersion)))
            } catch (GradleException ignored) {
                // Leave the configuration empty so the ScalafixTask reaches its action phase and reports the
                // underlying error (unsupported / undetectable Scala version) from there instead of failing
                // earlier during dependency resolution.
            }
        }
        return cliCfg
    }

    private void configureScalafixTaskForSourceSet(Project project,
                                                   ScalaSourceSet sourceSet,
                                                   ScalafixMainMode taskMode,
                                                   Task parentTask,
                                                   ScalafixExtension extension,
                                                   Configuration extRulesConfiguration,
                                                   Configuration scalafixCliConfiguration,
                                                   Property<Boolean> configureSemanticDb) {
        def taskName = parentTask.name + sourceSet.getName().capitalize()
        def scalafixTask = project.tasks.register(taskName, ScalafixTask, {
            description = "${parentTask.description} in '${sourceSet.getName()}'"
            group = parentTask.group
            sourceRoot = project.projectDir.path
            source = sourceSet.getScalaSources().matching {
                include(extension.includes.get())
                exclude(extension.excludes.get())
            }
            configFile = extension.configFile
            rules.set(project.provider({
                String prop = project.findProperty(RULES_PROPERTY) ?: ''
                prop.split('\\s*,\\s*').findAll { !it.empty }.toList()
            }))
            mode = taskMode
            scalaVersion.set(project.provider({ resolveScalaVersion(sourceSet) }))
            classpath.set(project.provider({ sourceSet.getFullClasspath().collect { it.path } }))
            compileOptions.set(project.provider({ sourceSet.getCompilerOptions() }))
            scalafixCliClasspath.from(scalafixCliConfiguration)
            toolClasspath.from(extRulesConfiguration)
            semanticDbConfigured = extension.semanticdb.autoConfigure.get()

            if (extension.semanticdb.autoConfigure.get()) {
                // Auto-configures the SemanticDB compiler plugin and triggers compilation only if the Scalafix
                // task gets configured (meaning it will run).
                configureSemanticDb.set(true)
                dependsOn sourceSet.getCompileTask()
            }
            dependsOn extRulesConfiguration
        })

        parentTask.dependsOn scalafixTask
    }

    private static RegularFile locateDefaultConfigFile(Project project) {
        RegularFile configFile = project.layout.projectDirectory.file(DEFAULT_CONFIG_FILE)
        return (configFile.asFile.exists() && configFile.asFile.isFile()) ? configFile : null
    }

    private String resolveScalaVersion(ScalaSourceSet sourceSet) {
        sourceSet.getScalaVersion().orElseThrow {
            new GradleException("Unable to detect the Scala version for the '${sourceSet.getName()}' source set. Please " +
                    "ensure it declares dependency to scala-library or consider adding it to 'ignoreSourceSets'")
        }
    }
}
