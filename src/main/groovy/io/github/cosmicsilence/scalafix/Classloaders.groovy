package io.github.cosmicsilence.scalafix

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import scalafix.interfaces.Scalafix
import scalafix.internal.interfaces.ScalafixInterfacesClassloader

import java.util.concurrent.ConcurrentHashMap

abstract class Classloaders {

    private static final Logger logger = Logging.getLogger(Classloaders)
    private static final ConcurrentHashMap<String, ClassLoader> cache = new ConcurrentHashMap<>()

    private Classloaders() {}

    static ClassLoader forScalafixCli(Set<File> cliJars, String cacheKey) {
        return cache.computeIfAbsent(cacheKey, {
            logger.debug("Creating classloader for '${cacheKey}'")
            def parentClassloader = new ScalafixInterfacesClassloader(Scalafix.class.classLoader)
            classloaderFrom(cliJars, parentClassloader)
        })
    }

    static URLClassLoader forExternalRules(Set<File> rulesJars, ClassLoader scalafixCliClassloader) {
        // No cache in this case as rules can be loaded from a subproject or source set under the same project.
        // There is no guarantee that rules would not be modified between executions.
        return classloaderFrom(rulesJars, scalafixCliClassloader)
    }

    private static URLClassLoader classloaderFrom(Set<File> jars, ClassLoader parent) {
        def jarUrls = jars.collect { it.toURI().toURL() }.toArray(new URL[0])
        return new URLClassLoader(jarUrls, parent)
    }
}
