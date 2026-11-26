package com.yogev.youtubeupdater.data

import android.os.Build

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
    /** Patch bundle repo named in the release notes, used to show its version. */
    val patchesRepo: String? = null,
) {
    enum class Mode { LATEST, SCAN }
}

object Sources {

    private const val MORPHE_PATCHES = "MorpheApp/morphe-patches"

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
        patchesRepo = MORPHE_PATCHES,
    )

    /**
     * YouTube Music Morphe. Same rotating-release repo as YouTube Morphe, but
     * this app ships one APK per CPU architecture (no universal "-all" build) —
     * pick the variant matching the device.
     */
    private val musicAbiToken: String =
        if (Build.SUPPORTED_ABIS.contains("arm64-v8a")) "arm64-v8a" else "arm-v7a"

    val MUSIC = UpdateSource(
        key = "music",
        displayName = "YouTube Music Morphe",
        packageName = "app.morphe.android.apps.youtube.music",
        owner = "j-hc",
        repo = "revanced-magisk-module",
        assetPattern = Regex("""^music-morphe-v(.+)-$musicAbiToken\.apk$"""),
        mode = UpdateSource.Mode.SCAN,
        scanPages = 3,
        perPage = 30,
        expectedSignerSha256 = "637c226c67aec0cdbc6f49cd476d5247f999122606286273e16233a913a088b4",
        patchesRepo = MORPHE_PATCHES,
    )

    /**
     * Gboard patched with jasonwu1994/Gboard-patches. That project ships only
     * the patch bundle, so our own builder repo (a j-hc builder fork) patches
     * Gboard in CI and releases the APK. Its default "Package Rename" patch
     * gives it its own package, so it installs next to the stock Gboard.
     * Builds are arm64-v8a only: the patches support no other ABI.
     */
    val GBOARD = UpdateSource(
        key = "gboard",
        displayName = "Gboard Patches",
        packageName = "dev.jason.com.google.android.inputmethod.latin",
        owner = "YogevBokobza",
        repo = "gboard-morphe-builder",
        assetPattern = Regex("""^gboard-patches-v(.+)-arm64-v8a\.apk$"""),
        mode = UpdateSource.Mode.LATEST,
        // The builder signs with the same j-hc key as the Morphe builds.
        expectedSignerSha256 = "637c226c67aec0cdbc6f49cd476d5247f999122606286273e16233a913a088b4",
        patchesRepo = "jasonwu1994/Gboard-patches",
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

    val ALL = listOf(YOUTUBE, MUSIC, MICROG, GBOARD, SELF)

    fun byKey(key: String): UpdateSource? = ALL.firstOrNull { it.key == key }
}
