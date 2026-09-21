package com.yogev.youtubeupdater.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.yogev.youtubeupdater.R
import com.yogev.youtubeupdater.data.AppStatus
import com.yogev.youtubeupdater.ui.MainActivity

object Notifications {

    const val CHANNEL = "updates"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) == null) {
            val channel = NotificationChannel(
                CHANNEL,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.notif_channel_desc) }
            manager.createNotificationChannel(channel)
        }
    }

    /** Posts (once per version) that an update is available; tapping opens the app. */
    fun updateAvailable(context: Context, status: AppStatus) {
        val remote = status.remote ?: return
        ensureChannel(context)

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            status.source.key.hashCode(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.update_available_title))
            .setContentText("${status.source.displayName} ${remote.version}")
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        notify(context, status.source.key.hashCode(), notification)
    }

    fun installResult(context: Context, label: String, success: Boolean, message: String?) {
        ensureChannel(context)
        val text = if (success) "$label הותקן בהצלחה" else "$label נכשל${message?.let { ": $it" } ?: ""}"
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(
                if (success) android.R.drawable.stat_sys_download_done
                else android.R.drawable.stat_notify_error
            )
            .setContentTitle(label)
            .setContentText(text)
            .setAutoCancel(true)
            .build()
        notify(context, ("result_$label").hashCode(), notification)
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        val nm = NotificationManagerCompat.from(context)
        if (nm.areNotificationsEnabled()) {
            try {
                nm.notify(id, notification)
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS not granted on API 33+; ignore.
            }
        }
    }
}
