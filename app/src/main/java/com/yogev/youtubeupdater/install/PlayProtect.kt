package com.yogev.youtubeupdater.install

import android.content.Context
import android.content.Intent

/**
 * Play Protect blocks installs of apps that impersonate Google Play Services
 * (microG being the classic case). A non-root app cannot disable that scan —
 * only the user can, in the Play Store's Play Protect settings. These helpers
 * take the user straight there.
 */
object PlayProtect {

    /** Best available intent to the Play Protect / verify-apps settings, or null. */
    fun settingsIntent(context: Context): Intent? {
        val candidates = listOf(
            Intent("com.google.android.gms.settings.VERIFY_APPS_SETTINGS"),
            Intent().setClassName(
                "com.google.android.gms",
                "com.google.android.gms.security.settings.VerifyAppsSettingsActivity",
            ),
            context.packageManager.getLaunchIntentForPackage("com.android.vending"),
        )
        for (intent in candidates) {
            if (intent == null) continue
            if (intent.resolveActivity(context.packageManager) != null) {
                return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        return null
    }

    fun openSettings(context: Context): Boolean {
        val intent = settingsIntent(context) ?: return false
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
