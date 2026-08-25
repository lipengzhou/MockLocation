package com.lipengzhou.mocklocation.viewmodel

import android.app.Application
import android.location.LocationManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lipengzhou.mocklocation.data.MockLocationPreferences
import com.lipengzhou.mocklocation.location.MockLocationService
import com.lipengzhou.mocklocation.location.MockLocationSystemController
import com.lipengzhou.mocklocation.map.Coordinate
import com.lipengzhou.mocklocation.map.CoordinateConverter
import com.lipengzhou.mocklocation.map.CoordinateInputSystem
import com.lipengzhou.mocklocation.map.MapSearchRepository
import com.lipengzhou.mocklocation.map.MapSearchResult
import com.lipengzhou.mocklocation.map.formatCoordinate
import com.lipengzhou.mocklocation.state.AppPage
import com.lipengzhou.mocklocation.state.DiagnosticUiState
import com.lipengzhou.mocklocation.state.MockLocationUiState
import com.lipengzhou.mocklocation.state.PermissionUiState
import com.lipengzhou.mocklocation.state.SearchUiState
import com.lipengzhou.mocklocation.state.StartMockAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MockLocationViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val preferences = MockLocationPreferences(application)
    private val systemController = MockLocationSystemController(application)
    private val searchRepository = MapSearchRepository(application)

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<MockLocationUiState> = _uiState.asStateFlow()

    private val _startMockAction = MutableSharedFlow<StartMockAction>()
    val startMockAction: SharedFlow<StartMockAction> = _startMockAction.asSharedFlow()

    private var searchJob: Job? = null
    private var reverseGeocodeJob: Job? = null

    fun refreshRuntimeState() {
        _uiState.update { state ->
            state.copy(
                isRunning = preferences.savedServiceRunningState(),
                statusText = preferences.savedStatusMessage(),
                permissions = currentPermissions(),
                diagnostics = currentDiagnostics()
            )
        }
    }

    fun onServiceStatusChanged(isRunning: Boolean, statusMessage: String?) {
        _uiState.update { state ->
            state.copy(
                isRunning = isRunning,
                statusText = statusMessage ?: state.statusText,
                permissions = state.permissions.copy(
                    hasMockLocationPermission = systemController.canUseMockLocation()
                ),
                diagnostics = currentDiagnostics()
            )
        }
    }

    fun onPermissionsResult() {
        val permissions = currentPermissions()
        val missingRuntimePermissions = buildList {
            if (!permissions.hasLocationPermission) {
                add("定位权限")
            }
            if (!permissions.hasNotificationPermission) {
                add("通知权限")
            }
        }
        _uiState.update { state ->
            state.copy(
                permissions = permissions,
                statusText = if (missingRuntimePermissions.isEmpty()) {
                    "运行时权限已就绪。"
                } else {
                    "${missingRuntimePermissions.joinToString("、")}仍未授权；如果系统没有弹窗，请在应用设置中手动开启。"
                }
            )
        }
    }

    fun onRuntimePermissionRequestStarted() {
        preferences.markLocationPermissionRequested()
        preferences.markNotificationPermissionRequested()
        _uiState.update {
            it.copy(statusText = "正在请求定位和通知权限...")
        }
    }

    fun onLocationPermissionRequestStarted() {
        preferences.markLocationPermissionRequested()
        _uiState.update {
            it.copy(statusText = "正在请求定位权限...")
        }
    }

    fun onNotificationPermissionRequestStarted() {
        preferences.markNotificationPermissionRequested()
        _uiState.update {
            it.copy(statusText = "正在请求通知权限...")
        }
    }

    fun onRuntimePermissionSettingsRequired(permissionName: String) {
        _uiState.update { state ->
            state.copy(
                permissions = currentPermissions(),
                statusText = "${permissionName}仍未授权，已打开应用设置，请在权限中手动开启。"
            )
        }
    }

    fun selectPage(page: AppPage) {
        _uiState.update { it.copy(selectedPage = page) }
    }

    fun acceptAgreement() {
        preferences.markAgreementAccepted()
        _uiState.update {
            it.copy(
                hasAcceptedAgreement = true,
                selectedPage = AppPage.PermissionGuide
            )
        }
    }

    fun completePermissionGuide() {
        val permissions = currentPermissions()
        if (!permissions.requiredPermissionsReady) {
            _uiState.update { state ->
                state.copy(
                    permissions = permissions,
                    statusText = "请先完成必需权限配置。"
                )
            }
            return
        }

        preferences.markPermissionGuideCompleted()
        _uiState.update { state ->
            state.copy(
                hasCompletedPermissionGuide = true,
                selectedPage = AppPage.Map,
                permissions = permissions,
                diagnostics = currentDiagnostics(),
                statusText = "权限配置已完成。"
            )
        }
    }

    fun openSearchPage() {
        searchJob?.cancel()
        _uiState.update { state ->
            state.copy(
                showSearchPage = true,
                search = state.search.copy(
                    keyword = "",
                    statusText = "",
                    isSearching = false,
                    results = emptyList()
                )
            )
        }
    }

    fun closeSearchPage() {
        searchJob?.cancel()
        _uiState.update { it.copy(showSearchPage = false) }
    }

    fun onSearchKeywordChange(value: String) {
        searchJob?.cancel()
        _uiState.update { state ->
            state.copy(
                search = state.search.copy(
                    keyword = value,
                    statusText = "",
                    isSearching = false,
                    results = emptyList()
                )
            )
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { state ->
            state.copy(
                search = state.search.copy(
                    keyword = "",
                    statusText = "",
                    isSearching = false,
                    results = emptyList()
                )
            )
        }
    }

    fun deleteSearchHistory(item: String) {
        val nextHistory = _uiState.value.search.history.filterNot { it == item }
        preferences.saveSearchHistory(nextHistory)
        _uiState.update { state ->
            state.copy(search = state.search.copy(history = nextHistory))
        }
    }

    fun searchPoi(requestedKeyword: String) {
        val keyword = requestedKeyword.trim()
        searchJob?.cancel()
        if (keyword.isBlank()) {
            _uiState.update { state ->
                state.copy(
                    search = state.search.copy(
                        isSearching = false,
                        results = emptyList(),
                        statusText = ""
                    )
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                search = state.search.copy(
                    keyword = keyword,
                    isSearching = true,
                    statusText = "正在搜索..."
                )
            )
        }
        searchJob = viewModelScope.launch {
            val response = searchRepository.search(keyword)
            _uiState.update { state ->
                if (response.keyword != state.search.keyword.trim()) {
                    state
                } else {
                    state.copy(
                        search = state.search.copy(
                            isSearching = false,
                            results = response.results,
                            statusText = response.message
                        )
                    )
                }
            }
        }
    }

    fun selectSearchResult(result: MapSearchResult) {
        val nextHistory = (listOf(result.title) + _uiState.value.search.history)
            .distinct()
            .take(MockLocationPreferences.MAX_SEARCH_HISTORY_ITEMS)
        preferences.saveSearchHistory(nextHistory)
        selectMapPoint(
            gcj02 = result.coordinate,
            selectedMapText = result.displayText(),
            message = "已从搜索结果回填 WGS84 坐标。"
        )
        _uiState.update { state ->
            state.copy(
                showSearchPage = false,
                search = state.search.copy(
                    keyword = result.title,
                    statusText = "",
                    isSearching = false,
                    results = emptyList(),
                    history = nextHistory
                )
            )
        }
    }

    fun onMapPointSelected(gcj02: Coordinate, wgs84: Coordinate) {
        updateSelectedMapPoint(
            gcj02 = gcj02,
            wgs84 = wgs84,
            selectedMapText = "正在解析位置...",
            message = "已从地图选点并回填 WGS84 坐标。"
        )
        reverseGeocodeMapPoint(gcj02)
    }

    fun onCoordinateInputConfirmed(
        longitude: String,
        latitude: String,
        coordinateSystem: CoordinateInputSystem,
    ): Boolean {
        val lon = longitude.trim().toDoubleOrNull()
        val lat = latitude.trim().toDoubleOrNull()
        if (lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
            _uiState.update { it.copy(statusText = "经纬度格式不正确。") }
            return false
        }

        val gcj02 = when (coordinateSystem) {
            CoordinateInputSystem.BD09 -> CoordinateConverter.bd09ToGcj02(
                latitude = lat,
                longitude = lon
            )
            CoordinateInputSystem.GPS -> CoordinateConverter.wgs84ToGcj02(
                latitude = lat,
                longitude = lon
            )
        }
        val wgs84 = when (coordinateSystem) {
            CoordinateInputSystem.BD09 -> CoordinateConverter.gcj02ToWgs84(
                latitude = gcj02.latitude,
                longitude = gcj02.longitude
            )
            CoordinateInputSystem.GPS -> Coordinate(
                latitude = lat,
                longitude = lon
            )
        }

        updateSelectedMapPoint(
            gcj02 = gcj02,
            wgs84 = wgs84,
            selectedMapText = "正在解析位置...",
            message = "已从坐标输入回填 WGS84 坐标。"
        )
        reverseGeocodeMapPoint(gcj02)
        return true
    }

    fun locateCurrentPosition() {
        if (!systemController.hasLocationPermission()) {
            _uiState.update {
                it.copy(
                    statusText = "需要先授予定位权限。",
                    permissions = currentPermissions()
                )
            }
            emitStartAction(StartMockAction.RequestPermissions)
            return
        }

        _uiState.update {
            it.copy(
                statusText = "正在定位当前位置...",
                permissions = currentPermissions(),
                diagnostics = currentDiagnostics()
            )
        }
        viewModelScope.launch {
            val location = systemController.getCurrentLocation()
            if (location == null) {
                _uiState.update {
                    it.copy(
                        statusText = "暂时没有可用的当前位置，请确认系统定位已开启后稍后再试。",
                        permissions = currentPermissions(),
                        diagnostics = currentDiagnostics()
                    )
                }
                return@launch
            }

            val wgs84 = Coordinate(
                latitude = location.latitude,
                longitude = location.longitude
            )
            val gcj02 = CoordinateConverter.wgs84ToGcj02(
                latitude = wgs84.latitude,
                longitude = wgs84.longitude
            )
            updateSelectedMapPoint(
                gcj02 = gcj02,
                wgs84 = wgs84,
                selectedMapText = "当前位置",
                message = "已定位到当前位置并回填 WGS84 坐标。"
            )
        }
    }

    private fun reverseGeocodeMapPoint(gcj02: Coordinate) {
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = viewModelScope.launch {
            val displayText = searchRepository.reverseGeocode(gcj02) ?: "地图选点位置"
            _uiState.update { state ->
                if (state.selectedCoordinate != gcj02) {
                    state
                } else {
                    state.copy(selectedMapText = displayText)
                }
            }
        }
    }

    fun onLatitudeChange(value: String) {
        _uiState.update { it.copy(latitude = value) }
    }

    fun onLongitudeChange(value: String) {
        _uiState.update { it.copy(longitude = value) }
    }

    fun onAltitudeChange(value: String) {
        _uiState.update { it.copy(altitude = value) }
    }

    fun onUpdateIntervalChange(value: Long) {
        preferences.saveUpdateIntervalMs(value)
        _uiState.update { state ->
            state.copy(
                statusText = "注入间隔已设置为 ${value}ms。",
                diagnostics = state.diagnostics.copy(updateIntervalMs = value)
            )
        }
    }

    fun onWakeDurationChange(value: Long) {
        preferences.saveWakeDurationMs(value)
        _uiState.update { state ->
            state.copy(
                statusText = "停止后真实定位恢复时长已设置为 ${value / 1000} 秒。",
                diagnostics = state.diagnostics.copy(wakeDurationMs = value)
            )
        }
    }

    fun startMocking() {
        val state = _uiState.value
        val lat = state.latitude.toDoubleOrNull()
        val lon = state.longitude.toDoubleOrNull()
        val alt = state.altitude.toDoubleOrNull() ?: MockLocationService.DEFAULT_ALTITUDE
        when {
            !systemController.hasLocationPermission() -> {
                _uiState.update {
                    it.copy(
                        statusText = "需要先授予定位权限。",
                        permissions = currentPermissions()
                    )
                }
                emitStartAction(StartMockAction.RequestPermissions)
            }

            !systemController.hasNotificationPermission() -> {
                _uiState.update {
                    it.copy(
                        statusText = "需要先授予通知权限。",
                        permissions = currentPermissions()
                    )
                }
                emitStartAction(StartMockAction.RequestPermissions)
            }

            !systemController.canUseMockLocation() -> {
                _uiState.update {
                    it.copy(
                        statusText = "请先在开发者选项中将本应用设置为模拟位置信息应用。",
                        permissions = currentPermissions()
                    )
                }
            }

            lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0 -> {
                _uiState.update { it.copy(statusText = "经纬度格式不正确。") }
            }

            else -> {
                _uiState.update {
                    it.copy(
                        statusText = "正在启动模拟定位...",
                        permissions = currentPermissions()
                    )
                }
                systemController.startMockLocationService(lat, lon, alt)
            }
        }
    }

    fun stopMocking() {
        systemController.stopMockLocationService()
        _uiState.update {
            it.copy(
                isRunning = false,
                statusText = "正在停止模拟定位..."
            )
        }
    }

    fun copyDiagnostics() {
        val state = _uiState.value
        val diagnosticText = buildDiagnosticText(
            isRunning = state.isRunning,
            statusText = state.statusText,
            providerNames = state.diagnostics.providerNames,
            updateCount = state.diagnostics.updateCount,
            updateIntervalMs = state.diagnostics.updateIntervalMs,
            wakeDurationMs = state.diagnostics.wakeDurationMs,
            lastStopTime = state.diagnostics.lastStopTime,
            lastError = state.diagnostics.lastError,
            hasGpsProvider = systemController.isProviderEnabled(LocationManager.GPS_PROVIDER),
            hasNetworkProvider = systemController.isProviderEnabled(LocationManager.NETWORK_PROVIDER),
            hasLocationPermission = systemController.hasLocationPermission(),
            hasNotificationPermission = systemController.hasNotificationPermission(),
            hasMockLocationPermission = systemController.canUseMockLocation()
        )
        systemController.copyToClipboard(diagnosticText)
        _uiState.update {
            it.copy(
                statusText = "诊断信息已复制。",
                permissions = currentPermissions(),
                diagnostics = currentDiagnostics()
            )
        }
    }

    private fun selectMapPoint(
        gcj02: Coordinate,
        selectedMapText: String,
        message: String,
    ) {
        updateSelectedMapPoint(
            gcj02 = gcj02,
            wgs84 = CoordinateConverter.gcj02ToWgs84(
                latitude = gcj02.latitude,
                longitude = gcj02.longitude
            ),
            selectedMapText = selectedMapText,
            message = message
        )
    }

    private fun updateSelectedMapPoint(
        gcj02: Coordinate,
        wgs84: Coordinate,
        selectedMapText: String,
        message: String,
    ) {
        _uiState.update { state ->
            state.copy(
                selectedCoordinate = gcj02,
                latitude = formatCoordinate(wgs84.latitude),
                longitude = formatCoordinate(wgs84.longitude),
                selectedMapText = selectedMapText,
                statusText = message
            )
        }
    }

    private fun createInitialState(): MockLocationUiState {
        val latitude = MockLocationService.DEFAULT_LATITUDE.toString()
        val longitude = MockLocationService.DEFAULT_LONGITUDE.toString()
        return MockLocationUiState(
            latitude = latitude,
            longitude = longitude,
            altitude = MockLocationService.DEFAULT_ALTITUDE.toString(),
            statusText = preferences.savedStatusMessage(),
            isRunning = preferences.savedServiceRunningState(),
            permissions = currentPermissions(),
            diagnostics = currentDiagnostics(),
            selectedCoordinate = Coordinate(
                latitude = latitude.toDoubleOrNull() ?: MockLocationService.DEFAULT_LATITUDE,
                longitude = longitude.toDoubleOrNull() ?: MockLocationService.DEFAULT_LONGITUDE
            ),
            search = SearchUiState(history = preferences.savedSearchHistory()),
            hasAcceptedAgreement = preferences.savedAgreementAccepted(),
            hasCompletedPermissionGuide = preferences.savedPermissionGuideCompleted()
        )
    }

    private fun currentPermissions(): PermissionUiState =
        PermissionUiState(
            hasLocationPermission = systemController.hasLocationPermission(),
            hasNotificationPermission = systemController.hasNotificationPermission(),
            hasMockLocationPermission = systemController.canUseMockLocation(),
            isSystemLocationEnabled = systemController.isSystemLocationEnabled(),
            canDrawOverlays = systemController.canDrawOverlays(),
            isIgnoringBatteryOptimizations = systemController.isIgnoringBatteryOptimizations(),
            hasRequestedLocationPermission = preferences.savedLocationPermissionRequested(),
            hasRequestedNotificationPermission = preferences.savedNotificationPermissionRequested()
        )

    private fun currentDiagnostics(): DiagnosticUiState =
        DiagnosticUiState(
            providerNames = preferences.savedProviderNames(),
            updateCount = preferences.savedUpdateCount(),
            updateIntervalMs = preferences.savedUpdateIntervalMs(),
            wakeDurationMs = preferences.savedWakeDurationMs(),
            lastStopTime = preferences.savedLastStopTimeText(),
            lastError = preferences.savedLastError(),
            hasGpsProvider = systemController.isProviderEnabled(LocationManager.GPS_PROVIDER),
            hasNetworkProvider = systemController.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        )

    private fun emitStartAction(action: StartMockAction) {
        viewModelScope.launch {
            _startMockAction.emit(action)
        }
    }

    private fun buildDiagnosticText(
        isRunning: Boolean,
        statusText: String,
        providerNames: String,
        updateCount: Long,
        updateIntervalMs: Long,
        wakeDurationMs: Long,
        lastStopTime: String,
        lastError: String,
        hasGpsProvider: Boolean,
        hasNetworkProvider: Boolean,
        hasLocationPermission: Boolean,
        hasNotificationPermission: Boolean,
        hasMockLocationPermission: Boolean,
    ): String = buildString {
        appendLine("服务状态：${if (isRunning) "运行中" else "已停止"}")
        appendLine("状态消息：$statusText")
        appendLine("当前通道：$providerNames")
        appendLine("注入次数：$updateCount")
        appendLine("注入间隔：${updateIntervalMs}ms")
        appendLine("停止恢复：${if (wakeDurationMs == 0L) "关闭" else "${wakeDurationMs / 1000}秒"}")
        appendLine("最近停止：$lastStopTime")
        appendLine("最近错误：$lastError")
        appendLine("GPS Provider：${if (hasGpsProvider) "已开启" else "未开启"}")
        appendLine("网络 Provider：${if (hasNetworkProvider) "已开启" else "未开启"}")
        appendLine("定位权限：${if (hasLocationPermission) "已就绪" else "待处理"}")
        appendLine("通知权限：${if (hasNotificationPermission) "已就绪" else "待处理"}")
        appendLine("模拟位置应用：${if (hasMockLocationPermission) "已就绪" else "待处理"}")
        appendLine("系统定位开关：${if (systemController.isSystemLocationEnabled()) "已开启" else "未开启"}")
        appendLine("悬浮窗权限：${if (systemController.canDrawOverlays()) "已就绪" else "待处理"}")
        appendLine("电池优化例外：${if (systemController.isIgnoringBatteryOptimizations()) "已设置" else "未设置"}")
    }

    private fun MapSearchResult.displayText(): String =
        listOf(title, address.takeIf { it.isNotBlank() && it != title })
            .filterNotNull()
            .joinToString(" · ")
}
