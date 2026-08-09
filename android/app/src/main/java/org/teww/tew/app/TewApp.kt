package org.teww.tew.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.teww.tew.core.auth.AuthState
import org.teww.tew.core.model.Memo
import org.teww.tew.feature.account.AccountViewModel
import org.teww.tew.feature.account.AppealScreen
import org.teww.tew.feature.account.ProfileScreen
import org.teww.tew.feature.account.ReportScreen
import org.teww.tew.feature.account.SignInScreen
import org.teww.tew.feature.carddeck.CardDeckScreen
import org.teww.tew.feature.carddeck.CardDeckViewModel
import org.teww.tew.feature.carddeck.Narrator
import org.teww.tew.feature.radio.RadioScreen
import org.teww.tew.feature.radio.RadioViewModel

/** Which feed model the tester is currently on. */
enum class FeedModel { RADIO, CARD_DECK }

private sealed interface Destination {
    data object Feed : Destination
    data object Profile : Destination
    data class Report(val memoId: String) : Destination
    data class Appeal(val memo: Memo) : Destination
}

/**
 * The whole app.
 *
 * A `when` over a sealed type rather than a navigation library. There are four
 * destinations, no deep links and no back-stack requirements beyond "go back
 * one", and a nav graph would cost more to read than it saves. Revisit when
 * there is a fifth destination or a real deep link.
 */
@Composable
fun TewApp(container: TewContainer) {
    val authState by container.authSession.state.collectAsStateWithLifecycle()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (authState) {
                is AuthState.SignedIn -> SignedIn(container)
                else -> SignInScreen(viewModel = accountViewModel(container))
            }
        }
    }
}

@Composable
private fun SignedIn(container: TewContainer) {
    var feedModel by remember { mutableStateOf(FeedModel.RADIO) }
    var destination by remember { mutableStateOf<Destination>(Destination.Feed) }
    var reportStatus by remember { mutableStateOf("") }

    val account = accountViewModel(container)

    Column(modifier = Modifier.fillMaxSize()) {
        ModeratorBar(
            feedModel = feedModel,
            onToggle = {
                feedModel = if (feedModel == FeedModel.RADIO) FeedModel.CARD_DECK else FeedModel.RADIO
                destination = Destination.Feed
            },
            onProfile = { destination = Destination.Profile },
            onFeed = { destination = Destination.Feed },
        )

        when (val current = destination) {
            is Destination.Feed -> when (feedModel) {
                FeedModel.RADIO -> RadioScreen(
                    viewModel = radioViewModel(container),
                    onComment = { /* recording UI is not in this build — see STATE.md */ },
                    onReport = { destination = Destination.Report(it) },
                    commandBus = container.commandBus,
                    voice = container.voiceCommandListener,
                )

                FeedModel.CARD_DECK -> {
                    val context = LocalContext.current
                    val narrator = remember(context) { Narrator(context) }
                    CardDeckScreen(
                        viewModel = cardDeckViewModel(container),
                        narrator = narrator,
                        onComment = { /* recording UI is not in this build */ },
                        onReport = { destination = Destination.Report(it) },
                        commandBus = container.commandBus,
                        voice = container.voiceCommandListener,
                    )
                }
            }

            is Destination.Profile -> ProfileScreen(
                viewModel = account,
                onAppeal = { destination = Destination.Appeal(it) },
            )

            is Destination.Report -> ReportScreen(
                memo = Memo(
                    id = current.memoId,
                    authorUsername = "this memo's author",
                    audioUrl = "",
                    durationMs = 0,
                    postedAtEpochSeconds = 0,
                    transcript = null,
                    likedByMe = false,
                    moderation = org.teww.tew.core.model.Moderation.visible,
                ),
                onReport = { reason ->
                    account.report(current.memoId, reason) { spoken ->
                        reportStatus = spoken
                        destination = Destination.Feed
                    }
                },
                onCancel = { destination = Destination.Feed },
                status = reportStatus,
            )

            is Destination.Appeal -> AppealScreen(
                memo = current.memo,
                onSubmit = {
                    account.appeal(current.memo.id, it)
                    destination = Destination.Profile
                },
                onCancel = { destination = Destination.Profile },
                status = account.uiState.collectAsStateWithLifecycle().value.announcement,
            )
        }
    }
}

/**
 * The moderator-only strip.
 *
 * `IMPLEMENTATION.md` asks for a toggle so a tester can try both feed models
 * without separate installs — that is what makes a same-session, same-tester
 * comparison possible, which the old project's status report named as the
 * actual blocker to deciding between them.
 *
 * It is plainly visible rather than hidden behind a secret gesture, because a
 * hidden control is one a blind moderator cannot find. Gating it properly is
 * work for when there are real accounts to gate against; until then a visible
 * strip is honest about what it is.
 */
@Composable
private fun ModeratorBar(
    feedModel: FeedModel,
    onToggle: () -> Unit,
    onProfile: () -> Unit,
    onFeed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "Moderator controls. Currently showing: ${feedModelName(feedModel)}.",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextButton(onClick = onToggle) {
                Text("Switch to ${feedModelName(otherModel(feedModel))}")
            }
            TextButton(onClick = onFeed) { Text("Feed") }
            TextButton(onClick = onProfile) { Text("You") }
        }
    }
}

internal fun feedModelName(model: FeedModel): String = when (model) {
    FeedModel.RADIO -> "the radio timeline"
    FeedModel.CARD_DECK -> "the card deck"
}

internal fun otherModel(model: FeedModel): FeedModel = when (model) {
    FeedModel.RADIO -> FeedModel.CARD_DECK
    FeedModel.CARD_DECK -> FeedModel.RADIO
}

// ---- View model construction ------------------------------------------
// Hand-rolled factories: these view models take constructor dependencies and
// there is no DI framework, so each screen gets one of these rather than a
// generated factory.

@Composable
private fun accountViewModel(container: TewContainer): AccountViewModel =
    viewModel(key = "account") {
        AccountViewModel(container.authSession, container.moderationRepository)
    }

@Composable
private fun radioViewModel(container: TewContainer): RadioViewModel =
    viewModel(key = "radio") {
        RadioViewModel(container.feedRepository, container.playbackSession)
    }

@Composable
private fun cardDeckViewModel(container: TewContainer): CardDeckViewModel =
    viewModel(key = "carddeck") {
        CardDeckViewModel(container.feedRepository, container.playbackSession)
    }
