package org.teww.tew.feature.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.teww.tew.core.auth.AuthState
import org.teww.tew.core.auth.AuthUser
import org.teww.tew.core.model.AppealState
import org.teww.tew.core.model.Memo
import org.teww.tew.core.model.Moderation
import org.teww.tew.core.model.ModerationState

/**
 * Non-negotiable #8 says moderation is visible and appealable. Whether that
 * holds is decided entirely by what these functions say, so the wording is
 * tested like any other behaviour.
 */
class ModerationCopyTest {

    private fun memo(moderation: Moderation) = Memo(
        id = "m", authorUsername = "you", audioUrl = "u", durationMs = 1,
        postedAtEpochSeconds = 0, transcript = null, likedByMe = false,
        moderation = moderation,
    )

    @Test
    fun `a removed memo always states a reason`() {
        val status = ownMemoStatus(
            memo(Moderation(ModerationState.REMOVED, "It targeted someone.", AppealState.AVAILABLE)),
        )

        assertTrue(status.contains("Taken down"))
        assertTrue(status.contains("It targeted someone."))
    }

    @Test
    fun `a removal with no reason recorded says so rather than staying silent`() {
        // A blank reason is a backend bug, but the user still has to be told
        // something actionable — silence here is the failure mode #8 exists
        // to prevent.
        val status = ownMemoStatus(
            memo(Moderation(ModerationState.REMOVED, null, AppealState.AVAILABLE)),
        )

        assertTrue(status.contains("No reason was recorded"))
        assertTrue(status.contains("appeal"))
    }

    @Test
    fun `a removed memo tells the author they can appeal`() {
        val status = ownMemoStatus(
            memo(Moderation(ModerationState.REMOVED, "Reason.", AppealState.AVAILABLE)),
        )

        assertTrue(status.contains("looked at again"))
    }

    @Test
    fun `appeal is offered only when it is available`() {
        assertTrue(canAppeal(memo(Moderation(ModerationState.REMOVED, "r", AppealState.AVAILABLE))))
        assertFalse(canAppeal(memo(Moderation(ModerationState.REMOVED, "r", AppealState.SUBMITTED))))
        assertFalse(canAppeal(memo(Moderation(ModerationState.REMOVED, "r", AppealState.DENIED))))
        assertFalse(canAppeal(memo(Moderation.visible)))
    }

    @Test
    fun `under review is explained, not just labelled`() {
        val status = ownMemoStatus(memo(Moderation(ModerationState.UNDER_REVIEW, null, AppealState.NOT_APPLICABLE)))

        assertTrue(status.contains("hidden until"))
    }

    @Test
    fun `every appeal state has words except the one that should be silent`() {
        assertEquals("", appealStatus(AppealState.NOT_APPLICABLE))
        AppealState.entries.filter { it != AppealState.NOT_APPLICABLE }.forEach {
            assertTrue("$it has no spoken text", appealStatus(it).isNotBlank())
        }
    }
}

class SignInStatusTest {

    @Test
    fun `every auth state says something`() {
        assertTrue(signInStatusText(AuthState.SignedOut).isNotBlank())
        assertTrue(signInStatusText(AuthState.SigningIn).contains("Signing you in"))
        assertTrue(
            signInStatusText(AuthState.SignedIn(AuthUser("id", "amina"))).contains("amina"),
        )
    }
}

class MemoCountTest {

    private fun memos(n: Int) = List(n) {
        Memo("m$it", "you", "u", 1, 0, null, false, Moderation.visible)
    }

    @Test
    fun `an empty list is announced rather than left silent`() {
        // Silence and a failed load are indistinguishable without sight.
        assertTrue(describeMemoCount(emptyList()).contains("not posted any"))
    }

    @Test
    fun `one memo is singular`() {
        assertTrue(describeMemoCount(memos(1)).contains("one memo"))
    }

    @Test
    fun `several memos are counted`() {
        assertTrue(describeMemoCount(memos(4)).contains("4 memos"))
    }
}
