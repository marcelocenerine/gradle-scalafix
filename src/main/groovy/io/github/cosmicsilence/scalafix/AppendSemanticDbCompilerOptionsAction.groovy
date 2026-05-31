package io.github.cosmicsilence.scalafix

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.scala.ScalaCompile

/**
 * Appends SemanticDB-related compiler options to a {@link ScalaCompile} task at execution time.
 * Registered unconditionally via {@code doFirst} so that the task's identity is stable across
 * invocations; the action no-ops when {@code configureSemanticDb} is not set, which is the case
 * when no scalafix task is being run.
 *
 * The mutated {@code additionalParameters} list itself is not part of the task's input snapshot,
 * but the gating {@code Property<Boolean>} is captured via {@code scalaCompilerPlugins} (resolved
 * to either the semanticdb plugin classpath or empty depending on the same Property), so cache
 * keys still vary correctly between invocations that wire SemanticDB and those that do not.
 */
class AppendSemanticDbCompilerOptionsAction implements Action<Task> {

    private final Property<Boolean> configureSemanticDb
    private final Property<String> scalaVersion
    private final File projectDir
    private final FileCollection scala2CompilerPluginFiles

    AppendSemanticDbCompilerOptionsAction(Property<Boolean> configureSemanticDb,
                                          Property<String> scalaVersion,
                                          File projectDir,
                                          FileCollection scala2CompilerPluginFiles) {
        this.configureSemanticDb = configureSemanticDb
        this.scalaVersion = scalaVersion
        this.projectDir = projectDir
        this.scala2CompilerPluginFiles = scala2CompilerPluginFiles
    }

    @Override
    void execute(Task task) {
        if (!configureSemanticDb.getOrElse(false)) return

        def compile = (ScalaCompile) task
        def existing = compile.scalaCompileOptions.additionalParameters ?: []
        def additions

        if (ScalaVersions.isScala3(scalaVersion.get())) {
            // It's currently not possible to set `-sourceroot` in a fully cache-friendly way:
            // https://github.com/gradle/gradle/issues/27161
            additions = ['-Xsemanticdb', '-sourceroot', projectDir.absolutePath]
        } else {
            // ScalaCompile.destinationDirectory was added in Gradle 6.1
            def outputDir = compile.hasProperty('destinationDirectory') ?
                    compile.destinationDirectory.get().asFile.toPath() :
                    compile.destinationDir.toPath()
            // Setting `sourceroot` to the project's absolute path is problematic for large code bases that require
            // aggressive caching: any difference in compiler options between machines forces Gradle to recompile,
            // rather than to download existing compiled artifacts. For that reason, `sourceroot` is set relative
            // to `targetroot`. For more context, see: https://github.com/scalameta/scalameta/issues/2515
            def relSourceRoot = outputDir.relativize(projectDir.toPath())
            additions = ['-Yrangepos', '-P:semanticdb:sourceroot:targetroot:' + relSourceRoot]

            if (scala2CompilerPluginFiles != null) {
                // Gradle < 6.4 has no ScalaCompile.scalaCompilerPlugins property; fall back to
                // injecting the semanticdb-scalac jar via -Xplugin:<paths>.
                additions += ['-Xplugin:' + scala2CompilerPluginFiles.asPath]
            }
        }

        compile.scalaCompileOptions.additionalParameters = existing + additions
    }
}
