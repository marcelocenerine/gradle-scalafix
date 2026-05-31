package io.github.cosmicsilence.scalafix

import org.gradle.api.Project
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class AppendSemanticDbCompilerOptionsActionTest extends Specification {

    private static final List<String> COMPILER_OPTS = ['-Xfoo']

    private Project project

    def setup() {
        project = buildScalaProject()
    }

    def 'should no-op when configureSemanticDb is false'() {
        given:
        def compileTask = project.tasks.compileScala
        def action = action(false, '2.13.15', null)

        when:
        action.execute(compileTask)

        then:
        compileTask.scalaCompileOptions.additionalParameters == COMPILER_OPTS
    }

    def 'should no-op when configureSemanticDb is not set'() {
        given:
        def compileTask = project.tasks.compileScala
        def configureSemanticDb = project.objects.property(Boolean)
        def action = new AppendSemanticDbCompilerOptionsAction(
                configureSemanticDb,
                project.objects.property(String).value('2.13.15'),
                project.projectDir,
                null)

        when:
        action.execute(compileTask)

        then:
        compileTask.scalaCompileOptions.additionalParameters == COMPILER_OPTS
    }

    def 'should append SemanticDB compiler options for Scala 2.x'() {
        given:
        def compileTask = project.tasks.compileScala
        def action = action(true, '2.13.15', null)

        when:
        action.execute(compileTask)

        then:
        compileTask.scalaCompileOptions.additionalParameters == COMPILER_OPTS + [
                '-Yrangepos', '-P:semanticdb:sourceroot:targetroot:../../../..'
        ]
    }

    def 'should append SemanticDB compiler options for Scala 3.x'() {
        given:
        def compileTask = project.tasks.compileScala
        def action = action(true, '3.3.1', null)

        when:
        action.execute(compileTask)

        then:
        compileTask.scalaCompileOptions.additionalParameters == COMPILER_OPTS + [
                '-Xsemanticdb', '-sourceroot', project.projectDir.absolutePath
        ]
    }

    def 'should include -Xplugin flag when scala2CompilerPluginFiles is provided'() {
        given:
        def compileTask = project.tasks.compileScala
        def pluginJar = new File(project.projectDir, 'semanticdb.jar')
        pluginJar.createNewFile()
        def fallback = project.files(pluginJar)
        def action = action(true, '2.13.15', fallback)

        when:
        action.execute(compileTask)

        then:
        def params = compileTask.scalaCompileOptions.additionalParameters
        params.any { it.startsWith('-Xplugin:') && it.contains('semanticdb.jar') }
    }

    def 'should not include -Xplugin flag for Scala 3.x even when scala2CompilerPluginFiles is provided'() {
        given:
        def compileTask = project.tasks.compileScala
        def pluginJar = new File(project.projectDir, 'semanticdb.jar')
        pluginJar.createNewFile()
        def fallback = project.files(pluginJar)
        def action = action(true, '3.3.1', fallback)

        when:
        action.execute(compileTask)

        then:
        def params = compileTask.scalaCompileOptions.additionalParameters
        !params.any { it.startsWith('-Xplugin:') }
    }

    private AppendSemanticDbCompilerOptionsAction action(boolean configureSemanticDb,
                                                         String scalaVersion,
                                                         def scala2CompilerPluginFiles) {
        return new AppendSemanticDbCompilerOptionsAction(
                project.objects.property(Boolean).value(configureSemanticDb),
                project.objects.property(String).value(scalaVersion),
                project.projectDir,
                scala2CompilerPluginFiles)
    }

    private Project buildScalaProject() {
        def project = ProjectBuilder.builder().build()

        project.with {
            apply plugin: 'scala'

            repositories { mavenCentral() }

            tasks.withType(ScalaCompile) {
                scalaCompileOptions.additionalParameters = COMPILER_OPTS
            }
        }

        return project
    }
}
