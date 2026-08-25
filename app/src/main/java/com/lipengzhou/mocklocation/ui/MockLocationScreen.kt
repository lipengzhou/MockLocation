package com.lipengzhou.mocklocation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.RectangleShape
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
    onLocateCurrentPosition: () -> Unit = {},
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onRequestPermissions: () -> Unit = {},
    onOpenDeveloperSettings: () -> Unit = {},
    onOpenApplicationSettings: () -> Unit = {},
    onCopyDiagnostics: () -> Unit = {},
    onLatitudeChange: (String) -> Unit = {},
    onLongitudeChange: (String) -> Unit = {},
    onAltitudeChange: (String) -> Unit = {},
    onUpdateIntervalChange: (Long) -> Unit = {},
    onWakeDurationChange: (Long) -> Unit = {},
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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
                onLocateCurrentPosition = onLocateCurrentPosition,
                onStart = onStart,
                onStop = onStop,
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
    onLocateCurrentPosition: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AMapPicker(
            modifier = Modifier.fillMaxSize(),
            initialLatitude = latitude,
            initialLongitude = longitude,
            selectedCoordinate = selectedCoordinate,
            zoomControlsBottomPadding = 188.dp,
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
