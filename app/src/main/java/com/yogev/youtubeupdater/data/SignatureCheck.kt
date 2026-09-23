package com.yogev.youtubeupdater.data

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Reads the SHA-256 digest(s) of an installed package's signing certificate(s).
 *
 * Android refuses to install an update over a package signed with a different
 * key (INSTALL_FAILED_UPDATE_INCOMPATIBLE) — it just fails silently from the
 * user's point of view. Comparing against a pinned "official" digest lets us
 * detect that case up front and offer uninstall-then-install instead of a
 * plain update that would never actually apply.
 */
object SignatureCheck {

    /** Lowercase hex SHA-256 digests of the installed app's signer(s), or null if not installed. */
    fun installedSignerHashes(context: Context, packageName: String): Set<String>? {
        val info = try {
            getPackageInfo(context, packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
        return certHashes(info)
    }

    private fun getPackageInfo(context: Context, packageName: String): PackageInfo {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(packageName, flags)
        }
    }

    private fun certHashes(info: PackageInfo): Set<String> {
        val certBytes: List<ByteArray> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo
            val certs = when {
                signingInfo == null -> emptyArray()
                signingInfo.hasMultipleSigners() -> signingInfo.apkContentsSigners
                else -> signingInfo.signingCertificateHistory
            }
            certs.map { it.toByteArray() }
        } else {
            @Suppress("DEPRECATION")
            (info.signatures ?: emptyArray()).map { it.toByteArray() }
        }
        val digest = MessageDigest.getInstance("SHA-256")
        return certBytes.map { bytes ->
            digest.digest(bytes).joinToString("") { "%02x".format(it) }
        }.toSet()
    }
}
