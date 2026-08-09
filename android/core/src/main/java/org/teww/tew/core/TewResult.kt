package org.teww.tew.core

/**
 * Result type for everything in `:core` that can fail.
 *
 * Kotlin's own `Result` carries a `Throwable`, and a stack trace is no use to
 * a screen that has to *say* what went wrong. Every failure here carries a
 * [Failure.spoken] string written to be read aloud — because non-negotiable #1
 * means an error the user cannot hear is an error that did not happen as far
 * as they are concerned.
 */
sealed interface TewResult<out T> {

    data class Ok<T>(val value: T) : TewResult<T>

    data class Failure(
        val kind: FailureKind,
        /** Plain language, second person, no jargon and no error codes. */
        val spoken: String,
        /** Developer-facing detail. Never shown or spoken to a user. */
        val detail: String? = null,
    ) : TewResult<Nothing>
}

enum class FailureKind {
    /** Offline, timed out, DNS — anything where retrying might work. */
    NETWORK,

    /** Signed out, or the token was rejected. The user must sign in again. */
    AUTH,

    /** Server said no in a way the client cannot fix. */
    SERVER,

    /** The thing being asked for is not there. */
    NOT_FOUND,

    /** Something the client got wrong — a bug, not a user problem. */
    UNKNOWN,
}

inline fun <T, R> TewResult<T>.map(transform: (T) -> R): TewResult<R> = when (this) {
    is TewResult.Ok -> TewResult.Ok(transform(value))
    is TewResult.Failure -> this
}

/** The value, or null on failure — for callers that only need the happy path. */
fun <T> TewResult<T>.valueOrNull(): T? = (this as? TewResult.Ok)?.value

/**
 * Timing rules from IMPLEMENTATION.md, in one place so both feed screens use
 * the same numbers rather than each inventing their own.
 */
object TewTiming {
    /** Gesture or tap to audible feedback. Hard rule, not a target. */
    const val FEEDBACK_BUDGET_MS: Long = 100

    /**
     * How long a screen may wait on the network before it must make some
     * sound. Silence past this point reads as a frozen app to someone who
     * cannot see a spinner.
     */
    const val SILENT_WAIT_LIMIT_MS: Long = 500

    /** Cold start to first playable memo, on a throttled connection. */
    const val COLD_START_BUDGET_MS: Long = 3_000
}
