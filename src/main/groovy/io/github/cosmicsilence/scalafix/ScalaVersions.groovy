package io.github.cosmicsilence.scalafix

abstract class ScalaVersions {

    private ScalaVersions() {}

    static boolean isScala212(String version) { version ==~ /^2\.12\..+$/ }
    static boolean isScala213(String version) { version ==~ /^2\.13\..+$/ }
    static boolean isScala3(String version) { version ==~ /^3\..+$/ }
}
