package com.yogev.youtubeupdater.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yogev.youtubeupdater.data.Prefs
import com.yogev.youtubeupdater.data.UpdateRepository
import com.yogev.youtubeupdater.notify.Notifications

/** Periodically checks each source and notifies once per new version. */
class CheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = UpdateRepository(applicationContext)
        val prefs = Prefs(applicationContext)
        return try {
            for (status in repo.loadStatuses(force = true)) {
                if (!status.updateAvailable) continue
                // Build id, not just the version: a same-version rebuild with
                // newer patches must notify too.
                val build = status.remote?.buildId ?: continue
                if (prefs.lastNotifiedVersion(status.source.key) != build) {
                    Notifications.updateAvailable(applicationContext, status)
                    prefs.setLastNotifiedVersion(status.source.key, build)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
