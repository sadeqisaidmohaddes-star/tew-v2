package org.teww.tew.core.playback

import kotlinx.coroutines.flow.StateFlow

/**
 * What is playing right now.
 *
 * [error] carries a spoken sentence rather than a code, for the same reason
 * [org.teww.tew.core.TewResult.Failure] does: a playback failure the user
 * cannot hear is indistinguishable from the app being broken.
 */
data class PlaybackState(
    val memoId: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val error: String? = null,
) {
    val isIdle: Boolean get() = memoId == null
}

/**
 * Audio playback, owned by `:core`.
 *
 * Both feed models play the same memos the same way, so neither `:feature-radio`
 * nor `:feature-carddeck` touches Media3 directly — IMPLEMENTATION.md calls
 * that a boundary violation, and it would also let the two screens drift into
 * behaving differently, which would quietly invalidate the usability
 * comparison they exist to support.
 */
interface PlaybackSession {

    val state: StateFlow<PlaybackState>

    /** Start [audioUrl], replacing whatever was playing. */
    fun play(memoId: String, audioUrl: String)

    fun pause()

    fun resume()

    fun stop()

    /**
     * Called when the owner is done — releases the underlying player. The
     * session is unusable afterwards.
     */
    fun release()

    /** Set a listener invoked when a memo finishes on its own. */
    fun onCompletion(listener: (memoId: String) -> Unit)
}
