package com.example.myjoystick

import android.content.Context

object SettingsManager {
    private const val PREFS_NAME = "joystick_prefs"
    private const val KEY_WIDTH = "panel_width"
    private const val KEY_HEIGHT = "panel_height"
    private const val KEY_ALPHA = "panel_alpha"

    private const val DEF_WIDTH = 280
    private const val DEF_HEIGHT = 400
    private const val DEF_ALPHA = 80

    const val ACTION_REBUILD = "com.example.myjoystick.REBUILD_PANEL"

    fun saveSettings(context: Context, widthDp: Int, heightDp: Int, alphaPercent: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putInt(KEY_WIDTH, widthDp)
            putInt(KEY_HEIGHT, heightDp)
            putInt(KEY_ALPHA, alphaPercent)
            apply()
        }
    }

    fun getWidthPx(context: Context): Int = dpToPx(context, getPrefs(context).getInt(KEY_WIDTH, DEF_WIDTH))
    fun getHeightPx(context: Context): Int = dpToPx(context, getPrefs(context).getInt(KEY_HEIGHT, DEF_HEIGHT))
    fun getAlpha(context: Context): Float = getPrefs(context).getInt(KEY_ALPHA, DEF_ALPHA) / 100f

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private fun dpToPx(context: Context, dp: Int): Int = (dp * context.resources.displayMetrics.density).toInt()
}
