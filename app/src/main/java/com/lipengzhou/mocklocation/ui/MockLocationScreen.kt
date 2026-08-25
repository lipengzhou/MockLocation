package com.lipengzhou.mocklocation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lipengzhou.mocklocation.location.MockLocationService
import com.lipengzhou.mocklocation.map.AMapPicker
import com.lipengzhou.mocklocation.map.Coordinate
import com.lipengzhou.mocklocation.map.CoordinateInputSystem
import com.lipengzhou.mocklocation.map.MapSearchResult
import com.lipengzhou.mocklocation.state.AppPage
import com.lipengzhou.mocklocation.state.MockLocationUiState
import com.lipengzhou.mocklocation.ui.theme.MockLocationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val AppDrawerMaxWidth = 304.dp
private const val AppDrawerScreenWidthFraction = 0.84f

@Composable
fun MockLocationScreen(
    modifier: Modifier = Modifier,
    uiState: MockLocationUiState = MockLocationUiState(),
    onPageSelected: (AppPage) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSearchKeywordChange: (String) -> Unit = {},
    onSearchKeyword: (String) -> Unit = {},
    onSearchClear: () -> Unit = {},
    onSearchBack: () -> Unit = {},
    onSearchResultSelected: (MapSearchResult) -> Unit = {},
    onSearchHistoryDelete: (String) -> Unit = {},
    onPointSelected: (gcj02: Coordinate, wgs84: Coordinate) -> Unit = { _, _ -> },
    onCoordinateInputConfirmed: (
        longitude: String,
        latitude: String,
        coordinateSystem: CoordinateInputSystem,
    ) -> Boolean = { _, _, _ -> false },
    onLocateCurrentPosition: () -> Unit = {},
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onRequestPermissions: () -> Unit = {},
    onRequestLocationPermission: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onOpenDeveloperSettings: () -> Unit = {},
    onOpenApplicationSettings: () -> Unit = {},
    onOpenLocationSettings: () -> Unit = {},
    onRefreshRuntimeState: () -> Unit = {},
    onCopyDiagnostics: () -> Unit = {},
    onLatitudeChange: (String) -> Unit = {},
    onLongitudeChange: (String) -> Unit = {},
    onAltitudeChange: (String) -> Unit = {},
    onUpdateIntervalChange: (Long) -> Unit = {},
    onWakeDurationChange: (Long) -> Unit = {},
    onAgreementAccepted: () -> Unit = {},
    onPermissionGuideCompleted: () -> Unit = {},
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val shouldShowPermissionGuideOnly =
        !uiState.hasCompletedPermissionGuide || !uiState.permissions.requiredPermissionsReady

    if (!uiState.hasAcceptedAgreement) {
        AgreementPage(
            modifier = modifier.fillMaxSize(),
            onAgreementAccepted = onAgreementAccepted
        )
        return
    }

    if (shouldShowPermissionGuideOnly) {
        PermissionGuidePage(
            hasLocationPermission = uiState.permissions.hasLocationPermission,
            hasNotificationPermission = uiState.permissions.hasNotificationPermission,
            hasMockLocationPermission = uiState.permissions.hasMockLocationPermission,
            isSystemLocationEnabled = uiState.permissions.isSystemLocationEnabled,
            hasRequestedLocationPermission = uiState.permissions.hasRequestedLocationPermission,
            hasRequestedNotificationPermission = uiState.permissions.hasRequestedNotificationPermission,
            statusText = uiState.statusText,
            showMenuButton = false,
            onMenuClick = {},
            onRequestLocationPermission = onRequestLocationPermission,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onOpenDeveloperSettings = onOpenDeveloperSettings,
            onOpenApplicationSettings = onOpenApplicationSettings,
            onOpenLocationSettings = onOpenLocationSettings,
            onRefreshRuntimeState = onRefreshRuntimeState,
            onEnterApp = onPermissionGuideCompleted,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        gesturesEnabled = uiState.selectedPage != AppPage.Map || drawerState.isOpen,
        drawerContent = {
            AppDrawerContent(
                selectedPage = uiState.selectedPage,
                onPageSelected = { page ->
                    onPageSelected(page)
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        when (uiState.selectedPage) {
            AppPage.Map -> MapHomePage(
                latitude = uiState.latitude.toDoubleOrNull() ?: MockLocationService.DEFAULT_LATITUDE,
                longitude = uiState.longitude.toDoubleOrNull() ?: MockLocationService.DEFAULT_LONGITUDE,
                selectedCoordinate = uiState.selectedCoordinate,
                selectedMapText = uiState.selectedMapText,
                statusText = uiState.statusText,
                isRunning = uiState.isRunning,
                onMenuClick = {
                    scope.launch { drawerState.open() }
                },
                onSearchClick = onSearchClick,
                onPointSelected = onPointSelected,
                onCoordinateInputConfirmed = onCoordinateInputConfirmed,
                onLocateCurrentPosition = onLocateCurrentPosition,
                onStart = onStart,
                onStop = onStop,
                modifier = Modifier.fillMaxSize()
            )

            AppPage.PermissionGuide -> PermissionGuidePage(
                hasLocationPermission = uiState.permissions.hasLocationPermission,
                hasNotificationPermission = uiState.permissions.hasNotificationPermission,
                hasMockLocationPermission = uiState.permissions.hasMockLocationPermission,
                isSystemLocationEnabled = uiState.permissions.isSystemLocationEnabled,
                hasRequestedLocationPermission = uiState.permissions.hasRequestedLocationPermission,
                hasRequestedNotificationPermission = uiState.permissions.hasRequestedNotificationPermission,
                statusText = uiState.statusText,
                showMenuButton = true,
                onMenuClick = {
                    scope.launch { drawerState.open() }
                },
                onRequestLocationPermission = onRequestLocationPermission,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onOpenDeveloperSettings = onOpenDeveloperSettings,
                onOpenApplicationSettings = onOpenApplicationSettings,
                onOpenLocationSettings = onOpenLocationSettings,
                onRefreshRuntimeState = onRefreshRuntimeState,
                onEnterApp = onPermissionGuideCompleted,
                modifier = Modifier.fillMaxSize()
            )

            AppPage.Configuration -> ConfigurationPage(
                latitude = uiState.latitude,
                longitude = uiState.longitude,
                altitude = uiState.altitude,
                statusText = uiState.statusText,
                isRunning = uiState.isRunning,
                hasLocationPermission = uiState.permissions.hasLocationPermission,
                hasNotificationPermission = uiState.permissions.hasNotificationPermission,
                hasMockLocationPermission = uiState.permissions.hasMockLocationPermission,
                providerNames = uiState.diagnostics.providerNames,
                updateCount = uiState.diagnostics.updateCount,
                updateIntervalMs = uiState.diagnostics.updateIntervalMs,
                wakeDurationMs = uiState.diagnostics.wakeDurationMs,
                lastStopTime = uiState.diagnostics.lastStopTime,
                lastError = uiState.diagnostics.lastError,
                hasGpsProvider = uiState.diagnostics.hasGpsProvider,
                hasNetworkProvider = uiState.diagnostics.hasNetworkProvider,
                onMenuClick = {
                    scope.launch { drawerState.open() }
                },
                onLatitudeChange = onLatitudeChange,
                onLongitudeChange = onLongitudeChange,
                onAltitudeChange = onAltitudeChange,
                onUpdateIntervalChange = onUpdateIntervalChange,
                onWakeDurationChange = onWakeDurationChange,
                onStart = onStart,
                onStop = onStop,
                onRequestPermissions = onRequestPermissions,
                onOpenDeveloperSettings = onOpenDeveloperSettings,
                onOpenApplicationSettings = onOpenApplicationSettings,
                onCopyDiagnostics = onCopyDiagnostics,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (uiState.showSearchPage) {
        SearchPage(
            keyword = uiState.search.keyword,
            statusText = uiState.search.statusText,
            isSearching = uiState.search.isSearching,
            searchResults = uiState.search.results,
            searchHistory = uiState.search.history,
            onKeywordChange = onSearchKeywordChange,
            onSearchKeyword = onSearchKeyword,
            onClear = onSearchClear,
            onBack = onSearchBack,
            onResultSelected = onSearchResultSelected,
            onHistoryDelete = onSearchHistoryDelete,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun AgreementPage(
    onAgreementAccepted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hasCheckedAgreement by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "用户协议和隐私政策",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "请先阅读并同意以下说明后继续使用模拟定位功能。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AgreementParagraph(
                        index = 1,
                        text = "本应用用于在用户授权的 Android 开发者选项环境下注入模拟位置信息，仅供调试、测试或个人合法用途。"
                    )
                    AgreementParagraph(
                        index = 2,
                        text = "使用前需自行确认所在地法律法规、目标应用或服务的使用规则，因违反规则或不当使用造成的责任由用户自行承担。"
                    )
                    AgreementParagraph(
                        index = 3,
                        text = "本应用会请求定位、通知等必要权限；定位数据仅用于本机地图选点、当前位置获取和模拟定位注入，不会上传到开发者服务器。"
                    )
                    AgreementParagraph(
                        index = 4,
                        text = "搜索关键词、位置和运行状态等数据仅保存在本机，用于改善本地使用体验；卸载应用后相关本地数据将随系统机制清除。"
                    )
                    AgreementParagraph(
                        index = 5,
                        text = "勾选即表示已阅读并同意以上用户协议和隐私说明。"
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = hasCheckedAgreement,
                        role = Role.Checkbox,
                        onValueChange = { hasCheckedAgreement = it }
                    )
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = hasCheckedAgreement,
                    onCheckedChange = null
                )
                Text(
                    text = "已阅读《用户协议和隐私政策》",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = hasCheckedAgreement,
                onClick = onAgreementAccepted
            ) {
                Text("进入应用")
            }
        }
    }
}

@Composable
private fun AgreementParagraph(
    index: Int,
    text: String,
) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = "$index. $text",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AppDrawerContent(
    selectedPage: AppPage,
    onPageSelected: (AppPage) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val drawerWidth = (configuration.screenWidthDp.dp * AppDrawerScreenWidthFraction)
        .coerceAtMost(AppDrawerMaxWidth)

    ModalDrawerSheet(
        modifier = Modifier.width(drawerWidth),
        drawerShape = RectangleShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "模拟定位",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            NavigationDrawerItem(
                selected = selectedPage == AppPage.Map,
                onClick = { onPageSelected(AppPage.Map) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null
                    )
                },
                label = { Text("地图选点") }
            )
            NavigationDrawerItem(
                selected = selectedPage == AppPage.PermissionGuide,
                onClick = { onPageSelected(AppPage.PermissionGuide) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null
                    )
                },
                label = { Text("权限引导") }
            )
            NavigationDrawerItem(
                selected = selectedPage == AppPage.Configuration,
                onClick = { onPageSelected(AppPage.Configuration) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null
                    )
                },
                label = { Text("配置与诊断") }
            )
        }
    }
}

@Composable
private fun MapHomePage(
    latitude: Double,
    longitude: Double,
    selectedCoordinate: Coordinate,
    selectedMapText: String,
    statusText: String,
    isRunning: Boolean,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit,
    onPointSelected: (gcj02: Coordinate, wgs84: Coordinate) -> Unit,
    onCoordinateInputConfirmed: (
        longitude: String,
        latitude: String,
        coordinateSystem: CoordinateInputSystem,
    ) -> Boolean,
    onLocateCurrentPosition: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCoordinateInputDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        AMapPicker(
            modifier = Modifier.fillMaxSize(),
            initialLatitude = latitude,
            initialLongitude = longitude,
            selectedCoordinate = selectedCoordinate,
            zoomControlsBottomPadding = 188.dp,
            onCoordinateInputClick = { showCoordinateInputDialog = true },
            onLocateCurrentPosition = onLocateCurrentPosition,
            onPointSelected = onPointSelected
        )

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, top = 8.dp, end = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onMenuClick) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "打开菜单"
                        )
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onSearchClick
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = "搜索地点"
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "当前选择",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = selectedMapText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = if (isRunning) "运行中" else "已停止",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isRunning) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = !isRunning,
                        onClick = onStart
                    ) {
                        Text("开始")
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onStop
                    ) {
                        Text("停止")
                    }
                }
            }
        }
    }

    if (showCoordinateInputDialog) {
        CoordinateInputDialog(
            onDismiss = { showCoordinateInputDialog = false },
            onConfirm = { inputLongitude, inputLatitude, coordinateSystem ->
                val accepted = onCoordinateInputConfirmed(
                    inputLongitude,
                    inputLatitude,
                    coordinateSystem
                )
                if (accepted) {
                    showCoordinateInputDialog = false
                }
                accepted
            }
        )
    }
}

@Composable
private fun CoordinateInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        longitude: String,
        latitude: String,
        coordinateSystem: CoordinateInputSystem,
    ) -> Boolean,
) {
    var longitude by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var coordinateSystem by remember { mutableStateOf(CoordinateInputSystem.GPS) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "输入坐标",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = longitude,
                    onValueChange = {
                        longitude = it
                        showError = false
                    },
                    placeholder = { Text("经度") },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true,
                    isError = showError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = latitude,
                    onValueChange = {
                        latitude = it
                        showError = false
                    },
                    placeholder = { Text("纬度") },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    singleLine = true,
                    isError = showError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                if (showError) {
                    Text(
                        text = "请输入有效的经度和纬度。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoordinateSystemOption(
                        text = "BD09 坐标系",
                        selected = coordinateSystem == CoordinateInputSystem.BD09,
                        onClick = { coordinateSystem = CoordinateInputSystem.BD09 }
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    CoordinateSystemOption(
                        text = "GPS 坐标系",
                        selected = coordinateSystem == CoordinateInputSystem.GPS,
                        onClick = { coordinateSystem = CoordinateInputSystem.GPS }
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("取消")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val accepted = onConfirm(longitude, latitude, coordinateSystem)
                        if (!accepted) {
                            showError = true
                        }
                    }
                ) {
                    Text("确定")
                }
            }
        }
    )
}

@Composable
private fun CoordinateSystemOption(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(40.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            softWrap = false
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SearchPage(
    keyword: String,
    statusText: String,
    isSearching: Boolean,
    searchResults: List<MapSearchResult>,
    searchHistory: List<String>,
    onKeywordChange: (String) -> Unit,
    onSearchKeyword: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onResultSelected: (MapSearchResult) -> Unit,
    onHistoryDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val trimmedKeyword = keyword.trim()
    val closeSearch = {
        keyboardController?.hide()
        onBack()
    }

    BackHandler(onBack = closeSearch)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(trimmedKeyword) {
        if (trimmedKeyword.isBlank()) {
            return@LaunchedEffect
        }
        delay(350)
        onSearchKeyword(trimmedKeyword)
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SearchHeader(
                keyword = keyword,
                focusRequester = focusRequester,
                onKeywordChange = onKeywordChange,
                onClear = onClear,
                onBack = closeSearch
            )

            if (trimmedKeyword.isBlank()) {
                SearchHistorySection(
                    history = searchHistory,
                    onHistorySelected = onKeywordChange,
                    onHistoryDelete = onHistoryDelete
                )
            } else {
                SearchResultList(
                    statusText = statusText,
                    isSearching = isSearching,
                    results = searchResults,
                    onResultSelected = { result ->
                        keyboardController?.hide()
                        onResultSelected(result)
                    }
                )
            }
        }
    }
}

@Composable
private fun SearchHeader(
    keyword: String,
    focusRequester: FocusRequester,
    onKeywordChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(26.dp),
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回地图"
                )
            }
            BasicTextField(
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                value = keyword,
                onValueChange = onKeywordChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (keyword.isBlank()) {
                            Text(
                                text = "搜索地点",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (keyword.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onClear),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(22.dp),
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "清空搜索"
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchHistorySection(
    history: List<String>,
    onHistorySelected: (String) -> Unit,
    onHistoryDelete: (String) -> Unit,
) {
    if (history.isEmpty()) {
        Text(
            text = "暂无搜索历史",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    Text(
        text = "搜索历史",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    history.forEach { item ->
        SearchHistoryItem(
            item = item,
            onSelected = { onHistorySelected(item) },
            onDelete = { onHistoryDelete(item) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchHistoryItem(
    item: String,
    onSelected: () -> Unit,
    onDelete: () -> Unit,
) {
    val deleteActionWidth = 88.dp
    val deleteActionWidthPx = with(LocalDensity.current) { deleteActionWidth.toPx() }
    var dragOffset by remember(item) { mutableFloatStateOf(0f) }
    var showActionDialog by remember(item) { mutableStateOf(false) }

    if (showActionDialog) {
        SearchHistoryActionDialog(
            title = item,
            onDelete = {
                showActionDialog = false
                onDelete()
            },
            onDismiss = { showActionDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .matchParentSize(),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(deleteActionWidth),
                    onClick = onDelete
                ) {
                    Text("删除")
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = dragOffset }
                .combinedClickable(
                    onClick = onSelected,
                    onLongClick = { showActionDialog = true }
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        dragOffset = (dragOffset + delta).coerceIn(-deleteActionWidthPx, 0f)
                    },
                    onDragStarted = {},
                    onDragStopped = {
                        dragOffset = if (dragOffset <= -deleteActionWidthPx / 2f) {
                            -deleteActionWidthPx
                        } else {
                            0f
                        }
                    }
                ),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null
                )
                Text(
                    text = item,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchHistoryActionDialog(
    title: String,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                HorizontalDivider()
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    onClick = onDelete
                ) {
                    Text(
                        text = "删除",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                HorizontalDivider()
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    onClick = onDismiss
                ) {
                    Text("关闭")
                }
            }
        }
    }
}

@Composable
private fun SearchResultList(
    statusText: String,
    isSearching: Boolean,
    results: List<MapSearchResult>,
    onResultSelected: (MapSearchResult) -> Unit,
) {
    when {
        isSearching -> Text(
            text = "正在搜索...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        statusText.isNotBlank() && results.isEmpty() -> Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        statusText.isNotBlank() -> Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(results) { result ->
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onResultSelected(result) }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = result.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionGuidePage(
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
    hasMockLocationPermission: Boolean,
    isSystemLocationEnabled: Boolean,
    hasRequestedLocationPermission: Boolean,
    hasRequestedNotificationPermission: Boolean,
    statusText: String,
    showMenuButton: Boolean,
    onMenuClick: () -> Unit,
    onRequestLocationPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenDeveloperSettings: () -> Unit,
    onOpenApplicationSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onRefreshRuntimeState: () -> Unit,
    onEnterApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var refreshRotationTarget by remember { mutableFloatStateOf(0f) }
    val refreshRotation by animateFloatAsState(
        targetValue = refreshRotationTarget,
        animationSpec = tween(durationMillis = 450),
        label = "refreshRotation"
    )
    val requiredSteps = listOf(
        PermissionGuideStep(
            title = "系统定位服务",
            description = "开启设备定位开关，地图选点和真实定位恢复依赖这个能力。",
            isReady = isSystemLocationEnabled,
            actionText = if (isSystemLocationEnabled) "已开启" else "打开定位设置",
            icon = Icons.Filled.MyLocation,
            onAction = onOpenLocationSettings
        ),
        PermissionGuideStep(
            title = "定位权限",
            description = "允许应用读取当前位置，用于定位到当前地点和恢复真实定位。",
            isReady = hasLocationPermission,
            actionText = when {
                hasLocationPermission -> "已授权"
                hasRequestedLocationPermission -> "打开应用设置"
                else -> "申请权限"
            },
            secondaryActionText = if (!hasLocationPermission && !hasRequestedLocationPermission) {
                "打开应用设置"
            } else {
                null
            },
            icon = Icons.Filled.LocationOn,
            onAction = if (hasRequestedLocationPermission) {
                onOpenApplicationSettings
            } else {
                onRequestLocationPermission
            },
            onSecondaryAction = onOpenApplicationSettings
        ),
        PermissionGuideStep(
            title = "通知权限",
            description = "前台服务运行时需要显示通知，Android 13 及以上需要单独授权。",
            isReady = hasNotificationPermission,
            actionText = when {
                hasNotificationPermission -> "已授权"
                hasRequestedNotificationPermission -> "打开应用设置"
                else -> "申请权限"
            },
            secondaryActionText = if (!hasNotificationPermission && !hasRequestedNotificationPermission) {
                "打开应用设置"
            } else {
                null
            },
            icon = Icons.Filled.Notifications,
            onAction = if (hasRequestedNotificationPermission) {
                onOpenApplicationSettings
            } else {
                onRequestNotificationPermission
            },
            onSecondaryAction = onOpenApplicationSettings
        ),
        PermissionGuideStep(
            title = "模拟位置应用",
            description = "在开发者选项中选择本应用作为模拟位置信息应用。",
            isReady = hasMockLocationPermission,
            actionText = if (hasMockLocationPermission) "已设置" else "打开开发者选项",
            icon = Icons.Filled.DeveloperMode,
            onAction = onOpenDeveloperSettings
        )
    )
    val readyRequiredCount = requiredSteps.count { it.isReady }
    val requiredReady = readyRequiredCount == requiredSteps.size

    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showMenuButton) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "打开菜单"
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "权限引导",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "按顺序完成系统授权与开发者选项设置",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = {
                    refreshRotationTarget += 360f
                    onRefreshRuntimeState()
                }
            ) {
                Icon(
                    modifier = Modifier.graphicsLayer {
                        rotationZ = refreshRotation
                    },
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "刷新权限状态"
                )
            }
        }

        PermissionGuideSummaryCard(
            readyRequiredCount = readyRequiredCount,
            requiredTotalCount = requiredSteps.size,
            requiredReady = requiredReady,
            statusText = statusText
        )

        PermissionGuideSection(
            title = "必需权限",
            steps = requiredSteps
        )

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenApplicationSettings
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = "打开应用设置"
            )
        }

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = requiredReady,
            onClick = onEnterApp
        ) {
            Text("进入应用")
        }
    }
}

private data class PermissionGuideStep(
    val title: String,
    val description: String,
    val isReady: Boolean,
    val actionText: String,
    val secondaryActionText: String? = null,
    val icon: ImageVector,
    val onAction: () -> Unit,
    val onSecondaryAction: (() -> Unit)? = null,
)

@Composable
private fun PermissionGuideSummaryCard(
    readyRequiredCount: Int,
    requiredTotalCount: Int,
    requiredReady: Boolean,
    statusText: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (requiredReady) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (requiredReady) "必需权限已就绪" else "还需完成必需权限",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$readyRequiredCount / $requiredTotalCount 已完成",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (requiredReady) {
                    "可以返回地图页开始模拟定位。"
                } else {
                    "建议从上到下处理，系统设置完成后返回本页刷新状态。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionGuideSection(
    title: String,
    steps: List<PermissionGuideStep>,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
    steps.forEach { step ->
        PermissionGuideStepCard(step)
    }
}

@Composable
private fun PermissionGuideStepCard(step: PermissionGuideStep) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PermissionStateBadge(isReady = step.isReady)
            }
            val secondaryActionText = step.secondaryActionText
            val secondaryAction = step.onSecondaryAction
            if (secondaryActionText == null || secondaryAction == null) {
                if (step.isReady) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = step.onAction
                    ) {
                        Text(step.actionText)
                    }
                } else {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = step.onAction
                    ) {
                        Text(step.actionText)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = step.onAction
                    ) {
                        Text(step.actionText)
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = secondaryAction
                    ) {
                        Text(secondaryActionText)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStateBadge(isReady: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = if (isReady) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isReady) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
        Text(
            text = if (isReady) "已就绪" else "待处理",
            style = MaterialTheme.typography.labelMedium,
            color = if (isReady) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun ConfigurationPage(
    latitude: String,
    longitude: String,
    altitude: String,
    statusText: String,
    isRunning: Boolean,
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
    hasMockLocationPermission: Boolean,
    providerNames: String,
    updateCount: Long,
    updateIntervalMs: Long,
    wakeDurationMs: Long,
    lastStopTime: String,
    lastError: String,
    hasGpsProvider: Boolean,
    hasNetworkProvider: Boolean,
    onMenuClick: () -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onAltitudeChange: (String) -> Unit,
    onUpdateIntervalChange: (Long) -> Unit,
    onWakeDurationChange: (Long) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenDeveloperSettings: () -> Unit,
    onOpenApplicationSettings: () -> Unit,
    onCopyDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "打开菜单"
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "配置与诊断",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "权限、坐标、稳定性和运行诊断",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        StatusCard(
            hasLocationPermission = hasLocationPermission,
            hasNotificationPermission = hasNotificationPermission,
            hasMockLocationPermission = hasMockLocationPermission,
            isRunning = isRunning,
            statusText = statusText
        )

        CoordinateInput(
            label = "纬度",
            value = latitude,
            onValueChange = onLatitudeChange
        )
        CoordinateInput(
            label = "经度",
            value = longitude,
            onValueChange = onLongitudeChange
        )
        CoordinateInput(
            label = "海拔（米）",
            value = altitude,
            onValueChange = onAltitudeChange
        )

        SettingsCard(
            updateIntervalMs = updateIntervalMs,
            wakeDurationMs = wakeDurationMs,
            onUpdateIntervalChange = onUpdateIntervalChange,
            onWakeDurationChange = onWakeDurationChange
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning,
            onClick = onStart
        ) {
            Text("开始模拟定位")
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onStop
        ) {
            Text("停止")
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onRequestPermissions
        ) {
            Text("申请权限")
        }

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenDeveloperSettings
        ) {
            Text("打开开发者选项")
        }

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenApplicationSettings
        ) {
            Text("打开应用设置")
        }

        DiagnosticCard(
            isRunning = isRunning,
            providerNames = providerNames,
            updateCount = updateCount,
            updateIntervalMs = updateIntervalMs,
            wakeDurationMs = wakeDurationMs,
            lastStopTime = lastStopTime,
            lastError = lastError,
            hasGpsProvider = hasGpsProvider,
            hasNetworkProvider = hasNetworkProvider,
            onCopy = onCopyDiagnostics
        )
    }
}

@Composable
private fun StatusCard(
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
    hasMockLocationPermission: Boolean,
    isRunning: Boolean,
    statusText: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isRunning) "运行中" else "已停止",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            StatusLine("定位权限", hasLocationPermission)
            StatusLine("通知权限", hasNotificationPermission)
            StatusLine("模拟位置应用", hasMockLocationPermission)
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusLine(label: String, passed: Boolean) {
    Text(
        text = "$label：${if (passed) "已就绪" else "待处理"}",
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
private fun CoordinateInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
private fun SettingsCard(
    updateIntervalMs: Long,
    wakeDurationMs: Long,
    onUpdateIntervalChange: (Long) -> Unit,
    onWakeDurationChange: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "稳定性设置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "注入间隔",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(200L, 500L, 1000L).forEach { value ->
                    SuggestionChip(
                        onClick = { onUpdateIntervalChange(value) },
                        label = { Text(if (value == updateIntervalMs) "${value}ms ✓" else "${value}ms") }
                    )
                }
            }
            Text(
                text = "停止后恢复真实定位",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0L, 5_000L, 10_000L).forEach { value ->
                    val label = if (value == 0L) "关闭" else "${value / 1000}秒"
                    SuggestionChip(
                        onClick = { onWakeDurationChange(value) },
                        label = { Text(if (value == wakeDurationMs) "$label ✓" else label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticCard(
    isRunning: Boolean,
    providerNames: String,
    updateCount: Long,
    updateIntervalMs: Long,
    wakeDurationMs: Long,
    lastStopTime: String,
    lastError: String,
    hasGpsProvider: Boolean,
    hasNetworkProvider: Boolean,
    onCopy: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "运行诊断",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text("服务状态：${if (isRunning) "运行中" else "已停止"}")
            Text("当前通道：$providerNames")
            Text("注入次数：$updateCount")
            Text("注入间隔：${updateIntervalMs}ms")
            Text("停止恢复：${if (wakeDurationMs == 0L) "关闭" else "${wakeDurationMs / 1000}秒"}")
            Text("最近停止：$lastStopTime")
            Text("最近错误：$lastError")
            Text("GPS Provider：${if (hasGpsProvider) "已开启" else "未开启"}")
            Text("网络 Provider：${if (hasNetworkProvider) "已开启" else "未开启"}")
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCopy
            ) {
                Text("复制诊断信息")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MockLocationScreenPreview() {
    MockLocationTheme {
        MockLocationScreen()
    }
}
