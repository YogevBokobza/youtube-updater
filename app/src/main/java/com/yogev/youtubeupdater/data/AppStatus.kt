package com.yogev.youtubeupdater.data

/** A conflicting (non-RE) variant found installed on the device. */
data class ConflictPackage(
    val packageName: String,
    val label: String,
)

/** Combined installed + remote state for one monitored app. */
data class AppStatus(
    val source: UpdateSource,
    val installedVersion: String?,
    val remote: RemoteRelease?,
    val error: String? = null,
    val conflicts: List<ConflictPackage> = emptyList(),
    /** Installed build is signed differently than the official one — a plain
     *  update will fail silently; needs uninstall-then-install instead. */
    val signatureMismatch: Boolean = false,
) {
    val isInstalled: Boolean get() = installedVersion != null

    val updateAvailable: Boolean
        get() = remote != null && Versions.isNewer(remote.version, installedVersion)
}
