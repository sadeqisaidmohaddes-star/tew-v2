package org.teww.tew.feature.radio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.teww.tew.core.TewResult
import org.teww.tew.core.model.Memo
import org.teww.tew.core.playback.FeedCommand
import org.teww.tew.core.playback.PlaybackSession
import org.teww.tew.core.repo.FeedRepository

/**
 * State of the radio timeline.
 *
 * [endOfStream] is a first-class field rather than something inferred from an
 * empty list, because non-negotiable #3 — *the stream ends* — needs the ending
 * to be an event the screen can announce, not an absence the user has to
 * notice.
 */
data class RadioUiState(
    val memos: List<Memo> = emptyList(),
    val index: Int = 0,
    val loading: Boolean = false,
    val endOfStream: Boolean = false,
    val announcement: String = "",
) {
    val current: Memo? get() = memos.getOrNull(index)
}

/**
 * The sequential feed: memos play one after another, like a radio station.
 *
 * Advancing happens on playback completion rather than on a timer, so a slow
 * connection stretches the gap instead of cutting a memo off. The next page is
 * fetched when the listener nears the end of the current one, so the pause
 * between memos is not a network round trip on a throttled connection.
 */
class RadioViewModel(
    private val feedRepository: FeedRepository,
    private val playbackSession: PlaybackSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RadioUiState())
    val uiState: StateFlow<RadioUiState> = _uiState.asStateFlow()

    val playbackState = playbackSession.state

    private var nextCursor: String? = null
    private var started = false

    init {
        playbackSession.onCompletion { advance() }
    }

    fun start() {
        if (started) return
        started = true
        loadFirstPage()
    }

    private fun loadFirstPage() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loading = true,
                announcement = "Finding memos for you.",
            )
            when (val result = feedRepository.feed(null)) {
                is TewResult.Ok -> {
                    nextCursor = result.value.nextCursor
                    val memos = result.value.memos
                    _uiState.value = RadioUiState(
                        memos = memos,
                        index = 0,
                        loading = false,
                        endOfStream = memos.isEmpty() && result.value.isLastPage,
                        announcement = if (memos.isEmpty()) {
                            EMPTY_FEED
                        } else {
                            "Playing memos. ${describePosition(0, memos.size)}"
                        },
                    )
                    memos.firstOrNull()?.let { playbackSession.play(it.id, it.audioUrl) }
                }

                is TewResult.Failure -> _uiState.value = _uiState.value.copy(
                    loading = false,
                    announcement = result.spoken,
                )
            }
        }
    }

    /** Move to the next memo, loading another page first if one is needed. */
    fun advance() {
        val state = _uiState.value
        val nextIndex = state.index + 1

        if (nextIndex < state.memos.size) {
            playAt(nextIndex)
            maybePrefetch(nextIndex, state.memos.size)
            return
        }

        val cursor = nextCursor
        if (cursor == null) {
            playbackSession.stop()
            _uiState.value = state.copy(endOfStream = true, announcement = END_OF_STREAM)
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(loading = true)
            when (val result = feedRepository.feed(cursor)) {
                is TewResult.Ok -> {
                    nextCursor = result.value.nextCursor
                    val combined = state.memos + result.value.memos
                    if (result.value.memos.isEmpty()) {
                        playbackSession.stop()
                        _uiState.value = state.copy(
                            loading = false,
                            endOfStream = true,
                            announcement = END_OF_STREAM,
                        )
                    } else {
                        _uiState.value = state.copy(memos = combined, loading = false)
                        playAt(nextIndex)
                    }
                }

                is TewResult.Failure -> _uiState.value = state.copy(
                    loading = false,
                    announcement = result.spoken,
                )
            }
        }
    }

    private fun maybePrefetch(index: Int, size: Int) {
        val cursor = nextCursor ?: return
        if (size - index > PREFETCH_THRESHOLD) return
        viewModelScope.launch {
            when (val result = feedRepository.feed(cursor)) {
                is TewResult.Ok -> {
                    nextCursor = result.value.nextCursor
                    _uiState.value = _uiState.value.copy(
                        memos = _uiState.value.memos + result.value.memos,
                    )
                }
                // A failed prefetch stays silent on purpose: nothing has gone
                // wrong from the listener's side yet, and announcing a problem
                // they cannot act on interrupts the memo they are hearing.
                is TewResult.Failure -> Unit
            }
        }
    }

    private fun playAt(index: Int) {
        val state = _uiState.value
        val memo = state.memos.getOrNull(index) ?: return
        _uiState.value = state.copy(
            index = index,
            announcement = describePosition(index, state.memos.size),
        )
        playbackSession.play(memo.id, memo.audioUrl)
    }

    /**
     * Single entry point for commands arriving from outside the UI — media
     * buttons and voice. Routing every route through one method is what stops
     * them drifting apart: a route cannot support an action the others do not,
     * because there is only one place any of them can land.
     *
     * REPLY and REPORT need a destination the view model does not own, so they
     * are handed up via [onNavigationCommand].
     */
    fun onCommand(command: FeedCommand) {
        when (command) {
            FeedCommand.PLAY_PAUSE -> togglePlayPause()
            FeedCommand.LIKE -> likeCurrent()
            FeedCommand.SKIP -> skipCurrent()
            FeedCommand.REPLAY -> replayCurrent()
            FeedCommand.REPLY, FeedCommand.REPORT ->
                _uiState.value.current?.let { onNavigationCommand?.invoke(command, it.id) }
        }
    }

    /** Set by the screen, which owns navigation. */
    var onNavigationCommand: ((FeedCommand, String) -> Unit)? = null

    fun replayCurrent() {
        val memo = _uiState.value.current ?: return
        playbackSession.play(memo.id, memo.audioUrl)
        announce("Playing again.")
    }

    fun togglePlayPause() {
        val playing = playbackSession.state.value.isPlaying
        if (playing) playbackSession.pause() else playbackSession.resume()
        announce(if (playing) "Paused." else "Playing.")
    }

    fun likeCurrent() {
        val memo = _uiState.value.current ?: return
        val newValue = !memo.likedByMe
        viewModelScope.launch {
            when (feedRepository.setLiked(memo.id, newValue)) {
                is TewResult.Ok -> {
                    updateCurrent { it.copy(likedByMe = newValue) }
                    announce(if (newValue) "Liked." else "Like removed.")
                }

                is TewResult.Failure -> announce("That like did not save. Try again.")
            }
        }
    }

    fun skipCurrent() {
        val memo = _uiState.value.current ?: return
        viewModelScope.launch { feedRepository.skip(memo.id) }
        announce("Skipped.")
        advance()
    }

    override fun onCleared() {
        playbackSession.stop()
        super.onCleared()
    }

    private fun announce(text: String) {
        _uiState.value = _uiState.value.copy(announcement = text)
    }

    private inline fun updateCurrent(transform: (Memo) -> Memo) {
        val state = _uiState.value
        val memo = state.current ?: return
        val updated = state.memos.toMutableList()
        updated[state.index] = transform(memo)
        _uiState.value = state.copy(memos = updated)
    }

    companion object {
        const val EMPTY_FEED =
            "There are no memos waiting for you right now. Come back later, or record one yourself."

        const val END_OF_STREAM =
            "That is everything for now. The stream has ended — there is no more to scroll. " +
                "Come back later, or record a memo of your own."

        /** Fetch the next page once this few memos remain. */
        private const val PREFETCH_THRESHOLD = 2
    }
}

/** Pure, so the wording can be tested without a device. */
internal fun describePosition(index: Int, total: Int): String =
    "Memo ${index + 1} of $total."
