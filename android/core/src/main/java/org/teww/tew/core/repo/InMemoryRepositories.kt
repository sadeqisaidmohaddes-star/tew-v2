package org.teww.tew.core.repo

import kotlinx.coroutines.delay
import org.teww.tew.core.TewResult
import org.teww.tew.core.model.AppealState
import org.teww.tew.core.model.Comment
import org.teww.tew.core.model.FeedPage
import org.teww.tew.core.model.Memo
import org.teww.tew.core.model.Moderation
import org.teww.tew.core.model.ModerationState
import org.teww.tew.core.model.ReportReason
import java.io.File

/**
 * In-memory stand-ins for the repositories, with no backend behind them.
 *
 * The backend exists now, and `:app` uses [NetworkFeedRepository] whenever a
 * server address is set. These are what it falls back to when the field is
 * empty — so a tester with a phone and no reachable server can still install
 * the app, hear a feed and run it under TalkBack. The audio is bundled in the
 * APK, so this works with the radio off.
 *
 * **Still not a cache.** Nothing here survives the process, likes and skips are
 * forgotten on relaunch, and a memo posted in this mode never leaves the phone.
 * It is a way to test the screens, not a way to use the app.
 *
 * The [latencyMs] delay is deliberate. The target device is a budget phone on
 * throttled 3G, and IMPLEMENTATION.md requires a spoken loading state past
 * 500ms of silence. A fake that returns instantly would let the app be built
 * and shipped without that path ever running once.
 */
class InMemoryFeedRepository(
    private val latencyMs: Long = 700,
    seed: List<Memo> = sampleMemos(),
    seedComments: Map<String, List<Comment>> = sampleComments(),
) : FeedRepository {

    private val memos = seed.toMutableList()
    private val comments = seedComments
        .mapValues { (_, replies) -> replies.toMutableList() }
        .toMutableMap()
    private val skipped = mutableSetOf<String>()
    private var postedCount = 0

    override suspend fun feed(cursor: String?): TewResult<FeedPage> {
        delay(latencyMs)
        // Newest first, matching the ordering decision in STATE.md. The backend
        // sorts server-side; sorting here too means a tester on the built-in
        // memos sees the same feed shape as one on a real server, and a memo
        // recorded during the session appears where they expect it.
        val available = memos
            .filter { it.id !in skipped && it.moderation.state == ModerationState.VISIBLE }
            .sortedByDescending { it.postedAtEpochSeconds }
        val start = cursor?.toIntOrNull() ?: 0
        val end = (start + PAGE).coerceAtMost(available.size)
        if (start >= available.size) {
            return TewResult.Ok(FeedPage(emptyList(), null))
        }
        val page = available.subList(start, end)
        // Null cursor at the end — non-negotiable #3, the stream ends.
        val next = if (end >= available.size) null else end.toString()
        return TewResult.Ok(FeedPage(page, next))
    }

    override suspend fun setLiked(memoId: String, liked: Boolean): TewResult<Unit> {
        delay(FAST_ACTION_MS)
        val i = memos.indexOfFirst { it.id == memoId }
        if (i >= 0) memos[i] = memos[i].copy(likedByMe = liked)
        return TewResult.Ok(Unit)
    }

    override suspend fun skip(memoId: String): TewResult<Unit> {
        delay(FAST_ACTION_MS)
        skipped += memoId
        return TewResult.Ok(Unit)
    }

    override suspend fun comments(memoId: String): TewResult<List<Comment>> {
        delay(latencyMs)
        return TewResult.Ok(comments[memoId].orEmpty().toList())
    }

    override suspend fun postComment(memoId: String, audio: File): TewResult<Comment> {
        delay(latencyMs)
        val comment = Comment(
            id = "local-comment-${++postedCount}",
            memoId = memoId,
            authorUsername = "you",
            audioUrl = audio.toURI().toString(),
            durationMs = 0,
            postedAtEpochSeconds = nowSeconds(),
            transcript = null,
        )
        comments.getOrPut(memoId) { mutableListOf() } += comment
        return TewResult.Ok(comment)
    }

    override suspend fun postMemo(audio: File): TewResult<Memo> {
        delay(latencyMs)
        val memo = Memo(
            id = "local-memo-${++postedCount}",
            authorUsername = "you",
            audioUrl = audio.toURI().toString(),
            durationMs = 0,
            // Now, not zero: the feed is newest-first, and a memo the tester
            // has this second recorded belongs at the top of it rather than
            // silently below every seeded one.
            postedAtEpochSeconds = nowSeconds(),
            transcript = null,
            likedByMe = false,
            moderation = Moderation.visible,
        )
        memos += memo
        return TewResult.Ok(memo)
    }

    private companion object {
        const val PAGE = 5
        const val FAST_ACTION_MS = 120L
    }
}

class InMemoryModerationRepository(
    private val latencyMs: Long = 700,
    private val own: List<Memo> = sampleOwnMemos(),
) : ModerationRepository {

    private val mine = own.toMutableList()
    private val reported = mutableSetOf<String>()

    override suspend fun report(memoId: String, reason: ReportReason): TewResult<Unit> {
        delay(latencyMs)
        reported += memoId
        return TewResult.Ok(Unit)
    }

    override suspend fun myMemos(): TewResult<List<Memo>> {
        delay(latencyMs)
        return TewResult.Ok(mine.toList())
    }

    override suspend fun appeal(memoId: String, text: String): TewResult<Unit> {
        delay(latencyMs)
        val i = mine.indexOfFirst { it.id == memoId }
        if (i >= 0) {
            mine[i] = mine[i].copy(
                moderation = mine[i].moderation.copy(appeal = AppealState.SUBMITTED),
            )
        }
        return TewResult.Ok(Unit)
    }
}

/**
 * Where the seed content's audio lives.
 *
 * `rawresource://` is Media3's scheme for a bundled raw resource. It resolves
 * with no network, no file permissions and no storage, which is what makes the
 * built-in feed usable on a phone that has never reached a server.
 *
 * The memos themselves are in `SeedContent.kt`, generated by
 * `android/tools/generate-seed-audio.sh` alongside the audio it measures.
 */
internal fun rawResourceUri(resId: Int): String = "rawresource:///" + resId

/**
 * Seed timestamps are relative to whenever the app is launched.
 *
 * A fixed epoch would have every built-in memo read as "55 years ago" and get
 * worse each year. Feed order is unaffected either way — the offsets keep their
 * relative spacing — but a tester hearing a plausible time is testing the
 * screen they will actually ship.
 */
internal fun minutesAgo(minutes: Long): Long = nowSeconds() - minutes * 60

private fun nowSeconds(): Long = System.currentTimeMillis() / 1000
