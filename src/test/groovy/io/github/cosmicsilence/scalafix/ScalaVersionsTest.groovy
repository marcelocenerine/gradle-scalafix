package io.github.cosmicsilence.scalafix

import spock.lang.Specification
import spock.lang.Unroll

class ScalaVersionsTest extends Specification {

    @Unroll
    def 'isScala3 should return #expected for #version'() {
        expect:
        ScalaVersions.isScala3(version) == expected

        where:
        version   || expected
        '3.0.0'   || true
        '3.3.1'   || true
        '3.5.2'   || true
        '2.13.15' || false
        '2.12.18' || false
    }

    @Unroll
    def 'isScala212 should return #expected for #version'() {
        expect:
        ScalaVersions.isScala212(version) == expected

        where:
        version   || expected
        '2.12.0'  || true
        '2.12.18' || true
        '2.12.21' || true
        '2.13.15' || false
        '3.3.1'   || false
    }

    @Unroll
    def 'isScala213 should return #expected for #version'() {
        expect:
        ScalaVersions.isScala213(version) == expected

        where:
        version   || expected
        '2.13.0'  || true
        '2.13.12' || true
        '2.13.15' || true
        '2.12.18' || false
        '3.3.1'   || false
    }
}
