package org.teww.tew.core.auth

import kotlinx.coroutines.flow.StateFlow
import org.teww.tew.core.TewResult

/** The signed-in person. Minimal by design — TEW has no profile to speak of. */
data class AuthUser(
    val id: String,
    val username: String,
)

sealed interface AuthState {
    data object SignedOut : AuthState
    data object SigningIn : AuthState
    data class SignedIn(val user: AuthUser) : AuthState
}

/**
 * The app's view of who is signed in.
 *
 * Deliberately an interface with no Firebase types anywhere in its signature.
 * `STATE.md` defers the Firebase project — no account has been chosen — and the
 * whole app would otherwise be blocked behind that decision. Everything above
 * this interface is written against it, so swapping [StubAuthSession] for a
 * real Firebase implementation is one new file and one wiring change in `:app`,
 * with no feature-module edits at all.
 *
 * Non-negotiable #7 is why the shape is this narrow: voice is biometric data,
 * and an auth layer that hands identity tokens around casually is how that
 * stops being taken seriously. Only [bearerToken] leaves this interface, and
 * only the network layer calls it.
 */
interface AuthSession {

    val state: StateFlow<AuthState>

    /** Currently signed-in user, or null. Convenience over reading [state]. */
    val currentUser: AuthUser?

    suspend fun signIn(): TewResult<AuthUser>

    suspend fun signOut()

    /**
     * Token for the Authorization header, or null when signed out.
     * Called by the network layer only.
     */
    suspend fun bearerToken(): String?
}
