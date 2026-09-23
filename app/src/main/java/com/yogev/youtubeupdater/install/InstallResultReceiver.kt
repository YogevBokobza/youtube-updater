package com.yogev.youtubeupdater.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.yogev.youtubeupdater.notify.Notifications

/** Receives PackageInstaller results and launches the system confirm dialog. */
class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE,
        )
        val label = intent.getStringExtra(ApkInstaller.EXTRA_LABEL) ?: "אפליקציה"

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                confirm?.let { context.startActivity(it) }
            }

            PackageInstaller.STATUS_SUCCESS ->
                Notifications.installResult(context, label, success = true, message = null)

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                // user cancelled — stay quiet
            }

            PackageInstaller.STATUS_FAILURE_BLOCKED -> {
                // Blocked by Play Protect (or a device policy) — guide the user to the setting.
                Notifications.playProtectBlocked(context, label)
            }

            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                Notifications.installResult(context, label, success = false, message = message)
            }
        }
    }
}
