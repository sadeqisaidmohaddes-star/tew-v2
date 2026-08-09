package org.teww.tew.feature.carddeck

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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
    ) = CardDeckViewModel(repo, playback) to playback

    @Test
    fun `the first card plays on start`() = runTest(dispatcher) {
        val (model, playback) = vm()

        model.start()
        advanceUntilIdle()

        assertEquals(1, playback.played.size)
        assertTrue(model.uiState.value.announcement.contains("Memo from"))
    }

    @Test
    fun `a card does not advance on its own`() = runTest(dispatcher) {
        // The difference from radio: the deck waits for a decision. If this
        // ever auto-advances, the two feed models stop being distinguishable
        // and the usability comparison measures nothing.
        val (model, playback) = vm()
        model.start()
        advanceUntilIdle()

        advanceUntilIdle()

        assertEquals(1, playback.played.size)
        assertEquals(0, model.uiState.value.index)
    }

    @Test
    fun `liking moves to the next card`() = runTest(dispatcher) {
        val (model, playback) = vm()
        model.start(); advanceUntilIdle()

        model.like(); advanceUntilIdle()

        assertEquals(1, model.uiState.value.index)
        assertEquals(2, playback.played.size)
    }

    @Test
    fun `skipping moves to the next card and announces the new one`() = runTest(dispatcher) {
        val (model, playback) = vm()
        model.start(); advanceUntilIdle()
        val firstCard = model.uiState.value.current!!

        model.skip(); advanceUntilIdle()

        assertEquals(1, model.uiState.value.index)
        assertEquals(2, playback.played.size)
        // The transient "Skipped." is replaced by the next card's description,
        // which is what the user actually needs to hear next.
        val announcement = model.uiState.value.announcement
        assertTrue(announcement.contains("Memo from"))
        assertFalse(announcement.contains(firstCard.authorUsername))
    }

    @Test
    fun `the deck ends, and says so`() = runTest(dispatcher) {
        val (model, playback) = vm()
        model.start(); advanceUntilIdle()

        repeat(20) { model.skip(); advanceUntilIdle() }

        assertTrue("deck never ended", model.uiState.value.endOfStream)
        assertTrue(model.uiState.value.announcement.contains("stream has ended"))
        assertTrue(playback.stopped)
    }

    @Test
    fun `an empty feed is announced`() = runTest(dispatcher) {
        val (model, _) = vm(InMemoryFeedRepository(latencyMs = 0, seed = emptyList()))

        model.start(); advanceUntilIdle()

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
        assertTrue(spoken.contains("Good morning."))
        assertTrue(spoken.contains("Like, skip, or reply."))
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
