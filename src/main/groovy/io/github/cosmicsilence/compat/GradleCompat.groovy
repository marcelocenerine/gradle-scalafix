package io.github.cosmicsilence.compat

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.util.GradleVersion

abstract class GradleCompat {

    private static final boolean IS_VERSION_4 = GradleVersion.current().version.startsWith("4.")
    private static final boolean SUPPORTS_OBJECTS_FILE_COLLECTION =
            GradleVersion.current().compareTo(GradleVersion.version("5.3")) >= 0

    private GradleCompat() {}

    static ConfigurableFileCollection fileCollection(ObjectFactory objects, ProjectLayout layout) {
        // ObjectFactory.fileCollection() was added in Gradle 5.3; on older versions use ProjectLayout.configurableFiles().
        return SUPPORTS_OBJECTS_FILE_COLLECTION ? objects.fileCollection() : layout.configurableFiles()
    }

    static RegularFileProperty fileProperty(ObjectFactory objects, ProjectLayout layout, RegularFile defaultFile = null) {
        // ObjectFactory.fileProperty() was added in Gradle 5.0; on 4.x, fall back to ProjectLayout.fileProperty().
        def fileProp = IS_VERSION_4 ? layout.fileProperty() : objects.fileProperty()

        if (defaultFile != null) {
            if (IS_VERSION_4) {
                fileProp.set(defaultFile)
            } else {
                fileProp.convention(defaultFile)
            }
        }

        return fileProp
    }

    static Property<Boolean> booleanProperty(ObjectFactory objects, Boolean defaultBoolean = null) {
        def prop = objects.property(Boolean)

        if (defaultBoolean != null) {
            if (IS_VERSION_4) {
                prop.set(defaultBoolean)
            } else {
                prop.convention(defaultBoolean)
            }
        }

        return prop
    }
}
