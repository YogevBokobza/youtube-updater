package com.yogev.youtubeupdater.data

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Shared OkHttp client. */
object Http {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.MINUTES) // large APK downloads
            .build()
    }
}
