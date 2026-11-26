package com.yogev.youtubeupdater.data

import android.content.Context

/** Coordinates GitHub lookups with the device's installed versions. */
class UpdateRepository(private val context: Context) {

    private val prefs = Prefs(context)

    /** Only the user's own token from Settings — nothing is embedded in the build. */
    private fun client(): GitHubClient = GitHubClient(Http.api(context), prefs.token)

    /**
     * Loads statuses. Network is hit only when [force] is set or the cache is
     * older than [MIN_FETCH_INTERVAL_MS] — this keeps us well under GitHub's
     * unauthenticated rate limit (60/h) despite frequent screen resumes: one cycle is about 5 requests.
     */
    suspend fun loadStatuses(force: Boolean): List<AppStatus> {
        val stale = System.currentTimeMillis() - prefs.lastFetch > MIN_FETCH_INTERVAL_MS
        val fetch = force || stale
        if (fetch) prefs.lastFetch = System.currentTimeMillis()
        // One client for the whole cycle so its page cache is shared across
        // sources; YouTube and YouTube Music scan the same j-hc release pages.
        val client = client()
        return Sources.ALL.map { status(it, fetch, client) }
    }

    private suspend fun status(
        source: UpdateSource,
        fetchRemote: Boolean,
        client: GitHubClient,
    ): AppStatus {
        val installed = InstalledApps.get(context, source.packageName)
        val conflicts = source.conflictingPackages.mapNotNull { pkg ->
            InstalledApps.label(context, pkg)?.let { ConflictPackage(pkg, it) }
        }
        val mismatch = installed != null &&
            source.expectedSignerSha256 != null &&
            run {
                val installedHashes = SignatureCheck.installedSignerHashes(context, source.packageName)
                installedHashes != null && source.expectedSignerSha256.lowercase() !in installedHashes
            }
        val cached = prefs.cachedRemote(source.key)

        if (source.privateRepo && prefs.token == null) {
            return AppStatus(
                source,
                installed?.versionName,
                remote = null,
                error = "ריפו פרטי: הוסף GitHub Token בהגדרות עם הרשאת קריאה ל-${source.owner}/${source.repo}.",
                conflicts = conflicts,
                signatureMismatch = mismatch,
                installedAt = installed?.lastUpdateTime,
            )
        }

        if (!fetchRemote) {
            return AppStatus(source, installed?.versionName, cached, conflicts = conflicts, signatureMismatch = mismatch, installedAt = installed?.lastUpdateTime)
        }

        return try {
            val remote = client.resolve(source, prefs.includePrereleases)
            if (remote != null) prefs.setCachedRemote(source.key, remote)
            // GitHub answers 404 for a private repo the token can't read.
            val error = if (remote == null && source.privateRepo) {
                "לא נמצא release ב-${source.owner}/${source.repo}. בדוק שלטוקן יש גישת קריאה לריפו."
            } else {
                null
            }
            AppStatus(source, installed?.versionName, remote ?: cached, error = error, conflicts = conflicts, signatureMismatch = mismatch, installedAt = installed?.lastUpdateTime)
        } catch (e: Exception) {
            // Keep showing the last known remote version, with a friendly error.
            AppStatus(
                source,
                installed?.versionName,
                remote = cached,
                error = friendlyError(e),
                conflicts = conflicts,
                signatureMismatch = mismatch,
                installedAt = installed?.lastUpdateTime,
            )
        }
    }

    private fun friendlyError(e: Exception): String {
        val msg = e.message ?: "שגיאה"
        return if ("403" in msg || "429" in msg) {
            "מגבלת בקשות ל-GitHub. המתן כשעה, או הוסף GitHub Token בהגדרות."
        } else {
            msg
        }
    }

    companion object {
        private const val MIN_FETCH_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes
    }
}
