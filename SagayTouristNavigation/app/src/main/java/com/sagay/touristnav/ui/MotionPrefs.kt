package com.sagay.touristnav.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

private const val DISPLAY_PREFS = "sagay_display_settings"
private const val KEY_MOTION_OVERRIDE = "motion_override"
private const val FOLLOW_SYSTEM = -1
private const val FULL_MOTION = 0
private const val REDUCED_MOTION = 1

/**
 * In-app motion override backed by SharedPreferences.
 *
 * null  -> follow the OS reduce-motion setting
 * false -> full motion even when the OS asks for less
 * true  -> reduce motion even when the OS allows animation
 *
 * The value is a Compose snapshot state so changing it from SettingsSheet
 * immediately recomposes every composable that calls Motion.reduced().
 */
object MotionPrefs {
    private val _override = mutableStateOf<Boolean?>(null)
    val override: State<Boolean?> = _override

    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val stored = context.applicationContext
            .getSharedPreferences(DISPLAY_PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_MOTION_OVERRIDE, FOLLOW_SYSTEM)
        _override.value = when (stored) {
            REDUCED_MOTION -> true
            FULL_MOTION -> false
            else -> null
        }
    }

    fun setOverride(context: Context, value: Boolean?) {
        val stored = when (value) {
            null -> FOLLOW_SYSTEM
            false -> FULL_MOTION
            true -> REDUCED_MOTION
        }
        context.applicationContext
            .getSharedPreferences(DISPLAY_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_MOTION_OVERRIDE, stored)
            .apply()
        loaded = true
        _override.value = value
    }

    /** The raw OS animation-duration scale: 0 means "reduce motion". */
    fun systemReduced(context: Context): Boolean =
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
}
