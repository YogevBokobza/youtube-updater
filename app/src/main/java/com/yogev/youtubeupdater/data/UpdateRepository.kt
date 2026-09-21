package com.yogev.youtubeupdater.data

import android.content.Context

/** Coordinates GitHub lookups with the device's installed versions. */
class UpdateRepository(private val context: Context) {

    private val prefs = Prefs(context)

    private fun client(): GitHubClient = GitHubClient(Http.client, prefs.token)

    suspend fun status(source: UpdateSource): AppStatus {
        val installed = InstalledApps.get(context, source.packageName)
        val conflicts = source.conflictingPackages.mapNotNull { pkg ->
            InstalledApps.label(context, pkg)?.let { ConflictPackage(pkg, it) }
        }
        return try {
            val remote = client().resolve(source, prefs.includePrereleases)
            AppStatus(source, installed?.versionName, remote, conflicts = conflicts)
        } catch (e: Exception) {
            AppStatus(
                source,
                installed?.versionName,
                remote = null,
                error = e.message ?: "שגיאה",
                conflicts = conflicts,
            )
        }
    }

    suspend fun statuses(): List<AppStatus> = Sources.ALL.map { status(it) }
}
