package com.lipengzhou.mocklocation.state

import com.lipengzhou.mocklocation.location.MockLocationService
import com.lipengzhou.mocklocation.map.Coordinate
import com.lipengzhou.mocklocation.map.MapSearchResult

enum class AppPage {
    Map,
    PermissionGuide,
    Configuration,
}

data class MockLocationUiState(
    val latitude: String = MockLocationService.DEFAULT_LATITUDE.toString(),
    val longitude: String = MockLocationService.DEFAULT_LONGITUDE.toString(),
    val altitude: String = MockLocationService.DEFAULT_ALTITUDE.toString(),
    val statusText: String = "准备就绪",
    val isRunning: Boolean = false,
    val permissions: PermissionUiState = PermissionUiState(),
    val diagnostics: DiagnosticUiState = DiagnosticUiState(),
    val selectedMapText: String = "尚未选择位置",
    val selectedCoordinate: Coordinate = Coordinate(
        latitude = MockLocationService.DEFAULT_LATITUDE,
        longitude = MockLocationService.DEFAULT_LONGITUDE
    ),
    val selectedPage: AppPage = AppPage.Map,
    val showSearchPage: Boolean = false,
    val search: SearchUiState = SearchUiState(),
    val update: AppUpdateUiState = AppUpdateUiState(),
    val hasAcceptedAgreement: Boolean = false,
    val hasCompletedPermissionGuide: Boolean = false,
)

data class PermissionUiState(
    val hasLocationPermission: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val hasMockLocationPermission: Boolean = false,
    val isSystemLocationEnabled: Boolean = false,
    val canDrawOverlays: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val hasRequestedLocationPermission: Boolean = false,
    val hasRequestedNotificationPermission: Boolean = false,
) {
    val requiredPermissionsReady: Boolean
        get() = hasLocationPermission &&
            hasNotificationPermission &&
            hasMockLocationPermission &&
            isSystemLocationEnabled
}

data class DiagnosticUiState(
    val providerNames: String = "无",
    val updateCount: Long = 0L,
    val updateIntervalMs: Long = MockLocationService.DEFAULT_UPDATE_INTERVAL_MS,
    val wakeDurationMs: Long = MockLocationService.DEFAULT_WAKE_DURATION_MS,
    val lastStopTime: String = "无",
    val lastError: String = "无",
    val hasGpsProvider: Boolean = false,
    val hasNetworkProvider: Boolean = false,
)

data class SearchUiState(
    val keyword: String = "",
    val statusText: String = "",
    val isSearching: Boolean = false,
    val results: List<MapSearchResult> = emptyList(),
    val history: List<String> = emptyList(),
)

data class AppUpdateUiState(
    val currentVersionName: String = "",
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadedFileName: String = "",
    val pendingInstallFileName: String = "",
    val availableRelease: AvailableAppUpdate? = null,
    val shouldShowPrompt: Boolean = false,
    val message: String = "",
) {
    val hasAvailableUpdate: Boolean
        get() = availableRelease != null

    val isWaitingForInstallPermission: Boolean
        get() = pendingInstallFileName.isNotBlank()
}

data class AvailableAppUpdate(
    val tagName: String,
    val versionName: String,
    val title: String,
    val downloadUrl: String,
    val assetName: String,
    val assetSizeBytes: Long,
    val releaseNotes: String,
)

enum class StartMockAction {
    None,
    RequestPermissions,
}
