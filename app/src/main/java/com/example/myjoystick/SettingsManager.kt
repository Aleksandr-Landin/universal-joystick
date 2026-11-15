package com.example.myjoystick

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "joystick_prefs"
    private const val KEY_WIDTH = "panel_width"
    private const val KEY_TRACKPAD = "trackpad_size" // Новая переменная для квадрата тачпада
    private const val KEY_ALPHA = "panel_alpha"

    private const val DEFAULT_WIDTH_DP = 250
    private const val DEFAULT_TRACKPAD_DP = 150
    private const val DEFAULT_ALPHA = 80

    const val ACTION_REBUILD = "com.example.myjoystick.REBUILD_PANEL"

    fun saveSettings(context: Context, widthDp: Int, trackpadDp: Int, alphaPercent: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_WIDTH, widthDp)
            .putInt(KEY_TRACKPAD, trackpadDp)
            .putInt(KEY_ALPHA, alphaPercent)
            .apply()
    }

    fun getWidthPx(context: Context): Int {
        val dp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_WIDTH, DEFAULT_WIDTH_DP)
        return dpToPx(context, dp)
    }

    fun getTrackpadSizePx(context: Context): Int {
        val dp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_TRACKPAD, DEFAULT_TRACKPAD_DP)
        return dpToPx(context, dp)
    }

    fun getAlpha(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val percent = prefs.getInt(KEY_ALPHA, DEFAULT_ALPHA)
        return percent / 100f
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
