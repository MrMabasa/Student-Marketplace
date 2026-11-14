
package com.studentmarketplace.ui.settings

import android.content.Context

object AppPreferences {

    private const val PREFS_NAME = "student_marketplace_preferences"

    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDarkModeEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_DARK_MODE, false)

    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit()
            .putBoolean(KEY_DARK_MODE, enabled)
            .apply()
    }

    fun getLanguage(context: Context): String =
        preferences(context).getString(KEY_LANGUAGE, "en") ?: "en"

    fun setLanguage(context: Context, language: String) {
        preferences(context).edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    fun areNotificationsEnabled(context: Context): Boolean =
        preferences(context).getBoolean(KEY_NOTIFICATIONS, true)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit()
            .putBoolean(KEY_NOTIFICATIONS, enabled)
            .apply()
    }
}
