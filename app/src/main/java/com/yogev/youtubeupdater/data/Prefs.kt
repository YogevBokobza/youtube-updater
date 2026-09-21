package com.yogev.youtubeupdater.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Small SharedPreferences wrapper for user settings and de-dup of notifications. */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    /** Epoch millis of the last remote fetch (any source). */
    var lastFetch: Long
        get() = sp.getLong(KEY_LAST_FETCH, 0L)
        set(value) = sp.edit().putLong(KEY_LAST_FETCH, value).apply()

    fun cachedRemote(key: String): RemoteRelease? =
        sp.getString("$KEY_CACHE$key", null)?.let {
            runCatching { json.decodeFromString<RemoteRelease>(it) }.getOrNull()
        }

    fun setCachedRemote(key: String, remote: RemoteRelease) {
        sp.edit().putString("$KEY_CACHE$key", json.encodeToString(remote)).apply()
    }

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
        private const val KEY_LAST_FETCH = "last_fetch"
        private const val KEY_CACHE = "cache_remote_"
    }
}
