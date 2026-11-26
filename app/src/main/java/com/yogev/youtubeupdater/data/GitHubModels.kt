package com.yogev.youtubeupdater.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GhRelease(
    @SerialName("tag_name") val tagName: String? = null,
    val name: String? = null,
    val prerelease: Boolean = false,
    val draft: Boolean = false,
    @SerialName("html_url") val htmlUrl: String? = null,
    val body: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<GhAsset> = emptyList(),
)

@Serializable
data class GhAsset(
    val name: String,
    val size: Long = 0,
    /** e.g. "sha256:abcd…" — may be absent on older assets. */
    val digest: String? = null,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    /** API URL of the asset; the only download URL that works for a private repo. */
    @SerialName("url") val apiUrl: String? = null,
)

/** A resolved candidate: the newest matching APK for a source. */
@Serializable
data class RemoteRelease(
    val version: String,
    val assetName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val sha256: String?,
    val notes: String?,
    val htmlUrl: String?,
    /** ISO-8601 publish time of the release carrying this asset. */
    val publishedAt: String? = null,
    /** Patch bundle version from the release notes (e.g. "2.4.0"), if listed. */
    val patchesVersion: String? = null,
    /** API URL of the asset, used with a token when the repo is private. */
    val apiUrl: String? = null,
    /** From a private repo: download through [apiUrl] with the user's token. */
    val requiresAuth: Boolean = false,
) {
    /** [publishedAt] as epoch millis, or null when absent/unparseable. */
    val publishedMillis: Long?
        get() = publishedAt?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }

    /** Identifies one build: j-hc rebuilds the same app version with newer patches. */
    val buildId: String get() = "$version@${publishedAt ?: ""}"
}
