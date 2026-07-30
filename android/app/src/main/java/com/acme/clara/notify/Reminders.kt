package com.acme.clara.notify

import android.content.Context

/**
 * The welcome-back reminder is **on by default** — this is the opt-out. Stored in a small pref so
 * the Activity can read it in onStop without touching the game state.
 */
object Reminders {
    private const val PREFS = "clara_prefs"
    private const val KEY = "reminders_on"

    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true)

    fun setEnabled(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply()
        if (!on) WelcomeBackNotifier.cancel(context)
    }
}
