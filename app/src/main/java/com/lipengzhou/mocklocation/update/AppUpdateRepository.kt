package com.lipengzhou.mocklocation.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateRelease(
    val tagName: String,
    val versionName: String,
    val title: String,
    val releaseUrl: String,
    val downloadUrl: String,
    val assetName: String,
    val assetSizeBytes: Long,
    val releaseNotes: String,
)

class AppUpdateRepository(context: Context) {
    private val appContext = context.applicationContext

    @Suppress("DEPRECATION")
    fun currentVersionName(): String =
        runCatching {
            appContext.packageManager
                .getPackageInfo(appContext.packageName, 0)
                .versionName
                .orEmpty()
        }.getOrDefault("")

    suspend fun findLatestUpdate(): AppUpdateRelease? = withContext(Dispatchers.IO) {
        val currentVersionName = currentVersionName()
        val release = fetchLatestRelease()
        if (isNewerVersion(release.versionName, currentVersionName)) {
            release
        } else {
            null
        }
    }

    private fun fetchLatestRelease(): AppUpdateRelease {
        val connection = (URL(UPDATE_MANIFEST_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = HTTP_TIMEOUT_MS
            readTimeout = HTTP_TIMEOUT_MS
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "MockLocation-Android")
            useCaches = false
        }

        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("更新清单返回 $responseCode")
            }

            val responseText = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(responseText).toAppUpdateRelease()
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.toAppUpdateRelease(): AppUpdateRelease {
        val tagName = optString("tagName").trim()
            .ifBlank { optString("tag_name").trim() }
        val versionName = optString("versionName").trim()
            .ifBlank { tagName.toVersionName() }
        val title = optString("title").trim()
            .ifBlank { optString("name").trim() }
            .ifBlank { tagName }
        val releaseUrl = optString("releaseUrl").trim()
            .ifBlank { optString("html_url").trim() }
        val releaseNotes = optString("releaseNotes").trim()
            .ifBlank { optString("body").trim() }
        val apkAsset = optJSONArray("assets").findApkAsset()
        val assetName = optString("assetName").trim()
            .ifBlank { apkAsset?.optString("name").orEmpty() }
        val downloadUrl = optString("downloadUrl").trim()
            .ifBlank { apkAsset?.optString("browser_download_url").orEmpty() }
            .ifBlank { releaseUrl }
        val assetSizeBytes = longValue("assetSizeBytes")
            ?: apkAsset?.longValue("size")
            ?: 0L

        if (tagName.isBlank() || versionName.isBlank() || releaseUrl.isBlank() || downloadUrl.isBlank()) {
            throw IOException("更新清单信息不完整")
        }

        return AppUpdateRelease(
            tagName = tagName,
            versionName = versionName,
            title = title,
            releaseUrl = releaseUrl,
            downloadUrl = downloadUrl,
            assetName = assetName.ifBlank { "release APK" },
            assetSizeBytes = assetSizeBytes,
            releaseNotes = releaseNotes
        )
    }

    private fun JSONArray?.findApkAsset(): JSONObject? {
        if (this == null) return null
        for (index in 0 until length()) {
            val asset = optJSONObject(index) ?: continue
            val name = asset.optString("name")
            val state = asset.optString("state")
            if (name.endsWith(".apk", ignoreCase = true) && state.equals("uploaded", ignoreCase = true)) {
                return asset
            }
        }
        return null
    }

    private fun JSONObject.longValue(name: String): Long? =
        when (val value = opt(name)) {
            is Number -> value.toLong()
            is String -> value.trim().toLongOrNull()
            else -> null
        }

    private fun String.toVersionName(): String =
        VERSION_PATTERN.find(this)?.value.orEmpty()

    private fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
        val remoteParts = remoteVersion.versionParts()
        val currentParts = currentVersion.versionParts()
        if (remoteParts.isEmpty() || currentParts.isEmpty()) {
            return remoteVersion.isNotBlank() && remoteVersion != currentVersion
        }

        val maxSize = maxOf(remoteParts.size, currentParts.size)
        for (index in 0 until maxSize) {
            val remotePart = remoteParts.getOrNull(index) ?: 0
            val currentPart = currentParts.getOrNull(index) ?: 0
            if (remotePart != currentPart) {
                return remotePart > currentPart
            }
        }
        return false
    }

    private fun String.versionParts(): List<Int> =
        split(".")
            .mapNotNull { part -> part.toIntOrNull() }

    companion object {
        private const val UPDATE_MANIFEST_URL =
            "https://cdn.jsdelivr.net/gh/lipengzhou/MockLocation@main/release/update.json"
        private const val HTTP_TIMEOUT_MS = 8_000
        private val VERSION_PATTERN = Regex("""\d+(?:\.\d+)*""")
    }
}
