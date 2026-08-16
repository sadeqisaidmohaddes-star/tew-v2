/**
 * Who is calling.
 *
 * ## Why this is an interface with a stub behind it
 *
 * `STATE.md` defers the Firebase project — no account has been chosen — and
 * the backend would otherwise be blocked behind that decision exactly as the
 * Android client would have been. The Android side solved it the same way
 * (`AuthSession` + `StubAuthSession`), and mirroring the shape here keeps both
 * sides swappable in one file each.
 *
 * When the Firebase project exists, `FirebaseTokenVerifier` implements this
 * using the Admin SDK's `verifyIdToken`, and nothing else in the backend
 * changes.
 */

export interface AuthedUser {
  id: string;
  username: string;
}

export interface TokenVerifier {
  /**
   * Verify a bearer token. Returns null when it is missing, malformed, expired
   * or forged — the route layer turns that into a 401 without needing to know
   * which.
   */
  verify(token: string | null): Promise<AuthedUser | null>;
}

/**
 * Development stand-in. **Accepts any non-empty token and trusts its contents.**
 *
 * This is not a security boundary. It exists so the API can be built, run and
 * tested before a Firebase project exists, and it must never be reachable in
 * production — `createVerifier` refuses to return it when `NODE_ENV` is
 * `production`, which is a guard rather than a suggestion.
 *
 * The token format it accepts is `stub:<userId>:<username>`, so tests and a
 * locally-run Android client can act as different people without a real
 * identity provider.
 */
export class StubTokenVerifier implements TokenVerifier {
  async verify(token: string | null): Promise<AuthedUser | null> {
    if (!token) return null;

    const parts = token.split(':');
    if (parts[0] !== 'stub') {
      // The Android client's StubAuthSession sends a fixed placeholder rather
      // than this format. Accept it as a single known development user so the
      // two stubs interoperate without either pretending to be real auth.
      return { id: 'stub-user', username: 'test-user' };
    }

    const id = parts[1];
    const username = parts[2];
    if (!id || !username) return null;

    return { id, username };
  }
}

/**
 * Pick a verifier for the current environment.
 *
 * Throws in production rather than falling back. A backend that silently
 * accepts any token because a credential was missing is worse than one that
 * refuses to start, and non-negotiable #7 — voice is biometric data — makes
 * "who is this really" a question this service cannot get wrong quietly.
 */
export function createVerifier(env: NodeJS.ProcessEnv): TokenVerifier {
  const hasFirebase = Boolean(env.FIREBASE_PROJECT_ID && env.FIREBASE_CLIENT_EMAIL);

  if (hasFirebase) {
    throw new Error(
      'Firebase credentials are set but FirebaseTokenVerifier is not implemented yet. ' +
        'See backend/README.md — implement it against this interface before deploying.',
    );
  }

  if (env.NODE_ENV === 'production') {
    throw new Error(
      'Refusing to start in production with the stub token verifier. ' +
        'Set FIREBASE_PROJECT_ID and FIREBASE_CLIENT_EMAIL and implement FirebaseTokenVerifier.',
    );
  }

  return new StubTokenVerifier();
}

/** Pull the bearer token out of an Authorization header. */
export function bearerToken(header: string | undefined): string | null {
  if (!header) return null;
  const match = /^Bearer\s+(.+)$/i.exec(header.trim());
  return match?.[1] ?? null;
}
