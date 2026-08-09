package org.teww.tew.core.net

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.teww.tew.core.model.AppealState
import org.teww.tew.core.model.ModerationState

class DtoMappingTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun `feed page parses and maps`() {
        val body = """
            {
              "memos": [
                {
                  "id": "m1",
                  "author_username": "amina",
                  "audio_url": "https://example.org/m1.m4a",
                  "duration_ms": 21000,
                  "posted_at": 1770000000,
                  "transcript": "hello",
                  "liked_by_me": true
                }
              ],
              "next_cursor": "abc"
            }
        """.trimIndent()

        val page = json.decodeFromString<FeedPageDto>(body).toDomain()

        assertEquals(1, page.memos.size)
        assertEquals("amina", page.memos[0].authorUsername)
        assertTrue(page.memos[0].likedByMe)
        assertEquals("abc", page.nextCursor)
        assertFalse(page.isLastPage)
    }

    @Test
    fun `absent next_cursor means the stream ended`() {
        // The backend may omit the key entirely rather than send null, and
        // both have to mean "end of feed" — getting this wrong would make the
        // feed look infinite, breaking non-negotiable #3.
        val page = json.decodeFromString<FeedPageDto>("""{"memos":[]}""").toDomain()

        assertNull(page.nextCursor)
        assertTrue(page.isLastPage)
    }

    @Test
    fun `unknown fields are tolerated`() {
        // The backend is design-stage; it will grow fields before Android
        // knows about them, and a client that crashes on one is a client that
        // cannot be deployed independently.
        val body = """
            {"memos":[],"next_cursor":null,"server_version":"9.9","experiment":{"x":1}}
        """.trimIndent()

        val page = json.decodeFromString<FeedPageDto>(body).toDomain()

        assertTrue(page.isLastPage)
    }

    @Test
    fun `memo defaults to visible when moderation is absent`() {
        val body = """
            {"id":"m","author_username":"a","audio_url":"u","duration_ms":1,"posted_at":2}
        """.trimIndent()

        val memo = json.decodeFromString<MemoDto>(body).toDomain()

        assertEquals(ModerationState.VISIBLE, memo.moderation.state)
        assertEquals(AppealState.NOT_APPLICABLE, memo.moderation.appeal)
        assertFalse(memo.likedByMe)
    }

    @Test
    fun `removed memo carries its reason and appeal state`() {
        val body = """
            {
              "id":"m","author_username":"you","audio_url":"u","duration_ms":1,"posted_at":2,
              "moderation":{"state":"removed","reason":"Broke the rules.","appeal":"available"}
            }
        """.trimIndent()

        val memo = json.decodeFromString<MemoDto>(body).toDomain()

        assertEquals(ModerationState.REMOVED, memo.moderation.state)
        assertEquals("Broke the rules.", memo.moderation.reason)
        assertEquals(AppealState.AVAILABLE, memo.moderation.appeal)
    }

    @Test
    fun `unrecognised moderation values fall back to visible rather than crashing`() {
        val body = """
            {"id":"m","author_username":"a","audio_url":"u","duration_ms":1,"posted_at":2,
             "moderation":{"state":"quarantined","appeal":"pending_review"}}
        """.trimIndent()

        val memo = json.decodeFromString<MemoDto>(body).toDomain()

        assertEquals(ModerationState.VISIBLE, memo.moderation.state)
        assertEquals(AppealState.NOT_APPLICABLE, memo.moderation.appeal)
    }
}
