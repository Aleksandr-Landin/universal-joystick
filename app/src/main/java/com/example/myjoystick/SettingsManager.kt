package com.example.myjoystick

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "joystick_prefs"
    private const val KEY_TRACKPAD_W = "trackpad_w"
    private const val KEY_TRACKPAD_H = "trackpad_h"
    private const val KEY_BUTTONS_W = "buttons_w"
    private const val KEY_ALPHA = "panel_alpha"
    private const val KEY_IS_EXPANDED = "is_expanded"

    private const val DEF_TRACKPAD_W = 250
    private const val DEF_TRACKPAD_H = 150
    private const val DEF_BUTTONS_W = 250
    private const val DEF_ALPHA = 80
    private const val DEF_IS_EXPANDED = true

    const val ACTION_REBUILD = "com.example.myjoystick.REBUILD_PANEL"

    fun saveSettings(context: Context, tW: Int, tH: Int, bW: Int, alpha: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putInt(KEY_TRACKPAD_W, tW)
            putInt(KEY_TRACKPAD_H, tH)
            putInt(KEY_BUTTONS_W, bW)
            putInt(KEY_ALPHA, alpha)
            apply()
        }
    }

    fun saveExpanded(context: Context, isExpanded: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_IS_EXPANDED, isExpanded).apply()
    }

    fun getTrackpadW(context: Context): Int = dpToPx(context, getPrefs(context).getInt(KEY_TRACKPAD_W, DEF_TRACKPAD_W))
    fun getTrackpadH(context: Context): Int = dpToPx(context, getPrefs(context).getInt(KEY_TRACKPAD_H, DEF_TRACKPAD_H))
    fun getButtonsW(context: Context): Int = dpToPx(context, getPrefs(context).getInt(KEY_BUTTONS_W, DEF_BUTTONS_W))
    fun getAlpha(context: Context): Float = getPrefs(context).getInt(KEY_ALPHA, DEF_ALPHA) / 100f
    fun isExpanded(context: Context): Boolean = getPrefs(context).getBoolean(KEY_IS_EXPANDED, DEF_IS_EXPANDED)

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
