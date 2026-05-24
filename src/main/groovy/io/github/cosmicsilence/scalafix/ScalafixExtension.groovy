package io.github.cosmicsilence.scalafix

import io.github.cosmicsilence.compat.GradleCompat
import org.gradle.api.Action
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

import javax.inject.Inject

class ScalafixExtension {

    /**
     * Scalafix configuration file. If not specified, the plugin will try to find
     * a file named '.scalafix.conf' in the project's directory and then in the
     * root project's directory, in this order.
     */
    final RegularFileProperty configFile

    /**
     * Adds ANT style include patterns. If includes are not provided, then all files
     * in the Scala source set will be included. If includes are provided, then a
     * file must match at least one of the include patterns to be processed.
     */
    final SetProperty<String> includes

    /**
     * Adds ANT style exclude pattern. If excludes are not provided, then no files
     * will be excluded. If excludes are provided, then files must not match any
     * exclude pattern to be processed.
     */
    final SetProperty<String> excludes

    /**
     * Name of source sets to which the Scalafix plugin should not be applied.
     */
    final SetProperty<String> ignoreSourceSets

    /**
     * Semanticdb settings.
     */
    private final SemanticdbParameters semanticdb

    private final ProjectLayout projectLayout

    @Inject
    ScalafixExtension(ObjectFactory objects, ProjectLayout layout) {
        this.projectLayout = layout
        configFile = GradleCompat.fileProperty(objects, layout)
        includes = objects.setProperty(String)
        excludes = objects.setProperty(String)
        ignoreSourceSets = objects.setProperty(String)
        semanticdb = objects.newInstance(SemanticdbParameters)
    }

    /**
     * Path to the Scalafix configuration file. This path must be relative to the
     * project's directory. If not specified, the plugin will try to find a file
     * named '.scalafix.conf' in the project's directory and then in the root
     * project's directory, in this order.
     */
    void setConfigFile(String path) {
        configFile.set(projectLayout.projectDirectory.file(path))
    }

    @Nested
    @Optional
    SemanticdbParameters getSemanticdb() {
        return semanticdb
    }

    void semanticdb(Action<? super SemanticdbParameters> action) {
        action.execute(semanticdb)
    }
}
