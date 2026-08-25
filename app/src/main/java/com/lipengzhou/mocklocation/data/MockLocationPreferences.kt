package com.lipengzhou.mocklocation.data

import android.content.Context
import com.lipengzhou.mocklocation.location.MockLocationService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MockLocationPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        MockLocationService.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun savedServiceRunningState(): Boolean =
        prefs.getBoolean(MockLocationService.KEY_IS_RUNNING, false)

    fun savedStatusMessage(): String =
        prefs.getString(MockLocationService.KEY_STATUS_MESSAGE, "准备就绪") ?: "准备就绪"

    fun savedUpdateCount(): Long =
        prefs.getLong(MockLocationService.KEY_UPDATE_COUNT, 0L)

    fun savedProviderNames(): String =
        prefs.getString(MockLocationService.KEY_PROVIDER_NAMES, "无") ?: "无"

    fun savedUpdateIntervalMs(): Long =
        prefs.getLong(
            MockLocationService.KEY_UPDATE_INTERVAL_MS,
            MockLocationService.DEFAULT_UPDATE_INTERVAL_MS
        )

    fun savedWakeDurationMs(): Long =
        prefs.getLong(
            MockLocationService.KEY_WAKE_DURATION_MS,
            MockLocationService.DEFAULT_WAKE_DURATION_MS
        )

    fun savedLastStopTimeText(): String {
        val timeMs = prefs.getLong(MockLocationService.KEY_LAST_STOP_TIME_MS, 0L)
        return if (timeMs <= 0L) {
            "无"
        } else {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timeMs))
        }
    }

    fun savedLastError(): String =
        prefs.getString(MockLocationService.KEY_LAST_ERROR, "无") ?: "无"

    fun savedSearchHistory(): List<String> =
        prefs.getString(SEARCH_HISTORY_PREFS_KEY, "")
            .orEmpty()
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(MAX_SEARCH_HISTORY_ITEMS)

    fun savedLocationPermissionRequested(): Boolean =
        prefs.getBoolean(LOCATION_PERMISSION_REQUESTED_PREFS_KEY, false)

    fun savedNotificationPermissionRequested(): Boolean =
        prefs.getBoolean(NOTIFICATION_PERMISSION_REQUESTED_PREFS_KEY, false)

    fun savedAgreementAccepted(): Boolean =
        prefs.getBoolean(AGREEMENT_ACCEPTED_PREFS_KEY, false)

    fun savedPermissionGuideCompleted(): Boolean =
        prefs.getBoolean(PERMISSION_GUIDE_COMPLETED_PREFS_KEY, false)

    fun saveSearchHistory(history: List<String>) {
        prefs.edit()
            .putString(SEARCH_HISTORY_PREFS_KEY, history.take(MAX_SEARCH_HISTORY_ITEMS).joinToString("\n"))
            .apply()
    }

    fun saveUpdateIntervalMs(value: Long) {
        prefs.edit()
            .putLong(MockLocationService.KEY_UPDATE_INTERVAL_MS, value)
            .apply()
    }

    fun saveWakeDurationMs(value: Long) {
        prefs.edit()
            .putLong(MockLocationService.KEY_WAKE_DURATION_MS, value)
            .apply()
    }

    fun markLocationPermissionRequested() {
        prefs.edit()
            .putBoolean(LOCATION_PERMISSION_REQUESTED_PREFS_KEY, true)
            .apply()
    }

    fun markNotificationPermissionRequested() {
        prefs.edit()
            .putBoolean(NOTIFICATION_PERMISSION_REQUESTED_PREFS_KEY, true)
            .apply()
    }

    fun markAgreementAccepted() {
        prefs.edit()
            .putBoolean(AGREEMENT_ACCEPTED_PREFS_KEY, true)
            .apply()
    }

    fun markPermissionGuideCompleted() {
        prefs.edit()
            .putBoolean(PERMISSION_GUIDE_COMPLETED_PREFS_KEY, true)
            .apply()
    }

    companion object {
        const val MAX_SEARCH_HISTORY_ITEMS = 8
        private const val SEARCH_HISTORY_PREFS_KEY = "search_history"
        private const val LOCATION_PERMISSION_REQUESTED_PREFS_KEY = "location_permission_requested"
        private const val NOTIFICATION_PERMISSION_REQUESTED_PREFS_KEY = "notification_permission_requested"
        private const val AGREEMENT_ACCEPTED_PREFS_KEY = "agreement_accepted"
        private const val PERMISSION_GUIDE_COMPLETED_PREFS_KEY = "permission_guide_completed"
    }
}
