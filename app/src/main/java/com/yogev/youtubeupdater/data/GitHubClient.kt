package com.yogev.youtubeupdater.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Reads GitHub Releases. Unauthenticated is fine (60 req/h); an optional token
 * raises the limit and is used when provided.
 */
class GitHubClient(
    private val client: OkHttpClient,
    private val token: String? = null,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Resolve the newest matching APK for a source, or null if none found. */
    suspend fun resolve(source: UpdateSource, includePrereleases: Boolean): RemoteRelease? =
        withContext(Dispatchers.IO) {
            when (source.mode) {
                UpdateSource.Mode.LATEST -> resolveLatest(source, includePrereleases)
                UpdateSource.Mode.SCAN -> resolveByScan(source, includePrereleases)
            }
        }

    private fun resolveLatest(source: UpdateSource, includePrereleases: Boolean): RemoteRelease? {
        // /releases/latest already excludes prereleases and drafts.
        val url = "https://api.github.com/repos/${source.owner}/${source.repo}/releases/latest"
        val release = getReleases(url).firstOrNull() ?: return null
        return pickFrom(listOf(release), source)
    }

    private fun resolveByScan(source: UpdateSource, includePrereleases: Boolean): RemoteRelease? {
        val releases = mutableListOf<GhRelease>()
        for (page in 1..source.scanPages) {
            val url = "https://api.github.com/repos/${source.owner}/${source.repo}" +
                "/releases?per_page=${source.perPage}&page=$page"
            val batch = getReleases(url)
            if (batch.isEmpty()) break
            releases += batch
        }
        val usable = releases.filter { !it.draft && (includePrereleases || !it.prerelease) }
        return pickFrom(usable, source)
    }

    /** Among the releases, find every matching asset and keep the highest version. */
    private fun pickFrom(releases: List<GhRelease>, source: UpdateSource): RemoteRelease? {
        var best: RemoteRelease? = null
        for (release in releases) {
            for (asset in release.assets) {
                val match = source.assetPattern.matchEntire(asset.name) ?: continue
                val version = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() } ?: continue
                if (best == null || Versions.compare(version, best!!.version) > 0) {
                    best = RemoteRelease(
                        version = version,
                        assetName = asset.name,
                        downloadUrl = asset.browserDownloadUrl,
                        sizeBytes = asset.size,
                        sha256 = asset.digest?.substringAfter("sha256:", "")?.takeIf { it.isNotBlank() },
                        notes = release.body,
                        htmlUrl = release.htmlUrl,
                    )
                }
            }
        }
        return best
    }

    /** GET a releases URL; returns a list (the /latest endpoint returns one object,
     *  which we wrap by requesting it as a single-element parse). */
    private fun getReleases(url: String): List<GhRelease> {
        val body = get(url) ?: return emptyList()
        val trimmed = body.trimStart()
        return if (trimmed.startsWith("[")) {
            json.decodeFromString<List<GhRelease>>(body)
        } else {
            listOf(json.decodeFromString<GhRelease>(body))
        }
    }

    private fun get(url: String): String? {
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "youtube-updater")
            .header("X-GitHub-Api-Version", "2022-11-28")
        if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer $token")
        client.newCall(builder.build()).execute().use { resp ->
            if (resp.code == 404) return null
            if (!resp.isSuccessful) throw IOException("GitHub ${resp.code} for $url")
            return resp.body?.string()
        }
    }
}
