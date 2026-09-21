package com.yogev.youtubeupdater.install

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import java.io.File

/**
 * Installs an APK via the PackageInstaller session API. On a non-root device the
 * system still shows a confirmation dialog (handled by [InstallResultReceiver]).
 */
object ApkInstaller {

    const val EXTRA_LABEL = "extra_label"

    fun install(context: Context, apk: File, label: String) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )
        val sessionId = installer.createSession(params)

        installer.openSession(sessionId).use { session ->
            apk.inputStream().use { input ->
                session.openWrite("package", 0, apk.length()).use { output ->
                    input.copyTo(output)
                    session.fsync(output)
                }
            }

            val callback = Intent(context, InstallResultReceiver::class.java).apply {
                putExtra(EXTRA_LABEL, label)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            val pending = PendingIntent.getBroadcast(context, sessionId, callback, flags)
            session.commit(pending.intentSender)
        }
    }
}
