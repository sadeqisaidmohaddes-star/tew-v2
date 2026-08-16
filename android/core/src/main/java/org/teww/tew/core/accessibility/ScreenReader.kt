package org.teww.tew.core.accessibility

import android.content.Context
import android.view.accessibility.AccessibilityManager

/**
 * True while a screen reader is driving the screen.
 *
 * Touch exploration is the signal rather than "any accessibility service is
 * enabled": a switch-access or magnification service is not a second voice,
 * and treating it as one would silence the app for people who need it to
 * speak. Touch exploration is what TalkBack turns on, and TalkBack is speech.
 *
 * ## Why this is a function and not a cached value
 *
 * The user can turn TalkBack on or off without leaving the app — that is
 * exactly what happened in the 2026-08-16 device session — so every caller
 * re-reads it. Caching this would leave the app in whichever mode it happened
 * to start in.
 *
 * ## What callers do with it
 *
 * Nothing in this app may start audio on its own while this is true. There is
 * no API for "TalkBack has finished speaking", so the app cannot sequence
 * itself behind it the way it can behind its own narrator; the only honest
 * alternative to guessing at timing is to let the person press play. See
 * `STATE.md`, "The clashing voices".
 */
fun screenReaderActive(context: Context): Boolean =
    (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager)
        .isTouchExplorationEnabled
