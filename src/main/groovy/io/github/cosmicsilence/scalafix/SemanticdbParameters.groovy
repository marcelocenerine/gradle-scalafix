package io.github.cosmicsilence.scalafix

import io.github.cosmicsilence.compat.GradleCompat
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

import javax.inject.Inject


class SemanticdbParameters {

    /**
     * Auto configures the SemanticDB Scala compiler. This is required to run
     * semantic rules.
     */
    final Property<Boolean> autoConfigure

    /**
     * Used to override the version of the SemanticDB compiler plugin. By default
     * this plugin uses a version that is guaranteed to be compatible with Scalafix.
     */
    final Property<String> version

    @Inject
    SemanticdbParameters(ObjectFactory objects) {
        autoConfigure = GradleCompat.booleanProperty(objects, true)
        version = objects.property(String)
    }
}
