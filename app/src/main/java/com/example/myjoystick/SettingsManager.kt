package com.example.myjoystick

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "joystick_prefs"
    private const val KEY_WIDTH = "panel_width"
    private const val KEY_HEIGHT = "panel_height"
    private const val KEY_ALPHA = "panel_alpha"

    // Наш общий «пароль» для сигнала перестройки.
    // Должен быть одинаковым в настройках и в сервисе!
    const val ACTION_REBUILD = "com.example.myjoystick.REBUILD_PANEL"

    private const val DEFAULT_WIDTH_DP = 250
    private const val DEFAULT_HEIGHT_DP = 350
    private const val DEFAULT_ALPHA = 80

    fun saveSettings(context: Context, widthDp: Int, heightDp: Int, alphaPercent: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_WIDTH, widthDp)
            .putInt(KEY_HEIGHT, heightDp)
            .putInt(KEY_ALPHA, alphaPercent)
            .apply()
    }

    fun getWidthDp(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_WIDTH, DEFAULT_WIDTH_DP)
    }

    fun getHeightDp(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_HEIGHT, DEFAULT_HEIGHT_DP)
    }

    fun getWidthPx(context: Context): Int {
        val dp = getWidthDp(context)
        return dpToPx(context, dp)
    }

    fun getHeightPx(context: Context): Int {
        val dp = getHeightDp(context)
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
