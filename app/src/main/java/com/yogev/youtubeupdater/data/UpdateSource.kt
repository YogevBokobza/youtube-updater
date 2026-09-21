package com.yogev.youtubeupdater.data

/**
 * A monitored app: where its releases live on GitHub, how to recognise the right
 * APK asset, and which installed package it maps to on the device.
 */
data class UpdateSource(
    val key: String,
    val displayName: String,
    val packageName: String,
    val owner: String,
    val repo: String,
    val assetPattern: Regex,
    val mode: Mode,
    /** How many release pages to scan (SCAN mode only). */
    val scanPages: Int = 1,
    val perPage: Int = 30,
) {
    enum class Mode { LATEST, SCAN }
}

object Sources {

    /**
     * YouTube Morphe (patched YouTube). Built by j-hc's CI, where each release
     * carries only a subset of apps, so the youtube-morphe APK is rarely in
     * `releases/latest` — we scan the recent releases instead.
     *
     * Package name defaults to the ReVanced/Morphe GmsCore package; verify on the
     * device (Settings → Apps → YouTube Morphe) and adjust if different.
     */
    val YOUTUBE = UpdateSource(
        key = "youtube",
        displayName = "YouTube Morphe",
        packageName = "app.revanced.android.youtube",
        owner = "j-hc",
        repo = "revanced-magisk-module",
        assetPattern = Regex("""^youtube-morphe-v(.+)-all\.apk$"""),
        mode = UpdateSource.Mode.SCAN,
        scanPages = 3,
        perPage = 30,
    )

    /** MicroG RE — clean tag-per-version releases; the universal APK. */
    val MICROG = UpdateSource(
        key = "microg",
        displayName = "MicroG RE",
        packageName = "app.revanced.android.gms",
        owner = "MorpheApp",
        repo = "MicroG-RE",
        assetPattern = Regex("""^microg-([0-9][0-9.]*)\.apk$"""),
        mode = UpdateSource.Mode.LATEST,
    )

    val ALL = listOf(YOUTUBE, MICROG)

    fun byKey(key: String): UpdateSource? = ALL.firstOrNull { it.key == key }
}
