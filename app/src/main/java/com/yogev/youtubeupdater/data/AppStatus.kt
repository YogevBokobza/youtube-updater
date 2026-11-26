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
    /** When the installed package was last installed/updated (epoch millis). */
    val installedAt: Long? = null,
) {
    val isInstalled: Boolean get() = installedVersion != null

    /**
     * Same app version, but the release was published after the installed copy
     * was put on the device: a rebuild with newer patches (j-hc keeps the
     * YouTube version and bumps only the patch bundle).
     */
    val newerBuild: Boolean
        get() {
            val r = remote ?: return false
            if (installedVersion == null || Versions.compare(r.version, installedVersion) != 0) return false
            val published = r.publishedMillis ?: return false
            val installed = installedAt ?: return false
            return published > installed
        }

    val updateAvailable: Boolean
        get() = remote != null && (Versions.isNewer(remote.version, installedVersion) || newerBuild)
}
