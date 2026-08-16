package org.teww.tew.feature.carddeck

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
    /**
     * Whether a screen reader is driving the screen right now. When it is,
     * this model never starts audio on its own — see [announceThenPlay].
     *
     * A function rather than a value because the user can toggle TalkBack
     * mid-session without leaving the app.
     */
    private val screenReaderActive: () -> Boolean = { false },
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardDeckUiState())
    val uiState: StateFlow<CardDeckUiState> = _uiState.asStateFlow()

    val playbackState = playbackSession.state

    private var nextCursor: String? = null
    private var started = false

    /** The memo waiting for the narrator to stop talking. See [announceThenPlay]. */
    private var pendingPlay: Memo? = null

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
                    )
                    if (memos.isEmpty()) announce(EMPTY_FEED) else announceThenPlay(memos[0])
                }

                is TewResult.Failure -> _uiState.value = _uiState.value.copy(
                    loading = false,
                    announcement = result.spoken,
                )
            }
        }
    }

    /**
     * Onboarding is still speaking until this is called, so the first card is
     * announced here rather than when the feed arrives.
     *
     * The feed still loads in the background during onboarding — only the
     * sound waits. Before the 2026-08-16 fix the first memo began playing
     * underneath the onboarding narration, which is the same collision as the
     * one this whole arrangement exists to prevent, just on a different screen.
     */
    fun onboardingFinished() {
        if (_uiState.value.onboardingDone) return
        _uiState.value = _uiState.value.copy(onboardingDone = true)
        _uiState.value.current?.let { announceThenPlay(it) }
    }

    /** See RadioViewModel.onCommand — the two feeds funnel routes identically. */
    fun onCommand(command: FeedCommand) {
        when (command) {
            FeedCommand.PLAY_PAUSE -> togglePlayPause()
            FeedCommand.LIKE -> like()
            FeedCommand.SKIP -> skip()
            FeedCommand.REPLAY -> playCurrent()
            FeedCommand.REPLY, FeedCommand.REPORT ->
                _uiState.value.current?.let { onNavigationCommand?.invoke(command, it.id) }
        }
    }

    /** Set by the screen, which owns navigation. */
    var onNavigationCommand: ((FeedCommand, String) -> Unit)? = null

    /**
     * Start this card's audio because the person asked for it — the play
     * button, the replay action, a media button or a voice command.
     *
     * Under a screen reader this is the *only* thing that starts a memo, which
     * is why the card deck's button says "Play" until it has been used.
     */
    fun playCurrent() {
        val memo = _uiState.value.current ?: return
        // They did not wait for the narrator, so neither does the app: drop
        // anything queued behind it rather than replaying this memo from the
        // top a second later when the announcement finishes.
        pendingPlay = null
        playbackSession.play(memo.id, memo.audioUrl)
    }

    fun togglePlayPause() {
        pendingPlay = null

        // Under a screen reader the deck announces a card and waits, so there
        // is nothing loaded to resume — this press is what starts it. Without
        // this the media-button and voice routes are dead on a card that has
        // not been played yet, while the on-screen button works, and the three
        // routes are supposed to be interchangeable.
        val memo = _uiState.value.current
        if (playbackSession.state.value.memoId == null && memo != null) {
            playbackSession.play(memo.id, memo.audioUrl)
            announce("Playing.")
            return
        }

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
            pendingPlay = null
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
                        pendingPlay = null
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
        _uiState.value = state.copy(index = index)
        announceThenPlay(memo)
    }

    /**
     * Describe the card, and line its audio up to start *after* the
     * description has been spoken — never at the same time.
     *
     * This is the fix for what the 2026-08-16 device session found. The deck
     * used to set an announcement and call `play` on the very next line, so
     * the narrator and the memo spoke simultaneously, saying the same words
     * twice over the top of each other.
     *
     * Three cases, and only the middle one ever starts audio by itself:
     *
     * - **Onboarding still showing** — the announcement is recorded so the
     *   deck is correct when it appears, but nothing is queued to play;
     *   onboarding has its own voice and a memo would talk over it.
     * - **No screen reader** — the narrator speaks the announcement and the
     *   screen reports back via [announcementSpoken], which is what starts
     *   the audio.
     * - **A screen reader** — nothing is queued. TalkBack is speaking and
     *   there is no API that says when it has stopped, so the app does not
     *   guess: the person presses play, and the announcement says so.
     */
    private fun announceThenPlay(memo: Memo) {
        val underScreenReader = screenReaderActive()
        pendingPlay = memo.takeIf { _uiState.value.onboardingDone && !underScreenReader }
        announce(cardAnnouncement(memo, underScreenReader))
    }

    /**
     * The narrator has finished speaking the current announcement, so audio
     * waiting on it can start.
     *
     * Called by the screen, which owns the narrator. Under a screen reader it
     * is never called — the narrator stays silent and there is nothing queued
     * for it to release anyway.
     */
    fun announcementSpoken() {
        val memo = pendingPlay ?: return
        pendingPlay = null
        // The card can have moved on while the narrator was talking — a media
        // button or a voice command does not wait for it. Starting the audio
        // now would play a memo the user has already left.
        if (_uiState.value.current?.id != memo.id) return
        playbackSession.play(memo.id, memo.audioUrl)
    }

    override fun onCleared() {
        playbackSession.stop()
        pendingPlay = null
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
/**
 * What the deck says about a card.
 *
 * The transcript is deliberately **not** spoken here. The memo's own audio
 * says those same words in the author's voice a second later, and the
 * 2026-08-16 device session is what a listener hears when both happen at once:
 * the sentence twice, in two voices, neither of them followable. The
 * transcript stays on the card as text, where a screen reader reaches it on
 * request and a sighted moderator can read it — it is not lost, it is just no
 * longer read aloud on top of the recording it duplicates.
 *
 * [screenReader] changes only the closing line: under a screen reader nothing
 * plays on its own, so the person has to be told that play is theirs to press.
 */
internal fun cardAnnouncement(memo: Memo, screenReader: Boolean = false): String = buildString {
    append("Memo from ${memo.authorUsername}. ")
    append(if (screenReader) "Press play, or like, skip, or reply." else "Like, skip, or reply.")
}
