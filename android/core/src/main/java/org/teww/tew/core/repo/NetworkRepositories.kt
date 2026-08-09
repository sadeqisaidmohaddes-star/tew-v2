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

class NetworkFeedRepository(
    private val api: TewApi,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FeedRepository {

    override suspend fun feed(cursor: String?): TewResult<FeedPage> =
        apiCall(dispatcher, { api.feed(cursor) }) { it.toDomain() }

    override suspend fun setLiked(memoId: String, liked: Boolean): TewResult<Unit> =
        apiUnit(dispatcher) { if (liked) api.like(memoId) else api.unlike(memoId) }

    override suspend fun skip(memoId: String): TewResult<Unit> =
        apiUnit(dispatcher) { api.skip(memoId) }

    override suspend fun comments(memoId: String): TewResult<List<Comment>> =
        apiCall(dispatcher, { api.comments(memoId) }) { dto ->
            dto.comments.map { it.toDomain() }
        }

    override suspend fun postComment(memoId: String, audio: File): TewResult<Comment> =
        apiCall(dispatcher, { api.postComment(memoId, audio.asAudioPart("audio")) }) {
            it.toDomain()
        }

    override suspend fun postMemo(audio: File): TewResult<Memo> =
        apiCall(dispatcher, { api.postMemo(audio.asAudioPart("audio")) }) { it.toDomain() }
}

class NetworkModerationRepository(
    private val api: TewApi,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ModerationRepository {

    override suspend fun report(memoId: String, reason: ReportReason): TewResult<Unit> =
        apiUnit(dispatcher) { api.report(memoId, ReportRequest(reason.wireValue())) }

    override suspend fun myMemos(): TewResult<List<Memo>> =
        apiCall(dispatcher, { api.myMemos() }) { dto -> dto.memos.map { it.toDomain() } }

    override suspend fun appeal(memoId: String, text: String): TewResult<Unit> =
        apiUnit(dispatcher) { api.appeal(memoId, AppealRequest(text)) }
}

internal fun ReportReason.wireValue(): String = when (this) {
    ReportReason.HARASSMENT -> "harassment"
    ReportReason.HATE_SPEECH -> "hate_speech"
    ReportReason.SEXUAL_CONTENT -> "sexual_content"
    ReportReason.SPAM -> "spam"
    ReportReason.SOMETHING_ELSE -> "other"
}
