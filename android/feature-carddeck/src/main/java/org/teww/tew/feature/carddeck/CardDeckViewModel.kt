package org.teww.tew.feature.carddeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.teww.tew.core.TewResult
import org.teww.tew.core.model.Memo
import org.teww.tew.core.playback.PlaybackSession
import org.teww.tew.core.repo.FeedRepository

/** One card at a time. [endOfStream] is explicit for the same reason as radio's. */
data class CardDeckUiState(
    val memos: List<Memo> = emptyList(),
    val index: Int = 0,
    val loading: Boolean = false,
    val endOfStream: Boolean = false,
    val announcement: String = "",
    val onboardingDone: Boolean = false,
) {
    val current: Memo? get() = memos.getOrNull(index)
}

/**
 * The card-deck feed: one memo, acted on, then the next.
 *
 * The behaviour is deliberately identical to `:feature-radio`'s where it can
 * be — same repository, same playback session, same end-of-stream rule. The
 * usability test the two screens exist for only means something if the
 * difference between them is the *interaction model* and not accidental drift
 * in what they do.
 *
 * The one real difference: the card deck does not auto-advance. A card waits
 * for a decision, which is the whole point of the model.
 */
class CardDeckViewModel(
    private val feedRepository: FeedRepository,
    private val playbackSession: PlaybackSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardDeckUiState())
    val uiState: StateFlow<CardDeckUiState> = _uiState.asStateFlow()

    val playbackState = playbackSession.state

    private var nextCursor: String? = null
    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loading = true,
                announcement = "Finding memos for you.",
            )
            when (val result = feedRepository.feed(null)) {
                is TewResult.Ok -> {
                    nextCursor = result.value.nextCursor
                    val memos = result.value.memos
                    _uiState.value = _uiState.value.copy(
                        memos = memos,
                        index = 0,
                        loading = false,
                        endOfStream = memos.isEmpty(),
                        announcement = if (memos.isEmpty()) EMPTY_FEED else cardAnnouncement(memos[0]),
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

    fun onboardingFinished() {
        _uiState.value = _uiState.value.copy(onboardingDone = true)
    }

    fun playCurrent() {
        val memo = _uiState.value.current ?: return
        playbackSession.play(memo.id, memo.audioUrl)
    }

    fun togglePlayPause() {
        val playing = playbackSession.state.value.isPlaying
        if (playing) playbackSession.pause() else playbackSession.resume()
        announce(if (playing) "Paused." else "Playing.")
    }

    fun like() {
        val memo = _uiState.value.current ?: return
        viewModelScope.launch {
            when (feedRepository.setLiked(memo.id, true)) {
                is TewResult.Ok -> {
                    updateCurrent { it.copy(likedByMe = true) }
                    announce("Liked.")
                    nextCard()
                }

                is TewResult.Failure -> announce("That like did not save. Try again.")
            }
        }
    }

    fun skip() {
        val memo = _uiState.value.current ?: return
        viewModelScope.launch { feedRepository.skip(memo.id) }
        announce("Skipped.")
        nextCard()
    }

    /** Advance without recording an opinion — used after replying. */
    fun nextCard() {
        val state = _uiState.value
        val nextIndex = state.index + 1

        if (nextIndex < state.memos.size) {
            moveTo(nextIndex)
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
                    if (result.value.memos.isEmpty()) {
                        playbackSession.stop()
                        _uiState.value = state.copy(
                            loading = false,
                            endOfStream = true,
                            announcement = END_OF_STREAM,
                        )
                    } else {
                        _uiState.value = state.copy(
                            memos = state.memos + result.value.memos,
                            loading = false,
                        )
                        moveTo(nextIndex)
                    }
                }

                is TewResult.Failure -> _uiState.value = state.copy(
                    loading = false,
                    announcement = result.spoken,
                )
            }
        }
    }

    private fun moveTo(index: Int) {
        val state = _uiState.value
        val memo = state.memos.getOrNull(index) ?: return
        _uiState.value = state.copy(index = index, announcement = cardAnnouncement(memo))
        playbackSession.play(memo.id, memo.audioUrl)
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
            "That is the last card. The stream has ended — there is no more to swipe through. " +
                "Come back later, or record a memo of your own."
    }
}

/** Pure, so the wording is testable. */
internal fun cardAnnouncement(memo: Memo): String = buildString {
    append("Memo from ${memo.authorUsername}. ")
    memo.transcript?.let { append("$it ") }
    append("Like, skip, or reply.")
}
