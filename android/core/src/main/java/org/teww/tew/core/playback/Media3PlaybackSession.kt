package org.teww.tew.core.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [PlaybackSession] backed by Media3's ExoPlayer.
 *
 * Two details are deliberate rather than incidental:
 *
 * - **Audio focus is requested and handled.** TalkBack is speech, and a memo
 *   playing over the screen reader makes both unintelligible. Letting Media3
 *   manage focus means announcements duck the memo instead of colliding with
 *   it, which matters more here than in an ordinary media app.
 * - **Position updates are polled, not pushed.** ExoPlayer has no continuous
 *   position callback, and a progress ticker is what lets a screen say how far
 *   through a memo it is. Polling stops whenever nothing is playing so it
 *   costs nothing on a budget device at idle.
 */
@OptIn(UnstableApi::class)
class Media3PlaybackSession(
    context: Context,
    /**
     * When supplied, a [MediaSession] is published so headset and lock-screen
     * transport buttons reach the feed — the "media controls" leg of
     * non-negotiable #5. Null keeps playback private to the app, which is what
     * tests and the spike want.
     */
    private val commandBus: FeedCommandBus? = null,
    /**
     * Supplies the Authorization header for memo audio.
     *
     * The audio endpoint is authenticated — these are recordings of
     * identifiable people, and an open one would make every memo downloadable
     * by anyone who guessed a key. ExoPlayer fetches the audio itself, so it
     * needs the token; without this every memo would 401 and the app would
     * look broken rather than protected.
     *
     * Null for local playback of a file the app just recorded, which needs no
     * header.
     */
    private val authHeader: (() -> String?)? = null,
) : PlaybackSession {

    private val appContext = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var mediaSession: MediaSession? = null

    private val _state = MutableStateFlow(PlaybackState())
    override val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var completionListener: ((String) -> Unit)? = null
    private var released = false

    private val player: ExoPlayer by lazy {
        ExoPlayer.Builder(appContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .let { builder ->
                val header = authHeader
                if (header == null) {
                    builder
                } else {
                    // Media3 1.5 takes a fixed property map rather than a
                    // per-request provider, so the token is read once when the
                    // player is first created — which happens on first playback,
                    // after sign-in. Good enough for the stub session, whose
                    // token never changes. When Firebase lands and tokens start
                    // expiring, this needs a DataSource.Factory that re-reads
                    // per request, or memos will 401 mid-session.
                    val properties = header()
                        ?.let { mapOf("Authorization" to "Bearer " + it) }
                        ?: emptyMap()
                    val http = DefaultHttpDataSource.Factory()
                        .setDefaultRequestProperties(properties)
                    builder.setMediaSourceFactory(DefaultMediaSourceFactory(http))
                }
            }
            .build()
            .apply { addListener(playerListener) }
            .also { exo ->
                commandBus?.let { bus ->
                    mediaSession = MediaSession.Builder(appContext, FeedForwardingPlayer(exo, bus))
                        .setId(MEDIA_SESSION_ID)
                        .build()
                }
            }
    }

    private val playerListener = object : Player.Listener {

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> update { it.copy(isBuffering = true) }
                Player.STATE_READY -> update {
                    it.copy(isBuffering = false, durationMs = safeDuration())
                }
                Player.STATE_ENDED -> {
                    val finished = _state.value.memoId
                    update { it.copy(isPlaying = false, isBuffering = false) }
                    stopTicker()
                    if (finished != null) completionListener?.invoke(finished)
                }
                Player.STATE_IDLE -> update { it.copy(isBuffering = false) }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startTicker() else stopTicker()
        }

        override fun onPlayerError(error: PlaybackException) {
            update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    error = "That memo could not be played. Check your connection and try again.",
                )
            }
            stopTicker()
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            if (released) return
            update { it.copy(positionMs = player.currentPosition, durationMs = safeDuration()) }
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun play(memoId: String, audioUrl: String) {
        if (released) return
        _state.value = PlaybackState(memoId = memoId, isBuffering = true)
        player.setMediaItem(MediaItem.fromUri(audioUrl))
        player.prepare()
        player.playWhenReady = true
    }

    override fun pause() {
        if (released) return
        player.playWhenReady = false
    }

    override fun resume() {
        if (released || _state.value.memoId == null) return
        player.playWhenReady = true
    }

    override fun stop() {
        if (released) return
        player.stop()
        player.clearMediaItems()
        stopTicker()
        _state.value = PlaybackState()
    }

    override fun release() {
        if (released) return
        released = true
        stopTicker()
        mediaSession?.release()
        mediaSession = null
        player.removeListener(playerListener)
        player.release()
        _state.value = PlaybackState()
    }

    override fun onCompletion(listener: (memoId: String) -> Unit) {
        completionListener = listener
    }

    private fun safeDuration(): Long =
        player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0) ?: 0

    private fun startTicker() {
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    private fun stopTicker() {
        handler.removeCallbacks(ticker)
    }

    private inline fun update(transform: (PlaybackState) -> PlaybackState) {
        _state.value = transform(_state.value)
    }

    private companion object {
        /**
         * Four ticks a second. Enough for a progress announcement to feel
         * current, infrequent enough not to spend battery on a budget phone.
         */
        const val TICK_MS = 250L

        /**
         * Stable id so the platform recognises this as the same session across
         * a moderator toggle between feed models — otherwise switching feeds
         * mid-session would drop and re-create the media notification.
         */
        const val MEDIA_SESSION_ID = "tew-feed"
    }
}
