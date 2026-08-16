package org.teww.tew.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * The only activity. `IMPLEMENTATION.md` describes `:app` as a thin shell —
 * navigation and wiring live in [TewApp] and [TewContainer], and nothing that
 * belongs to a feature belongs here.
 */
class MainActivity : ComponentActivity() {

    private lateinit var container: TewContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = TewContainer(this)
        setContent { TewApp(container) }
    }

    override fun onDestroy() {
        // The player holds an audio focus request and a codec; leaking it
        // means a memo can keep playing after the app is gone, which in an
        // audio-only app is a conspicuous failure rather than a quiet leak.
        container.release()
        super.onDestroy()
    }
}
