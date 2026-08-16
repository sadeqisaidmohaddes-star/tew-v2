package org.teww.tew.core.repo

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.teww.tew.core.TewResult

/**
 * The built-in feed is what a tester sees when no server address is set, and
 * the whole reason it exists is that it works with nothing reachable. These
 * guard the two ways that quietly stops being true: audio drifting back to a
 * remote URL, and a duration that no longer matches its file.
 */
class SeedContentTest {

    @Test
    fun `every seeded memo plays from inside the APK`() {
        // This used to be a bell hosted on Wikimedia, which meant the offline
        // feed needed the internet. If someone reintroduces an http URL here,
        // the app looks fine in the office and dies on a phone with no signal.
        val remote = (sampleMemos() + sampleOwnMemos())
            .filterNot { it.audioUrl.startsWith("rawresource://") }
        assertTrue("not bundled: $remote", remote.isEmpty())
    }

    @Test
    fun `every seeded reply plays from inside the APK`() {
        val remote = sampleComments().values.flatten()
            .filterNot { it.audioUrl.startsWith("rawresource://") }
        assertTrue("not bundled: $remote", remote.isEmpty())
    }

    @Test
    fun `durations are real, not placeholders`() {
        // Generated from the audio by tools/generate-seed-audio.sh. A zero here
        // means someone hand-wrote a memo into the generated file, and the app
        // will announce a length the clip does not have.
        (sampleMemos() + sampleOwnMemos()).forEach {
            assertTrue("${it.id} has duration ${it.durationMs}", it.durationMs > 1_000)
        }
    }

    @Test
    fun `people sound different from each other`() {
        // Each seeded author gets their own synthesised voice. A feed where
        // every memo is the same voice cannot be used to compare the radio
        // timeline against the card deck, which is the point of the test build.
        assertTrue(sampleMemos().map { it.authorUsername }.distinct().size >= 4)
    }

    @Test
    fun `the feed comes back newest first`() = runTest {
        // The ordering decision in STATE.md, enforced on this side too so the
        // built-in feed and a real server look the same.
        val page = InMemoryFeedRepository(latencyMs = 0).feed(null)
        val memos = (page as TewResult.Ok).value.memos
        assertEquals(
            memos.map { it.id },
            memos.sortedByDescending { it.postedAtEpochSeconds }.map { it.id },
        )
    }

    @Test
    fun `a memo recorded now goes to the top of the feed`() = runTest {
        val repo = InMemoryFeedRepository(latencyMs = 0)
        val posted = (repo.postMemo(java.io.File("memo.m4a")) as TewResult.Ok).value
        val first = (repo.feed(null) as TewResult.Ok).value.memos.first()
        assertEquals(posted.id, first.id)
    }

    @Test
    fun `replies are seeded so the comments screen has something to play`() = runTest {
        val repo = InMemoryFeedRepository(latencyMs = 0)
        val memoWithReplies = sampleComments().keys.first()
        val replies = (repo.comments(memoWithReplies) as TewResult.Ok).value
        assertTrue(replies.isNotEmpty())
    }
}
