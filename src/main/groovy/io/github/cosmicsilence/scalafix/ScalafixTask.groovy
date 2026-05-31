package io.github.cosmicsilence.scalafix

import io.github.cosmicsilence.scalafix.GradleCompat
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import scalafix.interfaces.Scalafix
import scalafix.interfaces.ScalafixMainMode
import scalafix.interfaces.ScalafixRule

import javax.inject.Inject
import java.nio.file.Paths

class ScalafixTask extends SourceTask {

    private static final Logger logger = Logging.getLogger(ScalafixTask)

    @InputFile
    @Optional
    final RegularFileProperty configFile

    @Input
    @Optional
    final ListProperty<String> rules

    @Input
    ScalafixMainMode mode

    @Input
    final Property<String> scalaVersion

    @Input
    @Optional
    final ListProperty<String> compileOptions

    @Input
    @Optional
    final ListProperty<String> classpath

    @Input
    String sourceRoot

    @Internal
    Boolean semanticDbConfigured

    @Classpath
    final ConfigurableFileCollection scalafixCliClasspath

    @Classpath
    final ConfigurableFileCollection toolClasspath

    @Inject
    ScalafixTask(ObjectFactory objects, ProjectLayout layout) {
        configFile = GradleCompat.fileProperty(objects, layout)
        rules = objects.listProperty(String)
        scalaVersion = objects.property(String)
        compileOptions = objects.listProperty(String)
        classpath = objects.listProperty(String)
        scalafixCliClasspath = GradleCompat.fileCollection(objects, layout)
        toolClasspath = GradleCompat.fileCollection(objects, layout)
    }

    @TaskAction
    void run() {
        def sourcePaths = source.collect { it.toPath() }
        def configFilePath = java.util.Optional.ofNullable(configFile.orNull).map { it.asFile.toPath() }
        def scalafixCliCoordinates = ScalafixProps.getScalafixCliArtifactCoordinates(scalaVersion.get())

        logger.info(
                """Running Scalafix with the following arguments:
                  | - Mode: ${mode}
                  | - Config file: ${configFilePath}
                  | - Scalafix cli artifact: ${scalafixCliCoordinates}
                  | - External rules classpath: ${toolClasspath.asPath}
                  | - Rules: ${rules.orNull}
                  | - Scala version: ${scalaVersion.get()}
                  | - Scalac options: ${compileOptions.orNull}
                  | - Source root: ${sourceRoot}
                  | - Sources: ${sourcePaths}
                  | - Classpath: ${classpath.orNull}
                  |""".stripMargin())

        def scalafixClassloader = Classloaders.forScalafixCli(scalafixCliClasspath.files, scalafixCliCoordinates)
        def externalRulesClassloader = Classloaders.forExternalRules(toolClasspath.files, scalafixClassloader)
        def scalafixArgs = Scalafix.classloadInstance(scalafixClassloader)
                .newArguments()
                .withMode(mode)
                .withConfig(configFilePath)
                .withRules(rules.getOrElse([]))
                .withSourceroot(Paths.get(sourceRoot))
                .withPaths(sourcePaths)
                .withToolClasspath(externalRulesClassloader)
                .withClasspath(classpath.getOrElse([]).collect { Paths.get(it) } )
                .withScalaVersion(scalaVersion.get())
                .withScalacOptions(compileOptions.getOrElse([]))

        logger.info(
                """Scalafix rules:
                  | - Available: ${scalafixArgs.availableRules().collect { it.name() }}
                  | - That will run: ${scalafixArgs.rulesThatWillRun().collect { it.name() }}
                  |""".stripMargin())

        if (!scalafixArgs.rulesThatWillRun().empty) {
            assertSemanticDbIsConfigured(scalafixArgs.rulesThatWillRun())

            logger.quiet("Running Scalafix on ${sourcePaths.size} Scala source file(s)")
            def errors = scalafixArgs.run()
            if (errors.size() > 0) throw new ScalafixFailed(errors)
        } else {
            logger.warn("No Scalafix rules to run")
        }
    }

    private void assertSemanticDbIsConfigured(List<ScalafixRule> rulesThatWillRun) {
        def semanticRules = rulesThatWillRun.findAll { it.kind().isSemantic() }

        if (!semanticRules.empty && !semanticDbConfigured) {
            def ruleNames = semanticRules.collect { it.name() }.join(", ")
            throw new GradleException("SemanticDB is required to run semantic rules such as $ruleNames. " +
                    "To fix this problem, please enable 'semanticdb.autoConfigure' in the plugin extension")
        }
    }
}
