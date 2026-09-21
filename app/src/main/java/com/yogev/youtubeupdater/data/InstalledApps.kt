package com.yogev.youtubeupdater.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object InstalledApps {

    data class Installed(val versionName: String?, val versionCode: Long)

    fun get(context: Context, packageName: String): Installed? = try {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, 0)
        }
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        Installed(info.versionName, code)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
