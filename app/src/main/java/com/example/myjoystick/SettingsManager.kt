package com.example.myjoystick

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "joystick_prefs"
    private const val KEY_WIDTH = "panel_width"
    private const val KEY_TRACKPAD_HEIGHT = "trackpad_height"
    private const val KEY_BUTTON_HEIGHT = "button_height"
    private const val KEY_ALPHA = "panel_alpha"
    private const val KEY_IS_EXPANDED = "is_panel_expanded"

    private const val DEFAULT_WIDTH_DP = 250
    private const val DEFAULT_TRACKPAD_HEIGHT_DP = 150
    private const val DEFAULT_BUTTON_HEIGHT_DP = 50
    private const val DEFAULT_ALPHA = 80
    private const val DEFAULT_IS_EXPANDED = false

    const val ACTION_REBUILD = "com.example.myjoystick.REBUILD_PANEL"

    fun saveSettings(context: Context, widthDp: Int, trackpadHeightDp: Int, buttonHeightDp: Int, alphaPercent: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_WIDTH, widthDp)
            .putInt(KEY_TRACKPAD_HEIGHT, trackpadHeightDp)
            .putInt(KEY_BUTTON_HEIGHT, buttonHeightDp)
            .putInt(KEY_ALPHA, alphaPercent)
            .apply()
    }

    fun saveExpandedState(context: Context, isExpanded: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_EXPANDED, isExpanded).apply()
    }

    fun isPanelExpanded(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_EXPANDED, DEFAULT_IS_EXPANDED)
    }

    fun getWidthPx(context: Context): Int {
        val dp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_WIDTH, DEFAULT_WIDTH_DP)
        return dpToPx(context, dp)
    }

    fun getTrackpadHeightPx(context: Context): Int {
        val dp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_TRACKPAD_HEIGHT, DEFAULT_TRACKPAD_HEIGHT_DP)
        return dpToPx(context, dp)
    }

    fun getButtonHeightPx(context: Context): Int {
        val dp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_BUTTON_HEIGHT, DEFAULT_BUTTON_HEIGHT_DP)
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
