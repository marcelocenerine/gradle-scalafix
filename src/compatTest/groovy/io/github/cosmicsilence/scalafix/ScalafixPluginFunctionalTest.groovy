package io.github.cosmicsilence.scalafix

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

// needed for proper discovery of condition method
import static io.github.cosmicsilence.scalafix.ScalafixPluginFunctionalTest.isScalaVersionSupported

class ScalafixPluginFunctionalTest extends Specification {

    private static final String SCALA_2_VERSION = '2.12.21'
    private static final String SCALA_3_VERSION = '3.3.7'

    def 'scalafixMain task should run compileScala by default'() {
        given:
        File projectDir = createScalaProject()

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-m')

        then:
        buildResult.output.contains('''
:compileScala SKIPPED
:scalafixMain SKIPPED
''')
        buildResult.output.contains('''
:compileTestScala SKIPPED
:scalafixTest SKIPPED
:scalafix SKIPPED
''')
    }

    def 'scalafixMain task should run compileScala when semanticdb.autoConfigure is enabled in the scalafix extension'() {
        given:
        File projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = true } }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-m')

        then:
        buildResult.output.contains('''
:compileScala SKIPPED
:scalafixMain SKIPPED
''')
        buildResult.output.contains('''
:compileTestScala SKIPPED
:scalafixTest SKIPPED
:scalafix SKIPPED
''')
    }

    def 'scalafixMain task should not run compileScala when semanticdb.autoConfigure is disabled in the scalafix extension'() {
        given:
        File projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-m')

        then:
        buildResult.output.contains(''':scalafixMain SKIPPED
:scalafixTest SKIPPED
:scalafix SKIPPED
''')
        !buildResult.output.contains(':compileScala')
        !buildResult.output.contains(':compileTestScala')
    }

    def 'checkScalafix task should run compileScala by default'() {
        given:
        File projectDir = createScalaProject()

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix', '-m')

        then:
        buildResult.output.contains('''
:compileScala SKIPPED
:checkScalafixMain SKIPPED
''')
        buildResult.output.contains('''
:compileTestScala SKIPPED
:checkScalafixTest SKIPPED
:checkScalafix SKIPPED
''')
    }

    def 'checkScalafix task should run compileScala when semanticdb.autoConfigure is enabled in the scalafix extension'() {
        given:
        File projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = true } }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix', '-m')

        then:
        buildResult.output.contains('''
:compileScala SKIPPED
:checkScalafixMain SKIPPED
''')
        buildResult.output.contains('''
:compileTestScala SKIPPED
:checkScalafixTest SKIPPED
:checkScalafix SKIPPED
''')
    }

    def 'checkScalafix task should not run compileScala when semanticdb.autoConfigure is disabled in the scalafix extension'() {
        given:
        File projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }')

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix', '-m')

        then:
        buildResult.output.contains(''':checkScalafixMain SKIPPED
:checkScalafixTest SKIPPED
:checkScalafix SKIPPED
''')
        !buildResult.output.contains(':compileScala')
        !buildResult.output.contains(':compileTestScala')
    }

    def 'scalafix<SourceSet> task should be created and run compile<SourceSet>Scala by default when additional source set exists in the build script'() {
        given:
        File projectDir = createScalaProject('''
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
        buildResult.output.contains(':scalafixMain SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':scalafixTest SKIPPED')
        buildResult.output.contains(':compileIntegTestScala SKIPPED')
        buildResult.output.contains(':scalafixIntegTest SKIPPED')
    }

    def 'checkScalafix<SourceSet> task should be created and run compile<SourceSet>Scala by default when additional source set exists in the build script'() {
        given:
        File projectDir = createScalaProject('''
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
        buildResult.output.contains(':checkScalafixMain SKIPPED')
        buildResult.output.contains(':compileTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixTest SKIPPED')
        buildResult.output.contains(':compileIntegTestScala SKIPPED')
        buildResult.output.contains(':checkScalafixIntegTest SKIPPED')
    }

    def 'check task should run checkScalafix tasks but not scalafix'() {
        given:
        File projectDir = createScalaProject()

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
        File projectDir = createScalaProject('''
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
        File projectDir = createScalaProject('''
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
        err.message.contains 'Task :scalafixNoScala FAILED'
        err.message.contains "Unable to detect the Scala version for the 'noScala' source set. Please ensure it " +
                "declares dependency to scala-library or consider adding it to 'ignoreSourceSets'"
        err.message.contains 'BUILD FAILED'
    }

    def 'scalafix task should fail if the Scala version is not supported'() {
        given:
        File projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }', '2.10.7')
        createSourceFile(projectDir, 'object Foo', 'main')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains 'Task :scalafixMain FAILED'
        err.message.contains "Scala version '2.10.7' is not supported"
        err.message.contains 'BUILD FAILED'
    }

    def 'SemanticDB files should be created when scalafix is run with Scala 2.x'() {
        given:
        File projectDir = createScalaProject()
        File mainSrc = createSourceFile(projectDir, 'object Foo', 'main')
        File testSrc = createSourceFile(projectDir, 'object FooTest', 'test')
        File buildDir = new File(projectDir, 'build')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        new File(buildDir, "classes/scala/main/META-INF/semanticdb/src/main/scala/dummy/${mainSrc.name}.semanticdb").exists()
        new File(buildDir, "classes/scala/test/META-INF/semanticdb/src/test/scala/dummy/${testSrc.name}.semanticdb").exists()
    }

    @Requires({ isScalaVersionSupported(SCALA_3_VERSION) })
    def 'SemanticDB files should be created when scalafix is run with Scala 3.x'() {
        given:
        File projectDir = createScalaProject('', SCALA_3_VERSION)
        File mainSrc = createSourceFile(projectDir, 'object Foo', 'main')
        File testSrc = createSourceFile(projectDir, 'object FooTest', 'test')
        File buildDir = new File(projectDir, 'build')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        new File(buildDir, "classes/scala/main/META-INF/semanticdb/src/main/scala/dummy/${mainSrc.name}.semanticdb").exists()
        new File(buildDir, "classes/scala/test/META-INF/semanticdb/src/test/scala/dummy/${testSrc.name}.semanticdb").exists()
    }

    def 'SemanticDB files should not be created when scalafix is run with semanticdb.autoConfigure = false'() {
        given:
        File projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }')
        createSourceFile(projectDir, 'object Foo', 'main')
        createSourceFile(projectDir, 'object FooTest', 'test')
        File classesDir = new File(projectDir, 'build/classes')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        !classesDir.exists()
    }

    def 'SemanticDB files should not be created when scalafix is not run'() {
        given:
        File projectDir = createScalaProject()
        createSourceFile(projectDir, 'object Foo', 'main')
        createSourceFile(projectDir, 'object FooTest', 'test')
        File buildDir = new File(projectDir, 'build')

        when:
        runGradle(projectDir, 'compileScala', 'compileTestScala')

        then:
        buildDir.eachFileRecurse {
            assert !it.name.endsWith('.semanticdb')
        }
    }

    @Requires({ gradleVersion() >= '8.0' })
    def 'scalafix should work with the configuration cache enabled on Scala 2.x'() {
        given:
        File projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ DisableSyntax ]')
        createSourceFile(projectDir, '''
object HelloWorld {
  val i: Int = 3
}
''', 'main')

        when:
        BuildResult firstRun = runGradle(projectDir, '--configuration-cache', 'scalafix')
        BuildResult secondRun = runGradle(projectDir, '--configuration-cache', 'scalafix')

        then:
        firstRun.output.contains('Configuration cache entry stored')
        !firstRun.output.contains('Configuration cache problems found')
        secondRun.output.contains('Reusing configuration cache')
        !secondRun.output.contains('Configuration cache problems found')
    }

    @Requires({ gradleVersion() >= '8.0' && isScalaVersionSupported(SCALA_3_VERSION) })
    def 'scalafix should work with the configuration cache enabled on Scala 3.x'() {
        given:
        File projectDir = createScalaProject('', SCALA_3_VERSION)
        createScalafixConfig(projectDir, 'rules = [ DisableSyntax ]')
        createSourceFile(projectDir, '''
object HelloWorld {
  val i: Int = 3
}
''', 'main')

        when:
        BuildResult firstRun = runGradle(projectDir, '--configuration-cache', 'scalafix')
        BuildResult secondRun = runGradle(projectDir, '--configuration-cache', 'scalafix')

        then:
        firstRun.output.contains('Configuration cache entry stored')
        !firstRun.output.contains('Configuration cache problems found')
        secondRun.output.contains('Reusing configuration cache')
        !secondRun.output.contains('Configuration cache problems found')
    }

    def 'compileScala should be restored from the build cache on consecutive scalafix runs'() {
        given:
        File projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ DisableSyntax ]')
        createSourceFile(projectDir, '''
object HelloWorld {
  val i: Int = 3
}
''', 'main')
        new File(projectDir, 'settings.gradle') << """
buildCache {
    local { directory = '${new File(projectDir, '.gradle/build-cache').absolutePath}' }
}
"""

        when:
        runGradle(projectDir, '--build-cache', 'scalafix')
        runGradle(projectDir, 'clean')
        BuildResult secondRun = runGradle(projectDir, '--build-cache', 'scalafix')

        then:
        secondRun.output.contains(':compileScala FROM-CACHE')
    }

    def 'checkScalafix and scalafix tasks should not fail when no rules are informed'() {
        given:
        File projectDir = createScalaProject()
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
        File projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ DisableSyntax ]')

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

    def 'scalafix should run semantic rewrite rules and fix input source files'() {
        given:
        File projectDir = createScalaProject()
        createScalafixConfig(projectDir, '''
rules = [ OrganizeImports, ExplicitResultTypes ]

OrganizeImports.groupedImports = Merge
OrganizeImports.removeUnused = false
''')
        File src = createSourceFile(projectDir, '''
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer

object HelloWorld {
  def foo = Map(1 -> "one")
}
''')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        src.getText() == '''
import scala.collection.mutable.{ArrayBuffer, Buffer}

object HelloWorld {
  def foo: Map[Int,String] = Map(1 -> "one")
}
'''
    }

    def 'checkScalafix should run semantic rewrite rule and fail the build without fixing input source files'() {
        given:
        File projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ ExplicitResultTypes ]')
        String originalSrcContent = '''
object HelloWorld {
  def foo = Map(1 -> "one")
}
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
        File projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }')
        createScalafixConfig(projectDir, 'rules = [ ExplicitResultTypes ]')
        createSourceFile(projectDir, 'object Foo')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains 'Task :scalafixMain FAILED'
        err.message.contains 'SemanticDB is required to run semantic rules such as ExplicitResultTypes.'
    }

    def 'checkScalafix should fail to run semantic rules if the SemanticDB compiler plugin is not configured'() {
        given:
        File projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }')
        createScalafixConfig(projectDir, 'rules = [ ExplicitResultTypes ]')
        createSourceFile(projectDir, 'object Foo')

        when:
        runGradle(projectDir, 'checkScalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains 'Task :checkScalafixMain FAILED'
        err.message.contains 'SemanticDB is required to run semantic rules such as ExplicitResultTypes.'
    }

    def 'scalafix should skip non-included source files'() {
        given:
        File projectDir = createScalaProject('scalafix { includes = ["**/animals/**"] }')
        createScalafixConfig(projectDir, 'rules = [ ProcedureSyntax ]')
        String dogSrcContent = 'object Dog { def fn {} }'
        String dogTestSrcContent = 'object DogTest { def fn {} }'
        String appleSrcContent = 'object Apple { def fn {} }'
        String appleTestSrcContent = 'object AppleTest { def fn {} }'
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
        File projectDir = createScalaProject('scalafix { includes = ["**/fruits/**"] }')
        createScalafixConfig(projectDir, 'rules = [ ProcedureSyntax ]')
        createSourceFile(projectDir, 'object Dog { def fn {} }', 'main', 'animals')
        createSourceFile(projectDir, 'object DogTest { def fn {} }', 'test', 'animals')
        createSourceFile(projectDir, 'object Apple', 'main', 'fruits')
        createSourceFile(projectDir, 'object AppleTest', 'test', 'fruits')

        when:
        BuildResult buildResult = runGradle(projectDir, 'checkScalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL' // rule violations in 'animals' are skipped
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
        File projectDir = createScalaProject('scalafix { excludes = ["**/animals/**"] }')
        createScalafixConfig(projectDir, 'rules = [ ProcedureSyntax ]')
        String originalSrcContent = 'object Dog { def fn {} }'
        File srcFile = createSourceFile(projectDir, originalSrcContent, 'main', 'animals')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        srcFile.getText() == originalSrcContent
    }

    def 'checkScalafix should skip excluded source files'() {
        given:
        File projectDir = createScalaProject('''
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
        File projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }')
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
        File projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }')
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

    def 'scalafix should run semantic rewrite rule and leave code without violations unchanged'() {
        given:
        File projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ ExplicitResultTypes ]')
        String originalSrcContent = '''
object HelloWorld {
  def f: Int = 1 * 1
}
'''
        File srcFile = createSourceFile(projectDir, originalSrcContent)

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix')

        then:
        buildResult.output.contains 'BUILD SUCCESSFUL'
        buildResult.output.contains '''
> Task :scalafixMain
Running Scalafix on 1 Scala source file(s)
'''
        srcFile.getText() == originalSrcContent
    }

    def 'checkScalafix should run syntactic linter rule and succeed if there are no violations'() {
        given:
        File projectDir = createScalaProject('scalafix { semanticdb { autoConfigure = false } }')
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
        File projectDir = createScalaProject()
        File src = createSourceFile(projectDir, '''
object Foo {
  def x = 1 * 1
  def proc { }
}
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-Pscalafix.rules=ProcedureSyntax', '--info')

        then:
        buildResult.output.contains '- That will run: [ProcedureSyntax]'
        buildResult.output.contains 'BUILD SUCCESSFUL'
        src.getText() == '''
object Foo {
  def x = 1 * 1
  def proc: Unit = { }
}
'''
    }

    def 'scalafix should run multiple rules informed via command line even when they are not defined in the config file'() {
        given:
        File projectDir = createScalaProject()
        File src = createSourceFile(projectDir, '''
object Foo {
  def x = 1 * 1
  def proc { }
}
''')

        when:
        BuildResult buildResult =
                runGradle(projectDir, 'scalafix', '-Pscalafix.rules=ExplicitResultTypes,ProcedureSyntax', '--info')

        then:
        buildResult.output.contains '- That will run: [ExplicitResultTypes, ProcedureSyntax]'
        buildResult.output.contains 'BUILD SUCCESSFUL'
        src.getText() == '''
object Foo {
  def x: Int = 1 * 1
  def proc: Unit = { }
}
'''
    }

    def 'scalafix should only run rule informed via command line when there are other rules defined in the config file'() {
        given:
        File projectDir = createScalaProject()
        createScalafixConfig(projectDir, '''
rules = [
  DisableSyntax
  ExplicitResultTypes
  ProcedureSyntax
]

DisableSyntax.noVars = true
''')
        File src = createSourceFile(projectDir, '''
object Foo {
  var foo = "foo"
  def bar = 1 * 1
  def proc { }
}
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-Pscalafix.rules=ProcedureSyntax', '--info')

        then:
        buildResult.output.contains '- That will run: [ProcedureSyntax]'
        buildResult.output.contains 'BUILD SUCCESSFUL'
        src.getText() == '''
object Foo {
  var foo = "foo"
  def bar = 1 * 1
  def proc: Unit = { }
}
'''
    }

    def 'scalafix should run rules defined in the config file if blank value is informed via command line'() {
        given:
        File projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ ExplicitResultTypes, ProcedureSyntax ]')
        File src = createSourceFile(projectDir, '''
object Foo {
  def bar = 1 * 1
  def proc { }
}
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '-Pscalafix.rules=', '--info')

        then:
        buildResult.output.contains '- That will run: [ExplicitResultTypes, ProcedureSyntax]'
        buildResult.output.contains 'BUILD SUCCESSFUL'
        src.getText() == '''
object Foo {
  def bar: Int = 1 * 1
  def proc: Unit = { }
}
'''
    }

    def 'scalafix should fail if invalid rule is informed via command line'() {
        given:
        File projectDir = createScalaProject()
        createSourceFile(projectDir, 'object Bar')

        when:
        runGradle(projectDir, 'scalafix', '-Pscalafix.rules=Foo')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains "scalafix.internal.interfaces.ScalafixMainArgsException: Unknown rule 'Foo'"
    }

    def 'scalafix should fail if invalid rule is informed via config file'() {
        given:
        File projectDir = createScalaProject()
        createScalafixConfig(projectDir, 'rules = [ Foo ]')
        createSourceFile(projectDir, 'object Bar')

        when:
        runGradle(projectDir, 'scalafix')

        then:
        UnexpectedBuildFailure err = thrown()
        err.message.contains "scalafix.internal.interfaces.ScalafixMainArgsException: Unknown rule 'Foo'"
    }

    @Unroll
    @Requires({ isScalaVersionSupported(data.scalaVersion) })
    def 'scalafix should run built-in/external semantic and syntactic rules on Scala 2.x projects - #scalaVersion'() {
        given:
        def scalaBinaryVersion = scalaVersion.substring(0, scalaVersion.lastIndexOf('.'))
        File projectDir = createScalaProject("""
dependencies {
    scalafix 'com.github.vovapolu:scaluzzi_${scalaBinaryVersion}:0.1.23'
}
""", scalaVersion)

        createScalafixConfig(projectDir, '''
rules = [
  OrganizeImports # built-in semantic
  RedundantSyntax # built-in syntactic
  MissingFinal    # external semantic
]
OrganizeImports.groupedImports = Merge
OrganizeImports.removeUnused = false
''')

        File redundantSyntaxSource = createSourceFile(projectDir, 'final object RedundantSyntaxTest')
        File missingFinalSource = createSourceFile(projectDir, 'case class MissingFinalTest(i: Int)')
        File organizeImportsSource = createSourceFile(projectDir, '''
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer
object OrganizeImportsTest
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix')

        then:
        buildResult.output.contains 'Running Scalafix on 3 Scala source file(s)'
        buildResult.output.contains 'BUILD SUCCESSFUL'
        redundantSyntaxSource.getText() == 'object RedundantSyntaxTest'
        missingFinalSource.getText() == 'final case class MissingFinalTest(i: Int)'
        organizeImportsSource.getText() == '''
import scala.collection.mutable.{ArrayBuffer, Buffer}
object OrganizeImportsTest
'''

        where:
        scalaVersion || _
        '2.12.18'    || _
        '2.12.19'    || _
        '2.12.20'    || _
        '2.13.15'    || _
        '2.13.16'    || _
        '2.13.17'    || _
        '2.13.18'    || _
    }

    @Unroll
    @Requires({ isScalaVersionSupported(data.scalaVersion) })
    def 'scalafix should run built-in/external semantic and syntactic rules on Scala 3.x projects - #scalaVersion'() {
        given:
        File projectDir = createScalaProject("""
dependencies {
    scalafix 'com.github.vovapolu:scaluzzi_2.13:0.1.23'
}
""", scalaVersion)

        createScalafixConfig(projectDir, '''
rules = [
  OrganizeImports # built-in semantic
  RedundantSyntax # built-in syntactic
  MissingFinal    # external semantic
]
OrganizeImports.groupedImports = Merge
OrganizeImports.removeUnused = false
''')

        File redundantSyntaxSource = createSourceFile(projectDir, '''
final object RedundantSyntaxTest:
  opaque type Logarithm = Double
''')
        File missingFinalSource = createSourceFile(projectDir, '''
case class MissingFinalTest(i: Int):
  if i < 0 then
    println("i < 0")
''')
        File organizeImportsSource = createSourceFile(projectDir, '''
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer

trait OrganizeImportsTest(val x: String):
  println(x)
''')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix')

        then:
        buildResult.output.contains 'Running Scalafix on 3 Scala source file(s)'
        buildResult.output.contains 'BUILD SUCCESSFUL'
        redundantSyntaxSource.getText() == '''
object RedundantSyntaxTest:
  opaque type Logarithm = Double
'''
        missingFinalSource.getText() == '''
final case class MissingFinalTest(i: Int):
  if i < 0 then
    println("i < 0")
'''
        organizeImportsSource.getText() == '''
import scala.collection.mutable.{ArrayBuffer, Buffer}

trait OrganizeImportsTest(val x: String):
  println(x)
'''

        where:
        scalaVersion || _
        '3.3.4'      || _
        '3.4.3'      || _
        '3.5.2'      || _
        '3.6.2'      || _
    }

    def 'scalafix should load local rules from a subproject'() {
        given:
        // root project
        def rootProject = Files.createTempDirectory('root').toFile()
        new File(rootProject, 'settings.gradle').write """
rootProject.name = 'root'
include 'scalafix-rules', 'scala-project'
"""
        new File(rootProject, 'build.gradle').write """
subprojects {
    apply plugin: 'scala'

    repositories { mavenCentral() }

    dependencies {
        implementation 'org.scala-lang:scala-library:${SCALA_2_VERSION}'
    }
}
"""
        // rules subproject
        def rulesSubProject = mkDir(rootProject, 'scalafix-rules')
        new File(rulesSubProject, 'build.gradle').write """
dependencies {
    compileOnly 'ch.epfl.scala:scalafix-core_2.13:0.11.1'
}
"""
        def ruleServicesDir = mkDir(rulesSubProject, 'src/main/resources/META-INF/services')
        new File(ruleServicesDir, 'scalafix.v1.Rule').write 'rules.FooDummyRule\n'
        def ruleSourcesDir = mkDir(rulesSubProject, 'src/main/scala/rules')
        new File(ruleSourcesDir, 'FooDummyRule.scala').write '''
package rules
import scalafix.v1._

class FooDummyRule extends SyntacticRule("FooDummyRule") {
  override def fix(implicit doc: SyntacticDocument): Patch = {
    println("##### Foo #####")
    Patch.empty
  }
}
'''
        // Scala subproject (where the rule is used)
        def scalaSubProject = mkDir(rootProject,  'scala-project')
        new File(scalaSubProject, 'build.gradle').write """
plugins {
    id 'io.github.cosmicsilence.scalafix'
}

dependencies {
    scalafix project(':scalafix-rules')
}
"""
        createScalafixConfig(scalaSubProject, 'rules = [ FooDummyRule ]')
        createSourceFile(scalaSubProject, 'object Foo')

        when:
        BuildResult buildResult = runGradle(rootProject, ':scala-project:scalafix', '--info')

        then:
        buildResult.output.contains 'Task :scalafix-rules:compileScala'
        buildResult.output.contains '- That will run: [FooDummyRule]'
        buildResult.output.contains '##### Foo #####'
        buildResult.output.contains 'BUILD SUCCESSFUL'
    }

    def 'scalafix should load local rules from a source set'() {
        given:
        File projectDir = createScalaProject("""
sourceSets {
     customScalafixRule { compileClasspath += sourceSets.main.compileClasspath }
}

dependencies {
    customScalafixRuleCompileOnly 'ch.epfl.scala:scalafix-core_2.13:0.11.1'
    scalafix sourceSets.customScalafixRule.output
}

scalafix { semanticdb { autoConfigure = false } }
""")
        def ruleServicesDir = mkDir(projectDir, 'src/customScalafixRule/resources/META-INF/services')
        new File(ruleServicesDir, 'scalafix.v1.Rule').write 'rules.BarDummyRule\n'
        def ruleSourcesDir = mkDir(projectDir, 'src/customScalafixRule/scala/rules')
        new File(ruleSourcesDir, 'BarDummyRule.scala').write '''
package rules
import scalafix.v1._

class BarDummyRule extends SyntacticRule("BarDummyRule") {
  override def fix(implicit doc: SyntacticDocument): Patch = {
    println("***** Bar *****")
    Patch.empty
  }
}
'''
        createScalafixConfig(projectDir, 'rules = [ BarDummyRule ]')
        createSourceFile(projectDir, 'object Bar')

        when:
        BuildResult buildResult = runGradle(projectDir, 'scalafix', '--info')

        then:
        buildResult.output.contains 'Task :compileCustomScalafixRuleScala'
        buildResult.output.contains '- That will run: [BarDummyRule]'
        buildResult.output.contains '***** Bar *****'
        buildResult.output.contains 'BUILD SUCCESSFUL'
    }

    private static BuildResult runGradle(File projectDir, String... arguments) {
        return GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(arguments.toList() + '--stacktrace')
                .withGradleVersion(gradleVersion())
                .withPluginClasspath()
                .forwardOutput()
                .build()
    }

    private static String gradleVersion() {
        return System.getProperty('compat.gradle.version')
    }

    private static boolean isScalaVersionSupported(String scalaVersion) {
        return scalaVersion.startsWith("2.12")
            // https://github.com/cosmicsilence/gradle-scalafix/pull/85#issuecomment-2588144036
            || scalaVersion.startsWith("2.13") && gradleVersion() >= '6.0'
            // https://docs.gradle.org/7.3/release-notes.html
            || scalaVersion.startsWith("3") && gradleVersion() >= '7.3'
    }

    private static File createScalaProject(String additionalBuildInstructions = '', String scalaVersion = SCALA_2_VERSION) {
        def projectDir = Files.createTempDirectory('test').toFile()
        def isScala3 = scalaVersion.startsWith('3.')

        new File(projectDir, 'build.gradle').write """
plugins {
    id 'scala'
    id 'io.github.cosmicsilence.scalafix'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.scala-lang:${isScala3 ? 'scala3-library_3' : 'scala-library' }:${scalaVersion}'
}

$additionalBuildInstructions
"""
        new File(projectDir, 'settings.gradle').write "rootProject.name = 'hello-world'"
        return projectDir
    }

    private static File createScalafixConfig(File projectDir, String content) {
        def file = new File(projectDir, '.scalafix.conf')
        file.write content
        return file
    }

    private static File createSourceFile(File projectDir, String content, String sourceSet = 'main', String pkgName = 'dummy') {
        def pkgFolder = mkDir(projectDir, "src/$sourceSet/scala/$pkgName")
        def srcFile = new File(pkgFolder, "source_${fileCount.incrementAndGet()}.scala")
        srcFile.write content
        return srcFile
    }

    private static File mkDir(File projectDir, String path) {
        def dir = new File(projectDir, path)
        dir.mkdirs()
        return dir
    }

    private static final AtomicInteger fileCount = new AtomicInteger()
}
