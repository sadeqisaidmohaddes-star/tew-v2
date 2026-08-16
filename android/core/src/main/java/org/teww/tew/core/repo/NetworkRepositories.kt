package org.teww.tew.core.repo

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.teww.tew.core.FailureKind
import org.teww.tew.core.TewResult
import org.teww.tew.core.model.Comment
import org.teww.tew.core.model.FeedPage
import org.teww.tew.core.model.Memo
import org.teww.tew.core.model.ReportReason
import org.teww.tew.core.net.AppealRequest
import org.teww.tew.core.net.ReportRequest
import org.teww.tew.core.net.TewApi
import org.teww.tew.core.net.toDomain
import retrofit2.Response
import java.io.File
import java.io.IOException

/**
 * Turns a Retrofit call into a [TewResult], including the sentence the UI will
 * say if it fails.
 *
 * Every branch produces plain second-person language with no status codes in
 * it. That is not politeness — a screen-reader user hears these, and "HTTP 503"
 * spoken aloud tells them nothing about whether to wait, retry or sign in.
 */
internal suspend fun <T, R> apiCall(
    dispatcher: CoroutineDispatcher,
    call: suspend () -> Response<T>,
    transform: (T) -> R,
): TewResult<R> = withContext(dispatcher) {
    try {
        val response = call()
        val body = response.body()
        when {
            response.isSuccessful && body != null -> TewResult.Ok(transform(body))

            response.isSuccessful -> TewResult.Failure(
                kind = FailureKind.SERVER,
                spoken = "Something went wrong on our side. Please try again.",
                detail = "Empty body on ${response.code()}",
            )

            response.code() == 401 || response.code() == 403 -> TewResult.Failure(
                kind = FailureKind.AUTH,
                spoken = "You have been signed out. Sign in again to carry on.",
                detail = "HTTP ${response.code()}",
            )

            response.code() == 404 -> TewResult.Failure(
                kind = FailureKind.NOT_FOUND,
                spoken = "That memo is no longer there.",
                detail = "HTTP 404",
            )

            else -> TewResult.Failure(
                kind = FailureKind.SERVER,
                spoken = "Something went wrong on our side. Please try again.",
                detail = "HTTP ${response.code()}",
            )
        }
    } catch (e: IOException) {
        TewResult.Failure(
            kind = FailureKind.NETWORK,
            spoken = "You seem to be offline. Check your connection and try again.",
            detail = e.message,
        )
    } catch (e: Exception) {
        TewResult.Failure(
            kind = FailureKind.UNKNOWN,
            spoken = "Something went wrong. Please try again.",
            detail = e.message,
        )
    }
}

/** A response with no body of interest — like, skip, report, appeal. */
internal suspend fun apiUnit(
    dispatcher: CoroutineDispatcher,
    call: suspend () -> Response<Unit>,
): TewResult<Unit> = apiCall(dispatcher, call) { }

private fun File.asAudioPart(fieldName: String): MultipartBody.Part =
    MultipartBody.Part.createFormData(
        fieldName,
        name,
        asRequestBody("audio/mp4".toMediaType()),
    )

/**
 * Turn the API's `/v1/audio/...` path into something Media3 can fetch.
 *
 * The server sends a path rather than an absolute URL because behind a reverse
 * proxy it does not reliably know its own public hostname. Resolving happens
 * here, once, rather than in each screen.
 */
internal fun resolveAudio(baseUrl: String, path: String): String = when {
    // Anything already carrying a scheme is left alone. Not just http(s):
    // the composer plays back `file://` URIs for a memo the app just recorded
    // and has not posted, and prefixing those would break playback preview.
    ABSOLUTE_URI.matches(path) -> path
    else -> baseUrl.trimEnd('/') + "/" + path.trimStart('/')
}

/** scheme://... — RFC 3986 scheme characters. */
private val ABSOLUTE_URI = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")

class NetworkFeedRepository(
    private val api: TewApi,
    private val baseUrl: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FeedRepository {

    override suspend fun feed(cursor: String?): TewResult<FeedPage> =
        apiCall(dispatcher, { api.feed(cursor) }) { page ->
            page.toDomain().let { domain ->
                domain.copy(memos = domain.memos.map { it.withResolvedAudio(baseUrl) })
            }
        }

    override suspend fun setLiked(memoId: String, liked: Boolean): TewResult<Unit> =
        apiUnit(dispatcher) { if (liked) api.like(memoId) else api.unlike(memoId) }

    override suspend fun skip(memoId: String): TewResult<Unit> =
        apiUnit(dispatcher) { api.skip(memoId) }

    override suspend fun comments(memoId: String): TewResult<List<Comment>> =
        apiCall(dispatcher, { api.comments(memoId) }) { dto ->
            dto.comments.map { it.toDomain().copy() }
                .map { it.copy(audioUrl = resolveAudio(baseUrl, it.audioUrl)) }
        }

    override suspend fun postComment(memoId: String, audio: File): TewResult<Comment> =
        apiCall(dispatcher, { api.postComment(memoId, audio.asAudioPart("audio")) } ) {
            it.toDomain().let { c -> c.copy(audioUrl = resolveAudio(baseUrl, c.audioUrl)) }
        }

    override suspend fun postMemo(audio: File): TewResult<Memo> =
        apiCall(dispatcher, { api.postMemo(audio.asAudioPart("audio")) }) {
            it.toDomain().withResolvedAudio(baseUrl)
        }
}

private fun Memo.withResolvedAudio(baseUrl: String): Memo =
    copy(audioUrl = resolveAudio(baseUrl, audioUrl))

class NetworkModerationRepository(
    private val api: TewApi,
    private val baseUrl: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ModerationRepository {

    override suspend fun report(memoId: String, reason: ReportReason): TewResult<Unit> =
        apiUnit(dispatcher) { api.report(memoId, ReportRequest(reason.wireValue())) }

    override suspend fun myMemos(): TewResult<List<Memo>> =
        apiCall(dispatcher, { api.myMemos() }) { dto ->
            dto.memos.map { it.toDomain().withResolvedAudio(baseUrl) }
        }

    override suspend fun appeal(memoId: String, text: String): TewResult<Unit> =
        apiUnit(dispatcher) { api.appeal(memoId, AppealRequest(text)) }

    override suspend fun deleteMemo(memoId: String): TewResult<Unit> =
        apiUnit(dispatcher) { api.deleteMemo(memoId) }
}

internal fun ReportReason.wireValue(): String = when (this) {
    ReportReason.HARASSMENT -> "harassment"
    ReportReason.HATE_SPEECH -> "hate_speech"
    ReportReason.SEXUAL_CONTENT -> "sexual_content"
    ReportReason.SPAM -> "spam"
    ReportReason.SOMETHING_ELSE -> "other"
}
