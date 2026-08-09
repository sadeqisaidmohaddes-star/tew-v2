package org.teww.tew.core.net

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.teww.tew.core.auth.AuthSession
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Where the API lives, and how chatty the client should be. */
data class TewApiConfig(
    /** Must end in a slash — Retrofit requires it. */
    val baseUrl: String,
    val logRequests: Boolean = false,
)

/**
 * Builds the [TewApi].
 *
 * Timeouts are set against the device this app is actually for: a three-year-old
 * budget phone on throttled 3G. They are deliberately finite and fairly short —
 * a request that hangs for a minute is worse than one that fails in ten
 * seconds, because the screen has nothing to say in the meantime and silence
 * is what breaks a voice-first interface (non-negotiable #1). Callers are
 * expected to speak a loading state well before these fire; see
 * [org.teww.tew.core.TewTiming.SILENT_WAIT_LIMIT_MS].
 */
object TewApiFactory {

    fun create(config: TewApiConfig, authSession: AuthSession): TewApi {
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(AuthInterceptor(authSession))

        if (config.logRequests) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    // BASIC, not BODY. Non-negotiable #7 treats voice as
                    // biometric data, and BODY would put memo audio and
                    // transcripts into logcat where anything can read them.
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
        }

        return Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(builder.build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TewApi::class.java)
    }

    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val READ_TIMEOUT_SECONDS = 20L
    private const val WRITE_TIMEOUT_SECONDS = 30L
}

/**
 * Attaches the bearer token, when there is one.
 *
 * OkHttp interceptors are synchronous and [AuthSession.bearerToken] is a
 * suspend function, so this bridges with [runBlocking]. That is safe here
 * because interceptors already run on OkHttp's own background threads, never
 * on the main thread — but it is the reason [AuthSession.bearerToken] must
 * stay cheap and must not itself perform a network call.
 */
internal class AuthInterceptor(
    private val authSession: AuthSession,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { authSession.bearerToken() }
        val request = chain.request()
        val authorized = if (token == null) {
            request
        } else {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(authorized)
    }
}
