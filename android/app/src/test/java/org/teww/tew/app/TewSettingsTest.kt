package org.teww.tew.app

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The address someone types on a phone, turned into something Retrofit
 * accepts.
 *
 * Worth testing rather than trusting: Retrofit throws at construction on a
 * base URL without a trailing slash, so a missing character here is a crash on
 * startup — a poor way to tell a tester they mistyped a hostname.
 */
class TewSettingsTest {

    @Test
    fun `a trailing slash is added because Retrofit requires one`() {
        assertEquals("https://tew.example.org/", normaliseServerUrl("https://tew.example.org"))
        assertEquals("https://tew.example.org/", normaliseServerUrl("https://tew.example.org/"))
    }

    @Test
    fun `a bare host defaults to https, not http`() {
        // These are voice recordings. Guessing plaintext because the user
        // omitted a scheme would be the wrong default to pick for them.
        assertEquals("https://tew.example.org/", normaliseServerUrl("tew.example.org"))
    }

    @Test
    fun `an explicit http address is respected`() {
        // Local testing against a machine on the same network is a real case,
        // and silently upgrading it would leave the tester with an app that
        // cannot connect and no explanation.
        assertEquals("http://192.168.1.10:8080/", normaliseServerUrl("http://192.168.1.10:8080"))
    }

    @Test
    fun `whitespace is forgiven`() {
        // Typed on a phone, possibly with a screen reader, possibly pasted.
        assertEquals("https://tew.example.org/", normaliseServerUrl("  tew.example.org  "))
    }

    @Test
    fun `blank means sample data, not a broken URL`() {
        assertEquals("", normaliseServerUrl(""))
        assertEquals("", normaliseServerUrl("   "))
    }
}
