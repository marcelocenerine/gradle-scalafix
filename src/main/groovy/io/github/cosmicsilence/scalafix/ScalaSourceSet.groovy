package io.github.cosmicsilence.scalafix

import groovy.transform.Memoized
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.ScalaRuntime
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.scala.ScalaCompile

class ScalaSourceSet {

    private final Project project
    private final SourceSet sourceSet
    private final ScalaCompile compileTask

    ScalaSourceSet(Project project, SourceSet sourceSet) {
        if (!isScalaSourceSet(project, sourceSet)) {
            throw new IllegalArgumentException(sourceSet.name + "is not a Scala source set")
        }

        this.project = project
        this.sourceSet = sourceSet
        this.compileTask = project.tasks.getByName(getCompileTaskName(sourceSet))
    }

    String getName() {
        return sourceSet.name
    }

    List<File> getFullClasspath() {
        return getClassesDirs() + getJarDependencies()
    }

    private List<File> getClassesDirs() {
        return sourceSet.output.classesDirs.toList()
    }

    private List<File> getJarDependencies() {
        return sourceSet.compileClasspath.toList().findAll { it.name.toLowerCase().endsWith(".jar") }
    }

    SourceDirectorySet getScalaSources() {
        if (sourceSet.hasProperty('allScala')) {
            return sourceSet.allScala // removed in Gradle 9+ (https://github.com/gradle/gradle/pull/33153)
        }

        return sourceSet.getExtensions().findByName('scala') as SourceDirectorySet
    }

    ScalaCompile getCompileTask() {
        return compileTask
    }

    @Memoized
    Optional<String> getScalaVersion() {
        def scalaRuntime = project.extensions.findByType(ScalaRuntime)
        def scala3Jar = scalaRuntime?.findScalaJar(compileTask.classpath, 'library_3')
        if (scala3Jar) return Optional.of(scalaRuntime.getScalaVersion(scala3Jar))

        def scala2Jar = scalaRuntime?.findScalaJar(compileTask.classpath, 'library')
        return Optional.ofNullable(scala2Jar ? scalaRuntime.getScalaVersion(scala2Jar) : null)
    }

    List<String> getCompilerOptions() {
        return compileTask.scalaCompileOptions.additionalParameters ?: []
    }

    static boolean isScalaSourceSet(Project project, SourceSet sourceSet) {
        def taskName = getCompileTaskName(sourceSet)
        return taskName ? project.tasks.findByName(taskName) : false
    }

    private static String getCompileTaskName(SourceSet sourceSet) {
        return sourceSet.getCompileTaskName('scala')
    }
}
