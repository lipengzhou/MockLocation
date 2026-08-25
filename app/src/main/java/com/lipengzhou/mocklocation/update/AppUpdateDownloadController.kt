package com.lipengzhou.mocklocation.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.lipengzhou.mocklocation.state.AvailableAppUpdate
import java.io.File

data class AppUpdateDownload(
    val id: Long,
    val fileName: String,
)

sealed interface AppUpdateDownloadStatus {
    data class Completed(val fileName: String) : AppUpdateDownloadStatus
    data class Failed(val message: String) : AppUpdateDownloadStatus
    data object InProgress : AppUpdateDownloadStatus
}

class AppUpdateDownloadController(context: Context) {
    private val appContext = context.applicationContext
    private val downloadManager = appContext.getSystemService(DownloadManager::class.java)

    fun enqueue(release: AvailableAppUpdate): AppUpdateDownload {
        val fileName = release.safeApkFileName()
        val apkFile = apkFile(fileName)
        runCatching { apkFile.delete() }

        val request = DownloadManager.Request(Uri.parse(release.downloadUrl))
            .setTitle("MockLocation ${release.tagName}")
            .setDescription("正在下载 ${release.assetName}")
            .setMimeType(APK_MIME_TYPE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                appContext,
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )

        return AppUpdateDownload(
            id = downloadManager.enqueue(request),
            fileName = fileName
        )
    }

    fun status(downloadId: Long, fileName: String): AppUpdateDownloadStatus {
        val query = DownloadManager.Query().setFilterById(downloadId)
        return downloadManager.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return@use AppUpdateDownloadStatus.Failed("下载任务不存在。")
            }

            when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_SUCCESSFUL -> AppUpdateDownloadStatus.Completed(fileName)
                DownloadManager.STATUS_FAILED -> AppUpdateDownloadStatus.Failed(
                    "下载失败：${cursor.downloadFailureReasonText()}"
                )
                else -> AppUpdateDownloadStatus.InProgress
            }
        } ?: AppUpdateDownloadStatus.Failed("无法读取下载状态。")
    }

    fun canRequestPackageInstalls(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            appContext.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(): Boolean {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${appContext.packageName}")
            )
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return runCatching {
            appContext.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun openInstaller(fileName: String): Boolean {
        val apkFile = apkFile(fileName)
        if (!apkFile.exists()) {
            return false
        }

        val apkUri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(apkUri, APK_MIME_TYPE)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return runCatching {
            appContext.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun apkFile(fileName: String): File {
        val downloadsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: appContext.filesDir
        return File(downloadsDir, fileName)
    }

    private fun AvailableAppUpdate.safeApkFileName(): String {
        val sourceName = assetName.takeIf { it.endsWith(".apk", ignoreCase = true) }
            ?: "MockLocation-$tagName-release.apk"
        return sourceName.replace(Regex("""[^A-Za-z0-9._-]"""), "_")
    }

    private fun android.database.Cursor.downloadFailureReasonText(): String {
        return when (getInt(getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))) {
            DownloadManager.ERROR_CANNOT_RESUME -> "无法继续下载"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "存储设备不可用"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "文件已存在"
            DownloadManager.ERROR_FILE_ERROR -> "文件写入失败"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "网络数据错误"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "存储空间不足"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "重定向次数过多"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "下载服务器响应异常"
            DownloadManager.ERROR_UNKNOWN -> "未知错误"
            else -> "未知错误"
        }
    }

    companion object {
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
