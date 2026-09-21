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
)
