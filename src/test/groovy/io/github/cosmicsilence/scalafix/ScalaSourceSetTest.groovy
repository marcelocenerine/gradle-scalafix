package io.github.cosmicsilence.scalafix

import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Unroll

class ScalaSourceSetTest extends Specification {

    private static final String SCALA_2_VERSION = '2.13.12'
    private static final String SCALA_3_VERSION = '3.3.1'

    @Unroll
    def 'it should consider #sourceSetName a valid Scala #scalaVersion source set'() {
        given:
        Project project = buildScalaProject(scalaVersion)
        SourceSet sourceSet = project.sourceSets.getByName(sourceSetName)

        when:
        def valid = ScalaSourceSet.isScalaSourceSet(project, sourceSet)

        then:
        valid

        where:
        scalaVersion    || sourceSetName
        SCALA_2_VERSION || 'main'
        SCALA_2_VERSION || 'test'
        SCALA_3_VERSION || 'main'
        SCALA_3_VERSION || 'test'
    }

    def 'it should not be considered as a valid Scala source set'() {
        given:
        Project project = buildJavaProject()
        SourceSet sourceSet = project.sourceSets.main

        when:
        def valid = ScalaSourceSet.isScalaSourceSet(project, sourceSet)

        then:
        !valid
    }

    def 'it should fail to instantiate if a non-Scala source set is passed in'() {
        given:
        Project project = buildJavaProject()
        SourceSet sourceSet = project.sourceSets.main

        when:
        new ScalaSourceSet(project, sourceSet)

        then:
        thrown IllegalArgumentException
    }

    def 'it should instantiate for a valid Scala 2.x source set'() {
        given:
        Project project = buildScalaProject(SCALA_2_VERSION, ['-Xfoo', '-Ybar'])
        SourceSet mainSourceSet = project.sourceSets.main

        when:
        def sourceSet = new ScalaSourceSet(project, mainSourceSet)

        then:
        sourceSet.name == 'main'
        sourceSet.fullClasspath.find { it.absolutePath == "${project.projectDir}/build/classes/java/main" }
        sourceSet.fullClasspath.find { it.absolutePath == "${project.projectDir}/build/classes/scala/main" }
        sourceSet.fullClasspath.find { it.name == "scala-library-${SCALA_2_VERSION}.jar" }
        !sourceSet.fullClasspath.find { it.name.endsWith('.pom') }
        sourceSet.scalaSources.asPath == new File(project.projectDir, 'src/main/scala/Foo.scala').absolutePath
        sourceSet.compileTask == project.tasks.compileScala
        sourceSet.compilerOptions == ['-Xfoo', '-Ybar']
        sourceSet.scalaVersion.get() == SCALA_2_VERSION
    }

    def 'it should instantiate for a valid Scala 3.x source set'() {
        given:
        Project project = buildScalaProject(SCALA_3_VERSION, ['-Xfoo', '-Ybar'])
        SourceSet testSourceSet = project.sourceSets.test

        when:
        def sourceSet = new ScalaSourceSet(project, testSourceSet)

        then:
        sourceSet.name == 'test'
        sourceSet.fullClasspath.find { it.absolutePath == "${project.projectDir}/build/classes/java/test" }
        sourceSet.fullClasspath.find { it.absolutePath == "${project.projectDir}/build/classes/scala/test" }
        sourceSet.fullClasspath.find { it.name == "scala3-library_3-${SCALA_3_VERSION}.jar" }
        !sourceSet.fullClasspath.find { it.name.endsWith('.pom') }
        sourceSet.scalaSources.asPath == new File(project.projectDir, 'src/test/scala/FooTest.scala').absolutePath
        sourceSet.compileTask == project.tasks.compileTestScala
        sourceSet.compilerOptions == ['-Xfoo', '-Ybar']
        sourceSet.scalaVersion.get() == SCALA_3_VERSION
    }

    private Project buildScalaProject(String scalaVersion, List<String> compilerOpts = []) {
        def project = ProjectBuilder.builder().build()
        def scalaArtifactId = scalaVersion.startsWith('3.') ? 'scala3-library_3' : 'scala-library'

        project.with {
            apply plugin: 'scala'

            repositories { mavenCentral() }

            dependencies {
                implementation "org.scala-lang:${scalaArtifactId}:${scalaVersion}"
                implementation group: 'org.scala-lang', name: scalaArtifactId, version: scalaVersion, ext: 'pom'
                scalaCompilerPlugins "org.wartremover:wartremover_${scalaVersion}:3.1.5"
            }

            tasks.withType(ScalaCompile) {
                scalaCompileOptions.additionalParameters = compilerOpts
            }
        }

        createSrcFile(project, 'src/main/scala/Foo.scala', 'object Foo')
        createSrcFile(project, 'src/test/scala/FooTest.scala', 'object FooTest')
        return project
    }

    private Project buildJavaProject() {
        def project = ProjectBuilder.builder().build()
        project.with {
            apply plugin: 'java'
        }
        return project
    }

    private void createSrcFile(Project project, String path, String content) {
        File srcFile = new File(project.projectDir, path)
        srcFile.parentFile.mkdirs()
        srcFile.write content
    }
}
