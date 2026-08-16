package org.teww.tew.feature.carddeck

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.teww.tew.core.model.Memo
import org.teww.tew.core.model.Moderation
import org.teww.tew.core.playback.PlaybackSession
import org.teww.tew.core.playback.PlaybackState
import org.teww.tew.core.repo.InMemoryFeedRepository

private class FakePlayback : PlaybackSession {
    override val state = MutableStateFlow(PlaybackState()) as StateFlow<PlaybackState>
    val played = mutableListOf<String>()
    var stopped = false
    override fun play(memoId: String, audioUrl: String) { played += memoId }
    override fun pause() = Unit
    override fun resume() = Unit
    override fun stop() { stopped = true }
    override fun release() = Unit
    override fun onCompletion(listener: (String) -> Unit) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class CardDeckViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(
        repo: InMemoryFeedRepository = InMemoryFeedRepository(latencyMs = 0),
        playback: FakePlayback = FakePlayback(),
        screenReader: Boolean = false,
    ) = CardDeckViewModel(repo, playback, screenReaderActive = { screenReader }) to playback

    /**
     * Start the deck the way the screen does: load, finish onboarding, and let
     * the narrator report that it has finished speaking. That last step is
     * what releases the audio — see [CardDeckViewModel.announcementSpoken].
     */
    private fun TestScope.begin(model: CardDeckViewModel) {
        model.start()
        advanceUntilIdle()
        model.onboardingFinished()
        model.announcementSpoken()
    }

    @Test
    fun `the first card plays once its announcement has been spoken`() = runTest(dispatcher) {
        val (model, playback) = vm()

        model.start()
        advanceUntilIdle()
        model.onboardingFinished()

        // Announced, but silent: the narrator is still saying who it is from.
        assertTrue(model.uiState.value.announcement.contains("Memo from"))
        assertTrue("audio started over the announcement", playback.played.isEmpty())

        model.announcementSpoken()

        assertEquals(1, playback.played.size)
    }

    @Test
    fun `nothing plays underneath onboarding`() = runTest(dispatcher) {
        // Onboarding narrates every step. The deck used to start memo one as
        // soon as the feed arrived, which put a stranger's voice underneath
        // the instructions telling you how to use the app.
        val (model, playback) = vm()

        model.start()
        advanceUntilIdle()
        model.announcementSpoken()

        assertTrue(playback.played.isEmpty())

        model.onboardingFinished()
        model.announcementSpoken()

        assertEquals(1, playback.played.size)
    }

    @Test
    fun `under a screen reader nothing starts on its own`() = runTest(dispatcher) {
        // TalkBack is already speaking, and Android will not say when it has
        // stopped. So the deck announces and waits to be asked.
        val (model, playback) = vm(screenReader = true)

        model.start()
        advanceUntilIdle()
        model.onboardingFinished()
        model.announcementSpoken()

        assertTrue(playback.played.isEmpty())
        assertTrue(model.uiState.value.announcement.contains("Press play"))

        model.playCurrent()

        assertEquals(1, playback.played.size)
    }

    @Test
    fun `the play button works on a card that has never played`() = runTest(dispatcher) {
        // Under a screen reader nothing is loaded, so play/pause has nothing to
        // resume. It has to start the memo instead, or the button is dead for
        // the people the deck exists for.
        val (model, playback) = vm(screenReader = true)
        model.start(); advanceUntilIdle(); model.onboardingFinished()

        model.togglePlayPause()

        assertEquals(1, playback.played.size)
    }

    @Test
    fun `a card does not advance on its own`() = runTest(dispatcher) {
        // The difference from radio: the deck waits for a decision. If this
        // ever auto-advances, the two feed models stop being distinguishable
        // and the usability comparison measures nothing.
        val (model, playback) = vm()
        begin(model)

        advanceUntilIdle()

        assertEquals(1, playback.played.size)
        assertEquals(0, model.uiState.value.index)
    }

    @Test
    fun `liking moves to the next card`() = runTest(dispatcher) {
        val (model, playback) = vm()
        begin(model)

        model.like(); advanceUntilIdle()
        model.announcementSpoken()

        assertEquals(1, model.uiState.value.index)
        assertEquals(2, playback.played.size)
    }

    @Test
    fun `skipping moves to the next card and announces the new one`() = runTest(dispatcher) {
        val (model, playback) = vm()
        begin(model)
        val firstCard = model.uiState.value.current!!

        model.skip(); advanceUntilIdle()
        model.announcementSpoken()

        assertEquals(1, model.uiState.value.index)
        assertEquals(2, playback.played.size)
        // The transient "Skipped." is replaced by the next card's description,
        // which is what the user actually needs to hear next.
        val announcement = model.uiState.value.announcement
        assertTrue(announcement.contains("Memo from"))
        assertFalse(announcement.contains(firstCard.authorUsername))
    }

    @Test
    fun `a late narrator callback does not play a card the user has left`() = runTest(dispatcher) {
        // Skip while the announcement is still being spoken. When it finishes,
        // the memo it was describing must not start — the user is two cards on.
        val (model, playback) = vm()
        begin(model)
        val played = playback.played.size

        model.skip(); advanceUntilIdle()
        model.skip(); advanceUntilIdle()
        model.announcementSpoken()

        assertEquals(model.uiState.value.current!!.id, playback.played.last())
        assertEquals(played + 1, playback.played.size)
    }

    @Test
    fun `the deck ends, and says so`() = runTest(dispatcher) {
        val (model, playback) = vm()
        begin(model)

        repeat(20) { model.skip(); advanceUntilIdle(); model.announcementSpoken() }

        assertTrue("deck never ended", model.uiState.value.endOfStream)
        assertTrue(model.uiState.value.announcement.contains("stream has ended"))
        assertTrue(playback.stopped)
    }

    @Test
    fun `an empty feed is announced`() = runTest(dispatcher) {
        val (model, _) = vm(InMemoryFeedRepository(latencyMs = 0, seed = emptyList()))

        begin(model)

        assertTrue(model.uiState.value.endOfStream)
        assertEquals(CardDeckViewModel.EMPTY_FEED, model.uiState.value.announcement)
    }

    @Test
    fun `onboarding gates the deck until finished`() = runTest(dispatcher) {
        val (model, _) = vm()

        assertFalse(model.uiState.value.onboardingDone)
        model.onboardingFinished()
        assertTrue(model.uiState.value.onboardingDone)
    }
}

class CardAnnouncementTest {

    @Test
    fun `the announcement names the author and offers the choices`() {
        val memo = Memo("m", "amina", "u", 1, 0, "Good morning.", false, Moderation.visible)

        val spoken = cardAnnouncement(memo)

        assertTrue(spoken.contains("amina"))
        assertTrue(spoken.contains("Like, skip, or reply."))
    }

    @Test
    fun `the announcement does not read out the transcript`() {
        // The memo's own audio says these words in the author's voice a moment
        // later. Speaking them here too is what the 2026-08-16 device session
        // heard: the same sentence twice, in two voices, at the same time.
        val memo = Memo("m", "amina", "u", 1, 0, "Good morning.", false, Moderation.visible)

        assertFalse(cardAnnouncement(memo).contains("Good morning."))
    }

    @Test
    fun `under a screen reader the announcement says play is theirs to press`() {
        // Nothing auto-plays under TalkBack, so not saying this leaves a person
        // waiting for audio that is never going to start.
        val memo = Memo("m", "amina", "u", 1, 0, "Good morning.", false, Moderation.visible)

        assertTrue(cardAnnouncement(memo, screenReader = true).contains("Press play"))
    }

    @Test
    fun `a memo with no transcript still offers the choices`() {
        val memo = Memo("m", "joseph", "u", 1, 0, null, false, Moderation.visible)

        assertTrue(cardAnnouncement(memo).contains("Like, skip, or reply."))
    }
}

class OnboardingScriptTest {

    @Test
    fun `onboarding promises the screen-reader route, not only swipes`() {
        // If onboarding taught swipes as the only way through, a screen-reader
        // user would be locked out on the first screen if the passthrough
        // spike comes back negative. This test is the guard on that.
        val script = onboardingSteps().joinToString(" ") { it.spoken }

        assertTrue(script.contains("actions menu"))
        assertTrue(script.contains("button on screen"))
    }

    @Test
    fun `onboarding promises no timed or precise gesture`() {
        val script = onboardingSteps().joinToString(" ") { it.spoken }

        assertTrue(script.contains("nothing to time"))
    }

    @Test
    fun `onboarding warns that the deck ends`() {
        val script = onboardingSteps().joinToString(" ") { it.spoken }

        assertTrue(script.contains("no endless pile"))
    }

    @Test
    fun `every step says something`() {
        assertTrue(onboardingSteps().isNotEmpty())
        onboardingSteps().forEach { assertTrue(it.spoken.isNotBlank()) }
    }
}
