package org.teww.tew.core.playback

import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/**
 * Makes headset and lock-screen transport buttons mean what they should in a
 * feed of memos.
 *
 * A `MediaSession` forwards media buttons straight to the player, and the
 * player only ever holds the one memo that is playing — so "next" would seek
 * nowhere and the button would appear dead. This intercepts the transport
 * commands and turns them into [FeedCommand]s the feed screen acts on:
 *
 * | Button | Means |
 * | --- | --- |
 * | Next | Skip this memo |
 * | Previous | Play this memo again |
 * | Play / pause | Play / pause |
 *
 * "Previous" is deliberately *replay*, not "go back a memo". The feed has no
 * backwards — memos are skipped or liked and then gone, and inventing a
 * history to step through would quietly turn an ending stream into a
 * browsable one (non-negotiable #3). Replay is the thing a listener actually
 * wants from that button: they missed what was said.
 *
 * The commands are forced available because the underlying player would
 * otherwise report them as unavailable with a single item queued, and the
 * system hides buttons it is told do nothing.
 */
@OptIn(UnstableApi::class)
internal class FeedForwardingPlayer(
    player: Player,
    private val bus: FeedCommandBus,
) : ForwardingPlayer(player) {

    override fun seekToNext() {
        bus.dispatch(FeedCommand.SKIP)
    }

    override fun seekToNextMediaItem() {
        bus.dispatch(FeedCommand.SKIP)
    }

    override fun seekToPrevious() {
        bus.dispatch(FeedCommand.REPLAY)
    }

    override fun seekToPreviousMediaItem() {
        bus.dispatch(FeedCommand.REPLAY)
    }

    override fun hasNextMediaItem(): Boolean = true

    override fun hasPreviousMediaItem(): Boolean = true

    override fun getAvailableCommands(): Player.Commands =
        super.getAvailableCommands()
            .buildUpon()
            .addAll(
                Player.COMMAND_SEEK_TO_NEXT,
                Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                Player.COMMAND_SEEK_TO_PREVIOUS,
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            )
            .build()

    override fun isCommandAvailable(command: Int): Boolean = when (command) {
        Player.COMMAND_SEEK_TO_NEXT,
        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
        Player.COMMAND_SEEK_TO_PREVIOUS,
        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        -> true

        else -> super.isCommandAvailable(command)
    }
}
