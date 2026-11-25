package com.sagay.touristnav.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * One source of truth for animation timing.
 *
 * The OS setting is still respected by default, but SettingsSheet can store an
 * explicit in-app override; every new animation goes through [duration] so a
 * reduce-motion user never sees a hard-coded tween.
 */
object Motion {
    const val MICRO_MS = 120
    const val LAYOUT_MS = 220
    const val PAGE_MS = 320

    val easing = FastOutSlowInEasing

    @Composable
    fun reduced(): Boolean {
        val context = LocalContext.current
        MotionPrefs.ensureLoaded(context.applicationContext)
        return MotionPrefs.override.value ?: MotionPrefs.systemReduced(context)
    }

    @Composable
    fun duration(base: Int): Int = if (reduced()) 0 else base
}
