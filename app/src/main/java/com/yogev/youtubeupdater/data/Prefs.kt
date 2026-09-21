package com.yogev.youtubeupdater.data

import android.content.Context

/** Small SharedPreferences wrapper for user settings and de-dup of notifications. */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("settings", Context.MODE_PRIVATE)

    var token: String?
        get() = sp.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }
        set(value) = sp.edit().putString(KEY_TOKEN, value?.trim()).apply()

    var includePrereleases: Boolean
        get() = sp.getBoolean(KEY_PRERELEASE, false)
        set(value) = sp.edit().putBoolean(KEY_PRERELEASE, value).apply()

    fun lastNotifiedVersion(key: String): String? =
        sp.getString("$KEY_LAST_NOTIFIED$key", null)

    fun setLastNotifiedVersion(key: String, version: String) {
        sp.edit().putString("$KEY_LAST_NOTIFIED$key", version).apply()
    }

    companion object {
        private const val KEY_TOKEN = "github_token"
        private const val KEY_PRERELEASE = "include_prereleases"
        private const val KEY_LAST_NOTIFIED = "last_notified_"
    }
}
