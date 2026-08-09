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
 * `backend/README.md` is design-stage — the API does not exist yet. Without
 * these the app would build and install and then show nothing, which makes the
 * on-device accessibility testing that this project actually depends on
 * impossible. These let both feed screens run, play real audio, and be tested
 * with TalkBack today.
 *
 * **Not a cache, not an offline mode, and not for release.** Swap for
 * [NetworkFeedRepository] / [NetworkModerationRepository] in `:app` the day the
 * backend answers.
 *
 * The [latencyMs] delay is deliberate. The target device is a budget phone on
 * throttled 3G, and IMPLEMENTATION.md requires a spoken loading state past
 * 500ms of silence. A fake that returns instantly would let the app be built
 * and shipped without that path ever running once.
 */
class InMemoryFeedRepository(
    private val latencyMs: Long = 700,
    seed: List<Memo> = sampleMemos(),
) : FeedRepository {

    private val memos = seed.toMutableList()
    private val comments = mutableMapOf<String, MutableList<Comment>>()
    private val skipped = mutableSetOf<String>()
    private var postedCount = 0

    override suspend fun feed(cursor: String?): TewResult<FeedPage> {
        delay(latencyMs)
        val available = memos.filter { it.id !in skipped && it.moderation.state == ModerationState.VISIBLE }
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
            postedAtEpochSeconds = 0,
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
            postedAtEpochSeconds = 0,
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
 * Sample audio uses short public-domain clips so the app makes real sound on a
 * device. Silent placeholders would defeat the point — this app is audio, and
 * a feed that plays nothing cannot be accessibility-tested.
 */
fun sampleMemos(): List<Memo> = listOf(
    sample("m1", "amina", 21_000, "Good morning everyone. I walked to the market on my own today for the first time since I moved here."),
    sample("m2", "joseph", 14_000, "Does anyone else find the bus announcements too quiet? I keep missing my stop."),
    sample("m3", "fatima", 33_000, "My daughter recorded her school song for me. I have listened to it about nine times."),
    sample("m4", "daniel", 18_000, "Question for the group. What is your trick for telling shampoo and conditioner apart?"),
    sample("m5", "grace", 26_000, "I just wanted to say that hearing other people's voices in the evening helps more than I expected."),
    sample("m6", "samuel", 12_000, "Rain on a tin roof. That is the whole memo. Enjoy."),
)

fun sampleOwnMemos(): List<Memo> = listOf(
    sample("own1", "you", 19_000, "Testing this out. Hello from my kitchen."),
    Memo(
        id = "own2",
        authorUsername = "you",
        audioUrl = SAMPLE_AUDIO,
        durationMs = 9_000,
        postedAtEpochSeconds = 0,
        transcript = "This one was taken down so the appeal flow has something to show.",
        likedByMe = false,
        moderation = Moderation(
            state = ModerationState.REMOVED,
            reason = "A listener reported this memo for harassment, and a moderator agreed it broke the community rules.",
            appeal = AppealState.AVAILABLE,
        ),
    ),
)

private fun sample(id: String, author: String, durationMs: Long, transcript: String) = Memo(
    id = id,
    authorUsername = author,
    audioUrl = SAMPLE_AUDIO,
    durationMs = durationMs,
    postedAtEpochSeconds = 0,
    transcript = transcript,
    likedByMe = false,
    moderation = Moderation.visible,
)

/**
 * A short public-domain bell from Wikimedia Commons. Placeholder until the
 * backend serves real memos; replaced, not kept, when it does.
 */
private const val SAMPLE_AUDIO =
    "https://upload.wikimedia.org/wikipedia/commons/8/85/Holy_Trinity_Church%2C_Ventnor%2C_bell.ogg"
