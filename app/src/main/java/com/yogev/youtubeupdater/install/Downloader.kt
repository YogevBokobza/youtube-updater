package com.yogev.youtubeupdater.install

import android.content.Context
import com.yogev.youtubeupdater.data.Http
import com.yogev.youtubeupdater.data.RemoteRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** Downloads an APK to the app cache and verifies its SHA-256 when known. */
class Downloader(private val context: Context) {

    suspend fun download(remote: RemoteRelease, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "apks").apply { mkdirs() }
            val target = File(dir, remote.assetName)

            // Skip the download if this exact version is already cached and valid.
            if (isCachedValid(target, remote)) {
                onProgress(1f)
                return@withContext target
            }

            val request = Request.Builder().url(remote.downloadUrl).build()
            Http.client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("הורדה נכשלה (${resp.code})")
                val body = resp.body ?: throw IOException("תשובה ריקה")
                val total = body.contentLength().takeIf { it > 0 } ?: remote.sizeBytes
                val digest = MessageDigest.getInstance("SHA-256")

                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buffer)
                            if (n == -1) break
                            output.write(buffer, 0, n)
                            digest.update(buffer, 0, n)
                            read += n
                            if (total > 0) onProgress((read.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }

                val expected = remote.sha256
                if (expected != null) {
                    val actual = digest.digest().joinToString("") { "%02x".format(it) }
                    if (!actual.equals(expected, ignoreCase = true)) {
                        target.delete()
                        throw IOException("אימות SHA-256 נכשל — הקובץ נמחק")
                    }
                }
            }
            target
        }

    /** A previously downloaded file counts as valid if its SHA-256 (when known)
     *  or size matches — so we install instead of re-downloading. */
    private fun isCachedValid(file: File, remote: RemoteRelease): Boolean {
        if (!file.exists() || file.length() == 0L) return false
        val expected = remote.sha256
        if (expected != null) {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n == -1) break
                    digest.update(buffer, 0, n)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            return actual.equals(expected, ignoreCase = true)
        }
        return remote.sizeBytes > 0 && file.length() == remote.sizeBytes
    }
}
