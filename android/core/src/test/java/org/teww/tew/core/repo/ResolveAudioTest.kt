package org.teww.tew.core.repo

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The API returns `/v1/audio/<key>` rather than an absolute URL, because
 * behind a reverse proxy the service does not reliably know its own public
 * hostname. The client resolves it. If this is wrong, every memo fails to play
 * and the app looks broken rather than misconfigured.
 */
class ResolveAudioTest {

    @Test
    fun `a path is joined onto the base URL`() {
        assertEquals(
            "https://tew.example.org/v1/audio/abc.m4a",
            resolveAudio("https://tew.example.org/", "/v1/audio/abc.m4a"),
        )
    }

    @Test
    fun `a missing or doubled slash does not produce a broken URL`() {
        // Base URLs are typed by a person and stored; both shapes occur.
        assertEquals(
            "https://tew.example.org/v1/audio/abc.m4a",
            resolveAudio("https://tew.example.org", "v1/audio/abc.m4a"),
        )
        assertEquals(
            "https://tew.example.org/v1/audio/abc.m4a",
            resolveAudio("https://tew.example.org/", "v1/audio/abc.m4a"),
        )
    }

    @Test
    fun `an absolute URL is left alone`() {
        // If the backend ever starts returning signed absolute URLs — for
        // object storage, say — the client must not mangle them.
        val absolute = "https://cdn.example.org/audio/abc.m4a?sig=xyz"
        assertEquals(absolute, resolveAudio("https://tew.example.org/", absolute))
    }

    @Test
    fun `a locally recorded file URI is left alone`() {
        // The composer plays back a file the app just wrote, before it is
        // posted anywhere.
        val local = "file:///data/user/0/org.teww.tew/cache/memo-1.m4a"
        assertEquals(local, resolveAudio("https://tew.example.org/", local))
    }
}
