package com.yogev.youtubeupdater.data

/** Combined installed + remote state for one monitored app. */
data class AppStatus(
    val source: UpdateSource,
    val installedVersion: String?,
    val remote: RemoteRelease?,
    val error: String? = null,
) {
    val isInstalled: Boolean get() = installedVersion != null

    val updateAvailable: Boolean
        get() = remote != null && Versions.isNewer(remote.version, installedVersion)
}
