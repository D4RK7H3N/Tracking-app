package com.sagay.touristnav.ui

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

object Motion {
    const val MICRO_MS = 120
    const val LAYOUT_MS = 220
    const val PAGE_MS = 320

    val easing = FastOutSlowInEasing

    @Composable
    fun reduced(): Boolean {
        val scale = Settings.Global.getFloat(
            LocalContext.current.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
        return scale == 0f
    }

    @Composable
    fun duration(base: Int): Int = if (reduced()) 0 else base
}
