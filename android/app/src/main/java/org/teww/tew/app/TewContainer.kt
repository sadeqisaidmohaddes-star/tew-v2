package org.teww.tew.app

import android.content.Context
import org.teww.tew.core.auth.AuthSession
import org.teww.tew.core.auth.StubAuthSession
import org.teww.tew.core.playback.FeedCommandBus
import org.teww.tew.core.playback.Media3PlaybackSession
import org.teww.tew.core.playback.PlaybackSession
import org.teww.tew.core.record.MemoRecorder
import org.teww.tew.core.voice.VoiceCommandListener
import kotlinx.coroutines.runBlocking
import org.teww.tew.core.net.TewApiConfig
import org.teww.tew.core.net.TewApiFactory
import org.teww.tew.core.repo.FeedRepository
import org.teww.tew.core.repo.InMemoryFeedRepository
import org.teww.tew.core.repo.InMemoryModerationRepository
import org.teww.tew.core.repo.ModerationRepository
import org.teww.tew.core.repo.NetworkFeedRepository
import org.teww.tew.core.repo.NetworkModerationRepository

/**
 * Dependency wiring, by hand.
 *
 * No DI framework. At this size one would add a compiler plugin, a build-time
 * cost on every module, and a layer of indirection for four objects — and
 * IMPLEMENTATION.md's target is a three-year-old budget phone, where the app's
 * own startup work is not the place to be casual. If the graph grows past what
 * this file can hold clearly, that is the signal to reach for a framework, not
 * before.
 *
 * ## Server or sample data
 *
 * If a server address has been set (Moderator controls → Server), the app
 * talks to it: real API client, real playback over HTTP, real posting. If not,
 * it runs on built-in sample data so the app is still usable on a phone with
 * nothing reachable — which is what makes an accessibility test possible
 * before any backend exists.
 *
 * [authSession] is still [StubAuthSession] either way. Firebase is deferred in
 * `STATE.md`, and the stub presents a token the backend's own stub verifier
 * accepts. **It signs anyone in and must not reach a public build.**
 */
class TewContainer(context: Context) {

    private val appContext = context.applicationContext

    val settings = TewSettings(appContext)

    val authSession: AuthSession = StubAuthSession()

    /**
     * Whether this session is talking to a real server.
     *
     * Read once at construction. Changing the address recreates the activity,
     * which rebuilds this container — a repository swapped underneath a
     * running feed would leave a memo playing from a server the app is no
     * longer signed in to.
     */
    val usingServer: Boolean = settings.usingServer

    private val api = if (usingServer) {
        TewApiFactory.create(
            TewApiConfig(baseUrl = settings.serverUrl, logRequests = true),
            authSession,
        )
    } else {
        null
    }

    val feedRepository: FeedRepository =
        api?.let { NetworkFeedRepository(it, settings.serverUrl) } ?: InMemoryFeedRepository()

    val moderationRepository: ModerationRepository =
        api?.let { NetworkModerationRepository(it, settings.serverUrl) }
            ?: InMemoryModerationRepository()

    /**
     * Where media buttons and voice deliver commands. One bus for the app: the
     * feed screen that is currently composed claims it, so a headset button
     * always reaches whichever feed the moderator toggle has on screen.
     */
    val commandBus = FeedCommandBus()

    val voiceCommandListener = VoiceCommandListener(context)

    /**
     * One playback session for the whole app, not one per screen. Two players
     * could otherwise talk over each other when the moderator toggle switches
     * feeds mid-session, which in an audio-only app is not a cosmetic bug.
     *
     * Passing [commandBus] publishes a `MediaSession`, which is what makes
     * headset and lock-screen transport buttons work — the media-controls leg
     * of non-negotiable #5.
     */
    val playbackSession: PlaybackSession = Media3PlaybackSession(
        context = context,
        commandBus = commandBus,
        // Memo audio is behind the same auth as everything else, so the player
        // needs the token. Only when there is a server: locally-recorded files
        // are read from disk and need no header.
        authHeader = if (usingServer) {
            { runBlocking { authSession.bearerToken() } }
        } else {
            null
        },
    )

    /**
     * A new recorder per composer, not a shared one. MediaRecorder is a
     * single-use state machine — reusing one across screens is how a
     * half-released recorder turns into a screen that silently fails to
     * record, which is this app's worst failure mode.
     */
    fun newRecorder(): MemoRecorder = MemoRecorder(appContext)

    fun release() {
        playbackSession.release()
        voiceCommandListener.release()
    }
}
