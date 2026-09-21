package com.yogev.youtubeupdater.data

/** Dotted numeric version comparison (e.g. "21.13.164"). */
object Versions {

    fun parse(v: String): List<Int> =
        v.trim().split(Regex("[.\\-_]")).mapNotNull { it.toIntOrNull() }

    /** compares two version strings; positive if [a] > [b]. */
    fun compare(a: String, b: String): Int {
        val pa = parse(a)
        val pb = parse(b)
        val n = maxOf(pa.size, pb.size)
        for (i in 0 until n) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    /** true when [remote] is strictly newer than the [installed] version. */
    fun isNewer(remote: String, installed: String?): Boolean {
        if (installed.isNullOrBlank()) return true
        return compare(remote, installed) > 0
    }
}
