package com.lipengzhou.mocklocation

import android.Manifest
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
    var statusText by rememberSaveable { mutableStateOf("Ready") }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    var hasNotificationPermission by remember { mutableStateOf(context.hasNotificationPermission()) }
    var hasMockLocationPermission by remember { mutableStateOf(context.canUseMockLocation()) }

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
            text = "MockLocation",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Manual coordinate mock location MVP",
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
            label = "Latitude",
            value = latitude,
            onValueChange = { latitude = it }
        )
        CoordinateInput(
            label = "Longitude",
            value = longitude,
            onValueChange = { longitude = it }
        )
        CoordinateInput(
            label = "Altitude",
            value = altitude,
            onValueChange = { altitude = it }
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
                        statusText = "Location permission is required."
                        permissionLauncher.launch(requiredPermissions())
                    }

                    !context.hasNotificationPermission() -> {
                        statusText = "Notification permission is required."
                        permissionLauncher.launch(requiredPermissions())
                    }

                    !context.canUseMockLocation() -> {
                        statusText = "Select this app as the mock location app in Developer options."
                    }

                    lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0 -> {
                        statusText = "Invalid coordinates."
                    }

                    else -> {
                        statusText = "Starting mock location..."
                        context.startMockLocationService(lat, lon, alt)
                    }
                }
                hasLocationPermission = context.hasLocationPermission()
                hasNotificationPermission = context.hasNotificationPermission()
                hasMockLocationPermission = context.canUseMockLocation()
            }
        ) {
            Text("Start Mock Location")
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            enabled = isRunning,
            onClick = {
                context.stopMockLocationService()
                isRunning = false
                statusText = "Stopping mock location..."
            }
        ) {
            Text("Stop")
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                permissionLauncher.launch(requiredPermissions())
            }
        ) {
            Text("Request Permissions")
        }

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            }
        ) {
            Text("Open Developer Options")
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
            Text("Open App Settings")
        }
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
                text = if (isRunning) "Running" else "Stopped",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            StatusLine("Location permission", hasLocationPermission)
            StatusLine("Notification permission", hasNotificationPermission)
            StatusLine("Mock location app", hasMockLocationPermission)
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
        text = "$label: ${if (passed) "OK" else "Required"}",
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
