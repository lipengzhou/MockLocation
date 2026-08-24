package com.lipengzhou.mocklocation

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import com.lipengzhou.mocklocation.location.MockLocationService
import com.lipengzhou.mocklocation.ui.theme.MockLocationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MockLocationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MockLocationScreen(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun MockLocationScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var latitude by rememberSaveable { mutableStateOf(MockLocationService.DEFAULT_LATITUDE.toString()) }
    var longitude by rememberSaveable { mutableStateOf(MockLocationService.DEFAULT_LONGITUDE.toString()) }
    var altitude by rememberSaveable { mutableStateOf(MockLocationService.DEFAULT_ALTITUDE.toString()) }
    var statusText by rememberSaveable { mutableStateOf(context.savedStatusMessage()) }
    var isRunning by rememberSaveable { mutableStateOf(context.savedServiceRunningState()) }
    var hasLocationPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    var hasNotificationPermission by remember { mutableStateOf(context.hasNotificationPermission()) }
    var hasMockLocationPermission by remember { mutableStateOf(context.canUseMockLocation()) }
    var updateCount by rememberSaveable { mutableStateOf(context.savedUpdateCount()) }
    var providerNames by rememberSaveable { mutableStateOf(context.savedProviderNames()) }
    var updateIntervalMs by rememberSaveable { mutableStateOf(context.savedUpdateIntervalMs()) }
    var wakeDurationMs by rememberSaveable { mutableStateOf(context.savedWakeDurationMs()) }
    var lastStopTime by rememberSaveable { mutableStateOf(context.savedLastStopTimeText()) }
    var lastError by rememberSaveable { mutableStateOf(context.savedLastError()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasLocationPermission = context.hasLocationPermission()
        hasNotificationPermission = context.hasNotificationPermission()
    }

    LaunchedEffect(Unit) {
        hasLocationPermission = context.hasLocationPermission()
        hasNotificationPermission = context.hasNotificationPermission()
        hasMockLocationPermission = context.canUseMockLocation()
        isRunning = context.savedServiceRunningState()
        statusText = context.savedStatusMessage()
        updateCount = context.savedUpdateCount()
        providerNames = context.savedProviderNames()
        updateIntervalMs = context.savedUpdateIntervalMs()
        wakeDurationMs = context.savedWakeDurationMs()
        lastStopTime = context.savedLastStopTimeText()
        lastError = context.savedLastError()
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                if (intent.action == MockLocationService.ACTION_STATUS) {
                    isRunning = intent.getBooleanExtra(
                        MockLocationService.EXTRA_STATUS_RUNNING,
                        false
                    )
                    statusText = intent.getStringExtra(
                        MockLocationService.EXTRA_STATUS_MESSAGE
                    ) ?: statusText
                    hasMockLocationPermission = receiverContext.canUseMockLocation()
                    updateCount = receiverContext.savedUpdateCount()
                    providerNames = receiverContext.savedProviderNames()
                    updateIntervalMs = receiverContext.savedUpdateIntervalMs()
                    wakeDurationMs = receiverContext.savedWakeDurationMs()
                    lastStopTime = receiverContext.savedLastStopTimeText()
                    lastError = receiverContext.savedLastError()
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(MockLocationService.ACTION_STATUS),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "模拟定位",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "手动输入坐标并启动系统模拟定位",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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
            onValueChange = { latitude = it }
        )
        CoordinateInput(
            label = "经度",
            value = longitude,
            onValueChange = { longitude = it }
        )
        CoordinateInput(
            label = "海拔（米）",
            value = altitude,
            onValueChange = { altitude = it }
        )

        SettingsCard(
            updateIntervalMs = updateIntervalMs,
            wakeDurationMs = wakeDurationMs,
            onUpdateIntervalChange = { value ->
                context.saveUpdateIntervalMs(value)
                updateIntervalMs = value
                statusText = "注入间隔已设置为 ${value}ms。"
            },
            onWakeDurationChange = { value ->
                context.saveWakeDurationMs(value)
                wakeDurationMs = value
                statusText = "停止后真实定位恢复时长已设置为 ${value / 1000} 秒。"
            }
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning,
            onClick = {
                val lat = latitude.toDoubleOrNull()
                val lon = longitude.toDoubleOrNull()
                val alt = altitude.toDoubleOrNull() ?: MockLocationService.DEFAULT_ALTITUDE
                when {
                    !context.hasLocationPermission() -> {
                        statusText = "需要先授予定位权限。"
                        permissionLauncher.launch(requiredPermissions())
                    }

                    !context.hasNotificationPermission() -> {
                        statusText = "需要先授予通知权限。"
                        permissionLauncher.launch(requiredPermissions())
                    }

                    !context.canUseMockLocation() -> {
                        statusText = "请先在开发者选项中将本应用设置为模拟位置信息应用。"
                    }

                    lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0 -> {
                        statusText = "经纬度格式不正确。"
                    }

                    else -> {
                        statusText = "正在启动模拟定位..."
                        context.startMockLocationService(lat, lon, alt)
                    }
                }
                hasLocationPermission = context.hasLocationPermission()
                hasNotificationPermission = context.hasNotificationPermission()
                hasMockLocationPermission = context.canUseMockLocation()
            }
        ) {
            Text("开始模拟定位")
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                context.stopMockLocationService()
                isRunning = false
                statusText = "正在停止模拟定位..."
            }
        ) {
            Text("停止")
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                permissionLauncher.launch(requiredPermissions())
            }
        ) {
            Text("申请权限")
        }

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            }
        ) {
            Text("打开开发者选项")
        }

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                )
            }
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
            hasGpsProvider = context.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER),
            hasNetworkProvider = context.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER),
            onCopy = {
                val diagnosticText = buildDiagnosticText(
                    isRunning = isRunning,
                    statusText = statusText,
                    providerNames = providerNames,
                    updateCount = updateCount,
                    updateIntervalMs = updateIntervalMs,
                    wakeDurationMs = wakeDurationMs,
                    lastStopTime = lastStopTime,
                    lastError = lastError,
                    hasGpsProvider = context.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER),
                    hasNetworkProvider = context.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER),
                    hasLocationPermission = context.hasLocationPermission(),
                    hasNotificationPermission = context.hasNotificationPermission(),
                    hasMockLocationPermission = context.canUseMockLocation()
                )
                context.copyToClipboard(diagnosticText)
                statusText = "诊断信息已复制。"
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MockLocationPreview() {
    MockLocationTheme {
        MockLocationScreen()
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

private fun Context.startMockLocationService(
    latitude: Double,
    longitude: Double,
    altitude: Double,
) {
    val intent = Intent(this, MockLocationService::class.java)
        .setAction(MockLocationService.ACTION_START)
        .putExtra(MockLocationService.EXTRA_LATITUDE, latitude)
        .putExtra(MockLocationService.EXTRA_LONGITUDE, longitude)
        .putExtra(MockLocationService.EXTRA_ALTITUDE, altitude)
    startForegroundService(this, intent)
}

private fun Context.stopMockLocationService() {
    val intent = Intent(this, MockLocationService::class.java)
        .setAction(MockLocationService.ACTION_STOP)
    startService(intent)
}

private fun Context.savedServiceRunningState(): Boolean =
    getSharedPreferences(MockLocationService.PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(MockLocationService.KEY_IS_RUNNING, false)

private fun Context.savedStatusMessage(): String =
    getSharedPreferences(MockLocationService.PREFS_NAME, Context.MODE_PRIVATE)
        .getString(MockLocationService.KEY_STATUS_MESSAGE, "准备就绪") ?: "准备就绪"

private fun Context.savedUpdateCount(): Long =
    getSharedPreferences(MockLocationService.PREFS_NAME, Context.MODE_PRIVATE)
        .getLong(MockLocationService.KEY_UPDATE_COUNT, 0L)

private fun Context.savedProviderNames(): String =
    getSharedPreferences(MockLocationService.PREFS_NAME, Context.MODE_PRIVATE)
        .getString(MockLocationService.KEY_PROVIDER_NAMES, "无") ?: "无"

private fun Context.savedUpdateIntervalMs(): Long =
    getSharedPreferences(MockLocationService.PREFS_NAME, Context.MODE_PRIVATE)
        .getLong(
            MockLocationService.KEY_UPDATE_INTERVAL_MS,
            MockLocationService.DEFAULT_UPDATE_INTERVAL_MS
        )

private fun Context.savedWakeDurationMs(): Long =
    getSharedPreferences(MockLocationService.PREFS_NAME, Context.MODE_PRIVATE)
        .getLong(
            MockLocationService.KEY_WAKE_DURATION_MS,
            MockLocationService.DEFAULT_WAKE_DURATION_MS
        )

private fun Context.savedLastStopTimeText(): String {
    val timeMs = getSharedPreferences(MockLocationService.PREFS_NAME, Context.MODE_PRIVATE)
        .getLong(MockLocationService.KEY_LAST_STOP_TIME_MS, 0L)
    return if (timeMs <= 0L) {
        "无"
    } else {
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timeMs))
    }
}

private fun Context.savedLastError(): String =
    getSharedPreferences(MockLocationService.PREFS_NAME, Context.MODE_PRIVATE)
        .getString(MockLocationService.KEY_LAST_ERROR, "无") ?: "无"

private fun Context.saveUpdateIntervalMs(value: Long) {
    getSharedPreferences(MockLocationService.PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putLong(MockLocationService.KEY_UPDATE_INTERVAL_MS, value)
        .apply()
}

private fun Context.saveWakeDurationMs(value: Long) {
    getSharedPreferences(MockLocationService.PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putLong(MockLocationService.KEY_WAKE_DURATION_MS, value)
        .apply()
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

private fun Context.hasNotificationPermission(): Boolean =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

private fun Context.canUseMockLocation(): Boolean {
    return runCatching {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val provider = "mocklocation_permission_check"
        runCatching { locationManager.removeTestProvider(provider) }
        locationManager.addTestProvider(
            provider,
            android.location.provider.ProviderProperties.Builder()
                .setPowerUsage(android.location.provider.ProviderProperties.POWER_USAGE_LOW)
                .setAccuracy(android.location.provider.ProviderProperties.ACCURACY_FINE)
                .build()
        )
        locationManager.removeTestProvider(provider)
        true
    }.getOrDefault(false)
}

private fun Context.isProviderEnabled(provider: String): Boolean =
    runCatching {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        locationManager.isProviderEnabled(provider)
    }.getOrDefault(false)

private fun Context.copyToClipboard(text: String) {
    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboardManager.setPrimaryClip(ClipData.newPlainText("模拟定位诊断", text))
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
}

private fun requiredPermissions(): Array<String> {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}
