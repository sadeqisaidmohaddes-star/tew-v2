package org.teww.tew.core.repo

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.teww.tew.core.TewResult
import org.teww.tew.core.model.FeedPage
import org.teww.tew.core.valueOrNull

class InMemoryFeedRepositoryTest {

    private fun repo() = InMemoryFeedRepository(latencyMs = 0)

    private suspend fun page(r: FeedRepository, cursor: String? = null): FeedPage =
        r.feed(cursor).valueOrNull()!!

    @Test
    fun `first page returns memos and a cursor`() = runTest {
        val first = page(repo())

        assertTrue(first.memos.isNotEmpty())
        assertFalse("sample data should span more than one page", first.isLastPage)
    }

    @Test
    fun `the stream ends`() = runTest {
        // Non-negotiable #3. Paging to the end must produce a null cursor
        // rather than looping — this is the test that would catch an
        // infinite feed being introduced later.
        val r = repo()
        var current = page(r)
        var guard = 0

        while (!current.isLastPage && guard++ < 50) {
            current = page(r, current.nextCursor)
        }

        assertTrue("feed never terminated", current.isLastPage)
        assertNull(current.nextCursor)
    }

    @Test
    fun `skipped memos do not come back`() = runTest {
        val r = repo()
        val first = page(r).memos.first()

        r.skip(first.id)

        val after = page(r)
        assertFalse(after.memos.any { it.id == first.id })
    }

    @Test
    fun `liking is remembered`() = runTest {
        val r = repo()
        val target = page(r).memos.first()
        assertFalse(target.likedByMe)

        r.setLiked(target.id, true)

        val reloaded = page(r).memos.first { it.id == target.id }
        assertTrue(reloaded.likedByMe)
    }

    @Test
    fun `liking is reversible`() = runTest {
        val r = repo()
        val target = page(r).memos.first()

        r.setLiked(target.id, true)
        r.setLiked(target.id, false)

        assertFalse(page(r).memos.first { it.id == target.id }.likedByMe)
    }

    @Test
    fun `paging past the end yields an empty final page`() = runTest {
        val r = repo()
        val result = r.feed("9999")

        val value = (result as TewResult.Ok).value
        assertEquals(emptyList<Any>(), value.memos)
        assertTrue(value.isLastPage)
    }
}
