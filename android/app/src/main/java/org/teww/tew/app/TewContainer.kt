package org.teww.tew.app

import android.content.Context
import org.teww.tew.core.auth.AuthSession
import org.teww.tew.core.auth.StubAuthSession
import org.teww.tew.core.playback.Media3PlaybackSession
import org.teww.tew.core.playback.PlaybackSession
import org.teww.tew.core.repo.FeedRepository
import org.teww.tew.core.repo.InMemoryFeedRepository
import org.teww.tew.core.repo.InMemoryModerationRepository
import org.teww.tew.core.repo.ModerationRepository

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
 * ## What is deliberately fake here
 *
 * [authSession] is [StubAuthSession] and the repositories are the in-memory
 * ones. Both are stand-ins with no backend behind them, and this file is the
 * single place either gets swapped:
 *
 * - Firebase Auth is deferred in `STATE.md` — no account chosen yet.
 * - `backend/README.md` is design-stage; the API does not exist.
 *
 * Swapping in the real ones is two lines here and no change anywhere else.
 * That is the point of the interfaces in `:core`.
 */
class TewContainer(context: Context) {

    val authSession: AuthSession = StubAuthSession()

    val feedRepository: FeedRepository = InMemoryFeedRepository()

    val moderationRepository: ModerationRepository = InMemoryModerationRepository()

    /**
     * One playback session for the whole app, not one per screen. Two players
     * could otherwise talk over each other when the moderator toggle switches
     * feeds mid-session, which in an audio-only app is not a cosmetic bug.
     */
    val playbackSession: PlaybackSession = Media3PlaybackSession(context)

    fun release() {
        playbackSession.release()
    }
}
