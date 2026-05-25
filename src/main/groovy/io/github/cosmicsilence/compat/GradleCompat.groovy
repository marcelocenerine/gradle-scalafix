package io.github.cosmicsilence.compat

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.util.GradleVersion

abstract class GradleCompat {

    private static final GradleVersion CURRENT = GradleVersion.current()
    private static final boolean SUPPORTS_OBJECTS_FILE_PROPERTY = CURRENT >= GradleVersion.version("5.0")
    private static final boolean SUPPORTS_PROPERTY_CONVENTION = CURRENT >= GradleVersion.version("5.1")
    private static final boolean SUPPORTS_OBJECTS_FILE_COLLECTION = CURRENT >= GradleVersion.version("5.3")

    private GradleCompat() {}

    static ConfigurableFileCollection fileCollection(ObjectFactory objects, ProjectLayout layout) {
        return SUPPORTS_OBJECTS_FILE_COLLECTION ? objects.fileCollection() : layout.configurableFiles()
    }

    static RegularFileProperty fileProperty(ObjectFactory objects, ProjectLayout layout, RegularFile defaultFile = null) {
        def fileProp = SUPPORTS_OBJECTS_FILE_PROPERTY ? objects.fileProperty() : layout.fileProperty()

        if (defaultFile != null) {
            if (SUPPORTS_PROPERTY_CONVENTION) {
                fileProp.convention(defaultFile)
            } else {
                fileProp.set(defaultFile)
            }
        }

        return fileProp
    }

    static Property<Boolean> booleanProperty(ObjectFactory objects, Boolean defaultBoolean = null) {
        def prop = objects.property(Boolean)

        if (defaultBoolean != null) {
            if (SUPPORTS_PROPERTY_CONVENTION) {
                prop.convention(defaultBoolean)
            } else {
                prop.set(defaultBoolean)
            }
        }

        return prop
    }
}
