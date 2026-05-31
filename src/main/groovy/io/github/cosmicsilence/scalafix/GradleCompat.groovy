package io.github.cosmicsilence.scalafix

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.util.GradleVersion

abstract class GradleCompat {

    private static final GradleVersion CURRENT = GradleVersion.current()
    private static final boolean SUPPORTS_OBJECTS_FILE_PROPERTY = CURRENT >= GradleVersion.version("5.0")
    private static final boolean SUPPORTS_PROPERTY_CONVENTION = CURRENT >= GradleVersion.version("5.1")
    private static final boolean SUPPORTS_OBJECTS_FILE_COLLECTION = CURRENT >= GradleVersion.version("5.3")
    private static final boolean SUPPORTS_GRADLE_PROPERTY_PROVIDER = CURRENT >= GradleVersion.version("6.2")
    // Provider.map() added in 5.0; Provider.orElse(T) added in 5.6
    private static final boolean SUPPORTS_PROVIDER_MAP_AND_ORELSE = CURRENT >= GradleVersion.version("5.6")

    private GradleCompat() {}

    static ConfigurableFileCollection fileCollection(ObjectFactory objects, ProjectLayout layout) {
        return SUPPORTS_OBJECTS_FILE_COLLECTION ? objects.fileCollection() : layout.configurableFiles()
    }

    static RegularFileProperty fileProperty(ObjectFactory objects, ProjectLayout layout, RegularFile defaultFile = null) {
        def prop = SUPPORTS_OBJECTS_FILE_PROPERTY ? objects.fileProperty() : layout.fileProperty()
        return setConvention(prop, defaultFile)
    }

    static Property<Boolean> booleanProperty(ObjectFactory objects, Boolean defaultBoolean = null) {
        def prop = objects.property(Boolean)
        return setConvention(prop, defaultBoolean)
    }

    static Provider<String> gradleProperty(Project project, String name) {
        if (SUPPORTS_GRADLE_PROPERTY_PROVIDER) {
            return project.providers.gradleProperty(name)
        }
        return project.provider { project.findProperty(name)?.toString() }
    }

    static Provider<List<String>> gradlePropertyAsList(Project project, String name) {
        if (SUPPORTS_PROVIDER_MAP_AND_ORELSE) {
            return gradleProperty(project, name)
                    .map { String prop -> GradleCompat.splitCommaSeparated(prop) }
                    .orElse([])
        }

        def items = splitCommaSeparated(project.findProperty(name)?.toString() ?: '')
        return project.provider { items }
    }

    static List<String> splitCommaSeparated(String value) {
        value.split(/\s*,\s*/).findAll { it }.toList()
    }

    static <T> Property<T> setConvention(Property<T> prop, T value) {
        if (value == null) return prop

        if (SUPPORTS_PROPERTY_CONVENTION) {
            prop.convention(value)
        } else {
            prop.set(value)
        }

        return prop
    }
}
