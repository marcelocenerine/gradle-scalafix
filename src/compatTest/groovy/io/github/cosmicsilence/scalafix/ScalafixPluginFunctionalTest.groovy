package io.github.cosmicsilence.scalafix

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.atomic.AtomicInteger

class ScalafixPluginFunctionalTest extends Specification {

    def 'scalafixMain task should run compileScala by default'() {
        given:
        TemporaryFolder projectDir = createScalaProject()

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
    }

    def 'scalafixMain task should run compileScala when semanticdb.autoConfigure is enabled in the scalafix extension'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = true } }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
    }

    def 'scalafixMain task should not run compileScala when semanticdb.autoConfigure is disabled in the scalafix extension'() {
        given:
        TemporaryFolder projectDir = createScalaProject('''scalafix { semanticdb { autoConfigure = false } }''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-m')

        then:
        !buildResult.output.contains(':compileScala SKIPPED')
        !buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
    }

    def 'scalafixMain task should skip compileScala and log a deprecation warning when autoConfigureSemanticdb is disabled in the scalafix extension'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-m')

        then:
        !buildResult.output.contains(':compileScala SKIPPED')
        !buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')

        and:
        buildResult.output.contains("WARNING: the 'autoConfigureSemanticdb' property is deprecated. Please use 'semanticdb.autoconfigure' instead")
    }

    def 'checkScalafix task should run compileScala by default'() {
        given:
        TemporaryFolder projectDir = createScalaProject()

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
    }

    def 'checkScalafix task should run compileScala when semanticdb.autoConfigure is enabled in the scalafix extension'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = true }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
    }

    def 'checkScalafix task should not run compileScala when semanticdb.autoConfigure is disabled in the scalafix extension'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix', '-m')

        then:
        !buildResult.output.contains(':compileScala SKIPPED')
        !buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')

    }

    def 'checkScalafix task should skip compileScala and log a deprecation warning when autoConfigureSemanticdb is disabled in the scalafix extension'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix', '-m')

        then:
        !buildResult.output.contains(':compileScala SKIPPED')
        !buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')

        and:
        buildResult.output.contains("WARNING: the 'autoConfigureSemanticdb' property is deprecated. Please use 'semanticdb.autoconfigure' instead")
    }

    def 'scalafix<SourceSet> task should be created and run compile<SourceSet>Scala by default when additional source set exists in the build script'() {
        given:
        TemporaryFolder projectDir = createScalaProject('''
sourceSets {
    integTest {
        compileClasspath += sourceSets.test.compileClasspath
    }
}
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':compileIntegTestScala SKIPPED')
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
        buildResult.output.contains(':scalafixIntegTest SKIPPED')
    }

    def 'checkScalafix<SourceSet> task should be created and run compile<SourceSet>Scala by default when additional source set exists in the build script'() {
        given:
        TemporaryFolder projectDir = createScalaProject('''
sourceSets {
    integTest {
        compileClasspath += sourceSets.test.compileClasspath
    }
}
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix', '-m')

        then:
        buildResult.output.contains(':compileScala SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':compileIntegTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
        buildResult.output.contains(':checkScalafixIntegTest SKIPPED')
    }

    def 'check task should run checkScalafix tasks but not scalafix'() {
        given:
        TemporaryFolder projectDir = createScalaProject()

        when:
        BuildResult buildResult = runGradle(projectDir, 'check', '-m')

        then:
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
        buildResult.output.contains(':checkScalafix SKIPPED')
        buildResult.output.contains(':check SKIPPED')
        !buildResult.output.contains(':scalafix')
        !buildResult.output.contains(':scalafixMain')
        !buildResult.output.contains(':scalafixTest')
    }

    def 'all scalafix tasks should be grouped'() {
        given:
        TemporaryFolder projectDir = createScalaProject('''
sourceSets {
    foo {
        compileClasspath += sourceSets.main.compileClasspath
    }
}
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'tasks')

        then:
        buildResult.output.contains '''
Scalafix tasks
--------------
checkScalafix - Fails if running Scalafix produces a diff or a linter error message. Won't write to files
checkScalafixFoo - Fails if running Scalafix produces a diff or a linter error message. Won't write to files in 'foo'
checkScalafixMain - Fails if running Scalafix produces a diff or a linter error message. Won't write to files in 'main'
checkScalafixTest - Fails if running Scalafix produces a diff or a linter error message. Won't write to files in 'test'
scalafix - Runs Scalafix on Scala sources
scalafixFoo - Runs Scalafix on Scala sources in 'foo'
scalafixMain - Runs Scalafix on Scala sources in 'main'
scalafixTest - Runs Scalafix on Scala sources in 'test'
'''
    }

    def 'scalafix task should fail if the Scala version cannot be detected'() {
        given:
        TemporaryFolder projectDir = createScalaProject('''
sourceSets { 
    noScala { } 
}

scalafix { 
    semanticdb { autoConfigure = false }
}
''')
        createSourceFile(projectDir, 'object Foo', 'noScala')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains "Task :scalafixNoScala FAILED"
        err.message.contains "Unable to detect the Scala version for the 'noScala' source set. Please ensure it " +
                "declares dependency to scala-library or consider adding it to 'ignoreSourceSets'"
        err.message.contains 'BUILD FAILED'
    }

    def 'scalafix task should fail if the Scala version is not supported'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }', '2.10.7')
        createSourceFile(projectDir, 'object Foo', 'main')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains 'Task :scalafixMain FAILED'
        err.message.contains "Scala version '2.10.7' is not supported"
        err.message.contains 'BUILD FAILED'
    }

    def '*.semanticdb files should be created during compilation when semanticdb.autoConfigure is true and scalafix task is run'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        File mainSrc = createSourceFile(projectDir, 'object Foo', 'main')
        File testSrc = createSourceFile(projectDir, 'object FooTest', 'test')
        File buildDir = new File(projectDir.root, 'build')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        new File(buildDir, "classes/scala/main/META-INF/semanticdb/src/main/scala/dummy/${mainSrc.name}.semanticdb").exists()
        new File(buildDir, "classes/scala/test/META-INF/semanticdb/src/test/scala/dummy/${testSrc.name}.semanticdb").exists()
    }

    def '*.semanticdb files should not be created during compilation when autoConfigureSemanticdb is false and scalafix task is run'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { autoConfigureSemanticdb = false }')
        createSourceFile(projectDir, 'object Foo', 'main')
        createSourceFile(projectDir, 'object FooTest', 'test')
        File buildDir = new File(projectDir.root, 'build')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        !buildDir.exists()
    }

    def '*.semanticdb files should not be created during compilation when semanticdb.autoConfigure is false and scalafix task is run'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }')
        createSourceFile(projectDir, 'object Foo', 'main')
        createSourceFile(projectDir, 'object FooTest', 'test')
        File buildDir = new File(projectDir.root, 'build')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        !buildDir.exists()
    }

    def '*.semanticdb files should not be created during compilation when scalafix task is not run'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createSourceFile(projectDir, 'object Foo', 'main')
        createSourceFile(projectDir, 'object FooTest', 'test')
        File buildDir = new File(projectDir.root, 'build')

        when:
        runGradle(projectDir, 'compileScala', 'compileTestScala')

        then:
        buildDir.eachFileRecurse {
            assert !it.name.endsWith('.semanticdb')
        }
    }

    def 'checkScalafix and scalafix tasks should not fail when no rules are informed'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createSourceFile(projectDir, 'object Foo')

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix', 'scalafix')

        then:
        buildResult.output.contains '''
> Task :checkScalafixMain
No Scalafix rules to run'''
        buildResult.output.contains '''
> Task :scalafixMain
No Scalafix rules to run'''
        buildResult.output.contains 'BUILD SUCCESSFUL'
    }

    def 'checkScalafix and scalafix tasks should not fail when there is no source file to be processed'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix', 'scalafix')

        then:
        buildResult.output.contains ':checkScalafixMain NO-SOURCE'
        buildResult.output.contains ':checkScalafixTest NO-SOURCE'
        buildResult.output.contains ':checkScalafix UP-TO-DATE'
        buildResult.output.contains ':scalafixMain NO-SOURCE'
        buildResult.output.contains ':scalafixTest NO-SOURCE'
        buildResult.output.contains ':scalafix UP-TO-DATE'
        buildResult.output.contains 'BUILD SUCCESSFUL'
    }

    def 'scalafix should run semantic rewrite rule and fix input source files'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused, ExplicitResultTypes ]')
        File src = createSourceFile(projectDir, '''
import scala.util.Random
object HelloWorld {
  def foo = Map(1 -> "one")
}
''')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        src.getText() == '''
object HelloWorld {
  def foo: Map[Int,String] = Map(1 -> "one")
}
'''
    }

    def 'checkScalafix should run semantic rewrite rule and fail the build without fixing input source files'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        String originalSrcContent = '''
import scala.util.Random
object HelloWorld
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        runGradle(projectDir, 'checkScalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains 'Task :checkScalafixMain FAILED'
        err.message.contains 'A file on disk does not match the file contents if it was fixed with Scalafix'
        srcFile.getText() == originalSrcContent
    }

    def 'scalafix should fail to run semantic rules if the SemanticDB compiler plugin is not configured'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }')
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        createSourceFile(projectDir, 'object Foo')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains 'Task :scalafixMain FAILED'
        err.message.contains 'The semanticdb compiler plugin is required to run semantic rules such as RemoveUnused.'
    }

    def 'checkScalafix should fail to run semantic rules if the SemanticDB compiler plugin is not configured'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }')
        createScalafixConfig(projectDir, 'rules = [ ExplicitResultTypes ]')
        createSourceFile(projectDir, 'object Foo')

        when:
        runGradle(projectDir, 'checkScalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains 'Task :checkScalafixMain FAILED'
        err.message.contains 'The semanticdb compiler plugin is required to run semantic rules such as ExplicitResultTypes.'
    }

    def 'scalafix should skip non-included source files'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { includes = ["**/animals/**"] }')
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        String dogSrcContent = '''
import scala.util.Random
object Dog
'''
        String dogTestSrcContent = '''
import scala.util.Random
object DogTest
'''
        String appleSrcContent = '''
import scala.util.Random
object Apple
'''
        String appleTestSrcContent = '''
import scala.util.Random
object AppleTest
'''
        File dogSrcFile = createSourceFile(projectDir, dogSrcContent, 'main', 'animals')
        File dogTestSrcFile = createSourceFile(projectDir, dogTestSrcContent, 'test', 'animals')
        File appleSrcFile = createSourceFile(projectDir, appleSrcContent, 'main', 'fruits')
        File appleTestSrcFile = createSourceFile(projectDir, appleTestSrcContent, 'test', 'fruits')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        buildResult.output.contains '''
> Task :scalafixMain
Running Scalafix on 1 Scala source file(s)
'''
        buildResult.output.contains '''
> Task :scalafixTest
Running Scalafix on 1 Scala source file(s)
'''
        dogSrcFile.getText() != dogSrcContent
        dogTestSrcFile.getText() != dogTestSrcContent
        appleSrcFile.getText() == appleSrcContent
        appleTestSrcFile.getText() == appleTestSrcContent
    }

    def 'checkScalafix should skip non-included source files'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { includes = ["**/fruits/**"] }')
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        createSourceFile(projectDir, '''
import scala.util.Random
object Dog
''', 'main', 'animals')
        createSourceFile(projectDir, '''
import scala.util.Random
object DogTest
''', 'test', 'animals')
        createSourceFile(projectDir, 'object Apple', 'main', 'fruits')
        createSourceFile(projectDir, 'object AppleTest', 'test', 'fruits')

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL' // offending files in 'animals' are skipped
        buildResult.output.contains '''
> Task :checkScalafixMain
Running Scalafix on 1 Scala source file(s)
'''
        buildResult.output.contains '''
> Task :checkScalafixTest
Running Scalafix on 1 Scala source file(s)
'''
    }

    def 'scalafix should skip excluded source files'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { excludes = ["**/dummy/**"] }')
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        String originalSrcContent = '''
import scala.util.Random
object HelloWorld
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        srcFile.getText() == originalSrcContent
    }

    def 'checkScalafix should skip excluded source files'() {
        given:
        TemporaryFolder projectDir = createScalaProject('''
scalafix {
  excludes = ["**/dummy/**"]
  semanticdb {
    autoConfigure = false
  }
}
''')
        createScalafixConfig(projectDir, '''
rules = [ DisableSyntax ]
DisableSyntax.noVars = true
''')
        String originalSrcContent = '''
object HelloWorld {
  var i: Int = 5
}
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        srcFile.getText() == originalSrcContent
    }

    def 'scalafix should run syntactic linter rule and fail the build if any violation is reported'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }')
        createScalafixConfig(projectDir, '''
rules = [ DisableSyntax ]
DisableSyntax.noVars = true
''')
        String originalSrcContent = '''
object HelloWorld {
  var msg: String = "hello, world!"
}
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        runGradle(projectDir, 'scalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains 'Task :scalafixMain FAILED'
        err.message.contains 'A linter error was reported'
        err.message.contains 'error: [DisableSyntax.var] mutable state should be avoided'
        srcFile.getText() == originalSrcContent
    }

    def 'checkScalafix should run syntactic linter rule and fail the build if any violation is reported'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }')
        createScalafixConfig(projectDir, '''
rules = [ DisableSyntax ]
DisableSyntax.noVars = true
''')
        String originalSrcContent = '''
object HelloWorld {
  var msg: String = "hello, world!"
}
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        runGradle(projectDir, 'checkScalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains 'Task :checkScalafixMain FAILED'
        err.message.contains 'A linter error was reported'
        err.message.contains 'error: [DisableSyntax.var] mutable state should be avoided'
        srcFile.getText() == originalSrcContent
    }

    def 'scalafix should run semantic rewrite rule and leave wart-free code unchanged'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused ]')
        String originalSrcContent = '''
import scala.util.Random
object HelloWorld {
  val i: Int = Random.nextInt()
}
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        srcFile.getText() == originalSrcContent
    }

    def 'checkScalafix should run syntactic linter rule and succeed if there are no violations'() {
        given:
        TemporaryFolder projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }')
        createScalafixConfig(projectDir, '''
rules = [ DisableSyntax ]
DisableSyntax.noVars = true
''')
        String originalSrcContent = '''
object HelloWorld {
  val i: Int = 3
}
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        srcFile.getText() == originalSrcContent
    }

    def 'scalafix should run single rule informed via command line even when it is not defined in the config file'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        File src = createSourceFile(projectDir, '''
import scala.util.Random
object Foo {
  def proc { }
}
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-Pscalafix.rules=ProcedureSyntax')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        src.getText() == '''
import scala.util.Random
object Foo {
  def proc: Unit = { }
}
'''
    }

    def 'scalafix should run multiple rules informed via command line even when they are not defined in the config file'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        File src = createSourceFile(projectDir, '''
import scala.util.Random
object Foo {
  def proc { }
}
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-Pscalafix.rules=ProcedureSyntax,RemoveUnused')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        src.getText() == '''
object Foo {
  def proc: Unit = { }
}
'''
    }

    def 'scalafix should only run rule informed via command line when there are other rules defined in the config file'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createScalafixConfig(projectDir, '''
rules = [
  DisableSyntax
  RemoveUnused
  ProcedureSyntax
]

DisableSyntax.noVars = true
''')
        File src = createSourceFile(projectDir, '''
import scala.util.Random
object Foo {
  var foo = "foo"
  def proc { }
}
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-Pscalafix.rules=ProcedureSyntax')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        src.getText() == '''
import scala.util.Random
object Foo {
  var foo = "foo"
  def proc: Unit = { }
}
'''
    }

    def 'scalafix should run rules defined in the config file if blank value is informed via command line'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ RemoveUnused, ProcedureSyntax ]')
        File src = createSourceFile(projectDir, '''
import scala.util.Random
object Foo {
  def proc { }
}
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-Pscalafix.rules=')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        src.getText() == '''
object Foo {
  def proc: Unit = { }
}
'''
    }

    def 'scalafix should fail if invalid rule is informed via command line'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createSourceFile(projectDir, 'object Bar')

        when:
        runGradle(projectDir, 'scalafix', '-Pscalafix.rules=Foo')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains "scalafix.internal.interfaces.ScalafixMainArgsException: Unknown rule 'Foo'"
    }

    def 'scalafix should fail if invalid rule is informed via config file'() {
        given:
        TemporaryFolder projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ Foo ]')
        createSourceFile(projectDir, 'object Bar')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains "scalafix.internal.interfaces.ScalafixMainArgsException: Unknown rule 'Foo'"
    }

    @Unroll
    def 'scalafix should run semantic/syntactic built-in and external rules in projects using Scala #scalaVersion'() {
        given:
        def scalaBinaryVersion = scalaVersion.substring(0, scalaVersion.lastIndexOf('.'))
        TemporaryFolder projectDir = createScalaProject("""
dependencies {
    scalafix 'com.github.liancheng:organize-imports_${scalaBinaryVersion}:0.5.0'
}
""", scalaVersion)

        createScalafixConfig(projectDir, '''
rules = [
  RemoveUnused
  LeakingImplicitClassVal
  NoValInForComprehension
  ProcedureSyntax
  OrganizeImports
]

DisableSyntax.noVars = true

RemoveUnused {
  imports = false
}

OrganizeImports {
    groupedImports = Merge
    groupExplicitlyImportedImplicitsSeparately = true
    groups = [ "re:javax?\\\\.", "scala." ]
    removeUnused = false
}
''')

        File removeUnusedSource = createSourceFile(projectDir, '''
object RemoveUnusedTest {
  private def unused1 = "remove me"
  def foo(): Unit = {
    val unused2 = "remove me"
    println("foo")
  }
}
''')
        File leakingImplicitClassValSource = createSourceFile(projectDir, '''
object LeakingImplicitClassValTest {
    implicit class XtensionVal(val str: String) extends AnyVal {
      def doubled: String = str + str
    }
}
''')
        File noValInForComprehensionSource = createSourceFile(projectDir, '''
object NoValInForComprehensionTest {
    for {
      n <- List(1, 2, 3)
      val inc = n + 1
    } println(inc)
}
''')
        File procedureSyntaxSource = createSourceFile(projectDir, '''
object ProcedureSyntaxTest {
    def debug { println("debug") }
}
''')
        File organizeImportsSource = createSourceFile(projectDir, '''
import scala.concurrent.duration._
import scala.language.postfixOps
import sun.misc.Unsafe
import java.lang.String
import scala.collection.immutable.List
import scala.collection.immutable.Set

object OrganizeImportsTest
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix')

        then:
        buildResult.output.contains 'Running Scalafix on 5 Scala source file(s)'
        buildResult.output.contains 'BUILD SUCCESSFUL'
        removeUnusedSource.getText() == '''
object RemoveUnusedTest {
  
  def foo(): Unit = {
    
    println("foo")
  }
}
'''
        leakingImplicitClassValSource.getText() == '''
object LeakingImplicitClassValTest {
    implicit class XtensionVal(private val str: String) extends AnyVal {
      def doubled: String = str + str
    }
}
'''
        noValInForComprehensionSource.getText() == '''
object NoValInForComprehensionTest {
    for {
      n <- List(1, 2, 3)
      inc = n + 1
    } println(inc)
}
'''
        procedureSyntaxSource.getText() == '''
object ProcedureSyntaxTest {
    def debug: Unit = { println("debug") }
}
'''
        organizeImportsSource.getText() == '''
import java.lang.String

import scala.collection.immutable.{List, Set}
import scala.concurrent.duration._

import sun.misc.Unsafe

import scala.language.postfixOps

object OrganizeImportsTest
'''

        where:
        scalaVersion || _
        '2.11.12'    || _
        '2.12.12'    || _
        '2.12.13'    || _
        '2.12.14'    || _
        '2.13.4'     || _
        '2.13.5'     || _
        '2.13.6'     || _
    }

    private BuildResult runGradle(TemporaryFolder projectDir, String... arguments) {
        return GradleRunner.create()
                .withProjectDir(projectDir.getRoot())
                .withArguments(arguments.toList() + '--stacktrace')
                .withGradleVersion(System.getProperty('compat.gradle.version'))
                .withPluginClasspath()
                .forwardOutput()
                .build()
    }

    private TemporaryFolder createScalaProject(String additionalBuildInstructions = '', String scalaVersion = '2.13.6') {
        TemporaryFolder projectDir = new TemporaryFolder()
        projectDir.create()
        projectDir.newFile("build.gradle").write """
plugins {
    id 'scala'
    id 'io.github.cosmicsilence.scalafix'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation "org.scala-lang:scala-library:${scalaVersion}"
}

tasks.withType(ScalaCompile) {
    // needed for RemoveUnused rule
    scalaCompileOptions.additionalParameters = [ '-Ywarn-unused' ]
}

$additionalBuildInstructions
"""
        projectDir.newFile("settings.gradle").write "rootProject.name = 'hello-world'"
        projectDir
    }

    private File createScalafixConfig(TemporaryFolder projectDir, String content) {
        projectDir.newFile(".scalafix.conf").write content
    }

    private File createSourceFile(TemporaryFolder projectDir, String content, String sourceSet = 'main', String pkgName = "dummy") {
        File pkgFolder = new File(projectDir.root, "src/$sourceSet/scala/$pkgName")

        if (!pkgFolder.exists()) pkgFolder.mkdirs()

        File scalaSrcFile = new File(pkgFolder, "source_${fileCount.incrementAndGet()}.scala")
        scalaSrcFile.write content
        scalaSrcFile
    }

    private AtomicInteger fileCount = new AtomicInteger()
}
