package org.teww.tew.feature.radio

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
import org.teww.tew.core.playback.PlaybackSession
import org.teww.tew.core.playback.PlaybackState
import org.teww.tew.core.repo.InMemoryFeedRepository
import org.teww.tew.core.repo.sampleMemos

/** Records what it was told to do; plays nothing. */
private class FakePlaybackSession : PlaybackSession {
    override val state = MutableStateFlow(PlaybackState()) as StateFlow<PlaybackState>
    val played = mutableListOf<String>()
    var stopped = false
    private var completion: ((String) -> Unit)? = null

    override fun play(memoId: String, audioUrl: String) { played += memoId }
    override fun pause() = Unit
    override fun resume() = Unit
    override fun stop() { stopped = true }
    override fun release() = Unit
    override fun onCompletion(listener: (String) -> Unit) { completion = listener }

    fun finishCurrent() = completion?.invoke(played.last())
}

@OptIn(ExperimentalCoroutinesApi::class)
class RadioViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        repo: InMemoryFeedRepository = InMemoryFeedRepository(latencyMs = 0),
        playback: FakePlaybackSession = FakePlaybackSession(),
    ) = RadioViewModel(repo, playback) to playback

    @Test
    fun `starting plays the first memo`() = runTest(dispatcher) {
        val (vm, playback) = viewModel()

        vm.start()
        advanceUntilIdle()

        assertEquals(listOf(sampleMemos().first().id), playback.played)
        assertTrue(vm.uiState.value.announcement.contains("Memo 1 of"))
    }

    @Test
    fun `finishing a memo advances to the next without being asked`() = runTest(dispatcher) {
        val (vm, playback) = viewModel()
        vm.start()
        advanceUntilIdle()

        playback.finishCurrent()
        advanceUntilIdle()

        assertEquals(2, playback.played.size)
        assertEquals(1, vm.uiState.value.index)
    }

    @Test
    fun `the stream ends, and says so`() = runTest(dispatcher) {
        // Non-negotiable #3. Playing to the end must produce an explicit,
        // speakable ending rather than silence or a loop.
        val (vm, playback) = viewModel()
        vm.start()
        advanceUntilIdle()

        repeat(sampleMemos().size + 3) {
            playback.finishCurrent()
            advanceUntilIdle()
        }

        assertTrue("never reached the end", vm.uiState.value.endOfStream)
        assertTrue(vm.uiState.value.announcement.contains("stream has ended"))
        assertTrue("playback should stop at the end", playback.stopped)
    }

    @Test
    fun `an empty feed is announced rather than left silent`() = runTest(dispatcher) {
        val (vm, _) = viewModel(InMemoryFeedRepository(latencyMs = 0, seed = emptyList()))

        vm.start()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.endOfStream)
        assertEquals(RadioViewModel.EMPTY_FEED, vm.uiState.value.announcement)
    }

    @Test
    fun `liking is announced and reflected in state`() = runTest(dispatcher) {
        val (vm, _) = viewModel()
        vm.start()
        advanceUntilIdle()

        vm.likeCurrent()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.current!!.likedByMe)
        assertEquals("Liked.", vm.uiState.value.announcement)
    }

    @Test
    fun `liking twice unlikes`() = runTest(dispatcher) {
        val (vm, _) = viewModel()
        vm.start(); advanceUntilIdle()

        vm.likeCurrent(); advanceUntilIdle()
        vm.likeCurrent(); advanceUntilIdle()

        assertFalse(vm.uiState.value.current!!.likedByMe)
        assertEquals("Like removed.", vm.uiState.value.announcement)
    }

    @Test
    fun `skipping moves on`() = runTest(dispatcher) {
        val (vm, playback) = viewModel()
        vm.start(); advanceUntilIdle()

        vm.skipCurrent(); advanceUntilIdle()

        assertEquals(2, playback.played.size)
    }
}

class PlaybackLineTest {

    @Test
    fun `buffering is stated, not shown as a stalled position`() {
        val line = playbackLine(PlaybackState(memoId = "m", isBuffering = true))
        assertTrue(line.contains("Loading"))
    }

    @Test
    fun `an error speaks the error`() {
        val line = playbackLine(PlaybackState(memoId = "m", error = "That memo could not be played."))
        assertEquals("That memo could not be played.", line)
    }

    @Test
    fun `playing reports the time left`() {
        val line = playbackLine(
            PlaybackState(memoId = "m", isPlaying = true, positionMs = 4_000, durationMs = 10_000),
        )
        assertTrue(line.contains("6 seconds left"))
    }

    @Test
    fun `idle is stated`() {
        assertEquals("Nothing playing.", playbackLine(PlaybackState()))
    }
}

class PositionTextTest {

    @Test
    fun `positions are one-based for humans`() {
        assertEquals("Memo 1 of 6.", describePosition(0, 6))
        assertEquals("Memo 6 of 6.", describePosition(5, 6))
    }
}
