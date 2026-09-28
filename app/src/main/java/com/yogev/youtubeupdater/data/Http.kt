package com.yogev.youtubeupdater.data

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/** Shared OkHttp clients. */
object Http {

    /** Plain client — used for APK downloads, which must not go through the cache. */
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.MINUTES) // large APK downloads
            .build()
    }

    @Volatile
    private var apiClient: OkHttpClient? = null

    /**
     * Client for the GitHub API, backed by a disk cache. The cache lets OkHttp
     * replay the stored `ETag` as `If-None-Match`, so an unchanged release list
     * comes back as `304 Not Modified` — and a 304 does not count against
     * GitHub's rate limit. Polling every few hours therefore costs us almost
     * nothing even unauthenticated. Shares the connection pool with [client].
     */
    fun api(context: Context): OkHttpClient =
        apiClient ?: synchronized(this) {
            apiClient ?: client.newBuilder()
                .cache(Cache(File(context.applicationContext.cacheDir, "gh-api"), API_CACHE_BYTES))
                .build()
                .also { apiClient = it }
        }

    private const val API_CACHE_BYTES = 5L * 1024 * 1024
}
