package com.sagay.touristnav.ui

import android.content.Context

private const val ONBOARDING_PREFS = "sagay_onboarding"
private const val KEY_SEEN = "seen_v1"

/** Remembers whether the first-run tour has already been shown. */
object OnboardingPrefs {
    fun isSeen(context: Context): Boolean =
        context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SEEN, false)

    fun markSeen(context: Context) {
        context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SEEN, true)
            .apply()
    }
}
