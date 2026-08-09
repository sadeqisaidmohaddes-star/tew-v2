package org.teww.tew.core.playback

/**
 * The actions a feed screen can be asked to perform, from any route.
 *
 * Non-negotiable #5 requires three routes to every action: media controls,
 * voice, and the screen-reader menu. This is the funnel they all arrive
 * through — one enum, so a route added later cannot quietly support a
 * different set of actions from the others, and so the feed view models need
 * to know nothing about where a command came from.
 */
enum class FeedCommand {
    PLAY_PAUSE,
    LIKE,
    SKIP,
    REPLAY,
    REPLY,
    REPORT,
}

/**
 * Where feed screens register to receive commands from routes that live
 * outside Compose — media buttons and voice.
 *
 * Deliberately a single mutable handler rather than a broadcast: only one feed
 * screen is on screen at a time, and delivering a "like" to a screen that is
 * not visible is how a moderator toggle turns into a data bug during a
 * usability session.
 */
class FeedCommandBus {

    private var handler: ((FeedCommand) -> Unit)? = null

    /** Called by whichever feed screen is currently active. */
    fun setHandler(handler: ((FeedCommand) -> Unit)?) {
        this.handler = handler
    }

    /** Returns true if something was listening — false means the command was dropped. */
    fun dispatch(command: FeedCommand): Boolean {
        val target = handler ?: return false
        target(command)
        return true
    }
}
