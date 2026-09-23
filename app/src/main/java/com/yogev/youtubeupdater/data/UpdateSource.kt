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
    /** Other (non-RE) variants of this app to offer removing before install. */
    val conflictingPackages: List<String> = emptyList(),
    /**
     * SHA-256 of the official build's signing certificate. When the installed
     * app at [packageName] is signed differently (e.g. a pre-RE legacy build
     * that happens to share the same package name), a plain update would fail
     * silently — we detect it and require uninstall-then-install instead.
     */
    val expectedSignerSha256: String? = null,
) {
    enum class Mode { LATEST, SCAN }
}

object Sources {

    /**
     * YouTube Morphe (patched YouTube). Built by j-hc's CI, where each release
     * carries only a subset of apps, so the youtube-morphe APK is rarely in
     * `releases/latest` — we scan the recent releases instead.
     *
     * Package name confirmed from the Morphe-patched APK manifest.
     */
    val YOUTUBE = UpdateSource(
        key = "youtube",
        displayName = "YouTube Morphe",
        packageName = "app.morphe.android.youtube",
        owner = "j-hc",
        repo = "revanced-magisk-module",
        assetPattern = Regex("""^youtube-morphe-v(.+)-all\.apk$"""),
        mode = UpdateSource.Mode.SCAN,
        scanPages = 3,
        perPage = 30,
        expectedSignerSha256 = "637c226c67aec0cdbc6f49cd476d5247f999122606286273e16233a913a088b4",
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
        conflictingPackages = listOf(
            "com.mgoogle.android.gms", // Vanced microG
            "org.microg.gms",          // standard microG
        ),
        // Legacy ReVanced GmsCore builds used this exact package name with a
        // different signer (versions look like "0.2.x.yymmdd" / "0.3.x.yymmdd").
        expectedSignerSha256 = "0b6c9515afb195fac59601696ba0a7907a0b217ccf720b43148427ccf64343e7",
    )

    /** The updater itself — enables in-app self-update from its own releases. */
    val SELF = UpdateSource(
        key = "self",
        displayName = "מעדכן YouTube (האפליקציה)",
        packageName = "com.yogev.youtubeupdater",
        owner = "YogevBokobza",
        repo = "youtube-updater",
        assetPattern = Regex("""^youtube-updater-v(.+)\.apk$"""),
        mode = UpdateSource.Mode.LATEST,
    )

    val ALL = listOf(YOUTUBE, MICROG, SELF)

    fun byKey(key: String): UpdateSource? = ALL.firstOrNull { it.key == key }
}
