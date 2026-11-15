package com.example.myjoystick

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "joystick_prefs"
    
    private const val KEY_TRACKPAD_WIDTH = "trackpad_width"
    private const val KEY_TRACKPAD_HEIGHT = "trackpad_height"
    private const val KEY_BUTTON_PANEL_WIDTH = "button_panel_width"
    private const val KEY_BUTTON_PANEL_HEIGHT = "button_panel_height"
    private const val KEY_IS_EXPANDED = "is_panel_expanded"

    private const val DEFAULT_TRACKPAD_WIDTH_DP = 250
    private const val DEFAULT_TRACKPAD_HEIGHT_DP = 150
    private const val DEFAULT_BUTTON_PANEL_WIDTH_DP = 250
    private const val DEFAULT_BUTTON_PANEL_HEIGHT_DP = 200
    private const val DEFAULT_IS_EXPANDED = false

    const val ACTION_REBUILD = "com.example.myjoystick.REBUILD_PANEL"

    fun saveSettings(context: Context, trackpadWidthDp: Int, trackpadHeightDp: Int, buttonPanelWidthDp: Int, buttonPanelHeightDp: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_TRACKPAD_WIDTH, trackpadWidthDp)
            .putInt(KEY_TRACKPAD_HEIGHT, trackpadHeightDp)
            .putInt(KEY_BUTTON_PANEL_WIDTH, buttonPanelWidthDp)
            .putInt(KEY_BUTTON_PANEL_HEIGHT, buttonPanelHeightDp)
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

    fun getTrackpadWidthPx(context: Context): Int {
        val dp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_TRACKPAD_WIDTH, DEFAULT_TRACKPAD_WIDTH_DP)
        return dpToPx(context, dp)
    }

    fun getTrackpadHeightPx(context: Context): Int {
        val dp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_TRACKPAD_HEIGHT, DEFAULT_TRACKPAD_HEIGHT_DP)
        return dpToPx(context, dp)
    }

    fun getButtonPanelWidthPx(context: Context): Int {
        val dp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_BUTTON_PANEL_WIDTH, DEFAULT_BUTTON_PANEL_WIDTH_DP)
        return dpToPx(context, dp)
    }

    fun getButtonPanelHeightPx(context: Context): Int {
        val dp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_BUTTON_PANEL_HEIGHT, DEFAULT_BUTTON_PANEL_HEIGHT_DP)
        return dpToPx(context, dp)
    }

    private fun dpToPx(context: Context, dp: Int): Int {
        val density = context.resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
