package org.teww.tew.feature.account

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.teww.tew.core.FailureKind
import org.teww.tew.core.TewResult
import org.teww.tew.core.auth.StubAuthSession
import org.teww.tew.core.model.Memo
import org.teww.tew.core.model.Moderation
import org.teww.tew.core.model.ReportReason
import org.teww.tew.core.repo.ModerationRepository

private fun memo(id: String) = Memo(id, "you", "u", 1, 0, "A memo.", false, Moderation.visible)

private class FakeModeration(
    own: List<Memo> = listOf(memo("a"), memo("b")),
    private val deleteFails: Boolean = false,
) : ModerationRepository {

    private val mine = own.toMutableList()
    val deleted = mutableListOf<String>()

    override suspend fun report(memoId: String, reason: ReportReason) = TewResult.Ok(Unit)

    override suspend fun myMemos(): TewResult<List<Memo>> = TewResult.Ok(mine.toList())

    override suspend fun appeal(memoId: String, text: String) = TewResult.Ok(Unit)

    override suspend fun deleteMemo(memoId: String): TewResult<Unit> {
        if (deleteFails) {
            return TewResult.Failure(
                kind = FailureKind.NETWORK,
                spoken = "You seem to be offline. Check your connection and try again.",
            )
        }
        deleted += memoId
        mine.removeAll { it.id == memoId }
        return TewResult.Ok(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(repo: FakeModeration = FakeModeration()) =
        AccountViewModel(StubAuthSession(), repo) to repo

    @Test
    fun `deleting removes the memo and says the recording is gone`() = runTest(dispatcher) {
        val (model, repo) = vm()
        model.loadMyMemos(); advanceUntilIdle()

        model.deleteMemo("a"); advanceUntilIdle()

        assertEquals(listOf("a"), repo.deleted)
        assertFalse(model.uiState.value.myMemos.any { it.id == "a" })
        // The one thing a person who cannot see the list needs told: it is
        // actually gone, not just off the screen.
        assertTrue(model.uiState.value.announcement.contains("gone"))
    }

    @Test
    fun `a failed delete says so and keeps the memo`() = runTest(dispatcher) {
        // Trimming the list on failure would tell someone their recording was
        // destroyed when it is still on the server — the worst possible lie for
        // this particular button.
        val (model, _) = vm(FakeModeration(deleteFails = true))
        model.loadMyMemos(); advanceUntilIdle()

        model.deleteMemo("a"); advanceUntilIdle()

        assertTrue(model.uiState.value.myMemos.any { it.id == "a" })
        assertTrue(model.uiState.value.announcement.contains("offline"))
    }

    @Test
    fun `deleting the last memo leaves an empty list, not a stale one`() = runTest(dispatcher) {
        val (model, _) = vm(FakeModeration(own = listOf(memo("only"))))
        model.loadMyMemos(); advanceUntilIdle()

        model.deleteMemo("only"); advanceUntilIdle()

        assertTrue(model.uiState.value.myMemos.isEmpty())
    }
}
