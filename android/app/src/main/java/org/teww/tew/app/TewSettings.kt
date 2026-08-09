package org.teww.tew.app

import android.content.Context

/**
 * Where the app is pointed, stored on the device.
 *
 * Runtime rather than a build-time constant, because the people running the
 * internal test have an APK and no toolchain — asking them to rebuild to
 * change a hostname would put the test behind a Gradle install. They type the
 * address once and it persists.
 *
 * Empty means "use the built-in sample data", which is what makes the app
 * usable on a phone with no backend reachable at all.
 */
class TewSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("tew-settings", Context.MODE_PRIVATE)

    /** Base URL of the API, or empty for offline sample data. */
    var serverUrl: String
        get() = prefs.getString(KEY_SERVER, "").orEmpty()
        set(value) {
            prefs.edit().putString(KEY_SERVER, normaliseServerUrl(value)).apply()
        }

    val usingServer: Boolean get() = serverUrl.isNotEmpty()

    private companion object {
        const val KEY_SERVER = "server-url"
    }
}

/**
 * Tidy what someone typed into something Retrofit accepts.
 *
 * Retrofit requires a trailing slash and will throw at construction without
 * one — a crash on startup because of a missing character is a poor way to
 * tell someone they mistyped an address. A bare host gets `https://` rather
 * than `http://`: these are voice recordings, and defaulting to plaintext
 * because the user omitted a scheme would be the wrong default to guess.
 *
 * Returns empty for blank input, which the app reads as "use sample data".
 */
fun normaliseServerUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""

    val withScheme = when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        else -> "https://$trimmed"
    }

    return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
}
