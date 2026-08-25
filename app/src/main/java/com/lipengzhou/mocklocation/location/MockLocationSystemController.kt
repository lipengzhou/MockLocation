package com.lipengzhou.mocklocation.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.location.provider.ProviderProperties
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startForegroundService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executor
import kotlin.coroutines.resume

class MockLocationSystemController(context: Context) {
    private val appContext = context.applicationContext
    private val directExecutor = Executor { command -> command.run() }

    fun startMockLocationService(
        latitude: Double,
        longitude: Double,
        altitude: Double,
    ) {
        val intent = Intent(appContext, MockLocationService::class.java)
            .setAction(MockLocationService.ACTION_START)
            .putExtra(MockLocationService.EXTRA_LATITUDE, latitude)
            .putExtra(MockLocationService.EXTRA_LONGITUDE, longitude)
            .putExtra(MockLocationService.EXTRA_ALTITUDE, altitude)
        startForegroundService(appContext, intent)
    }

    fun stopMockLocationService() {
        val intent = Intent(appContext, MockLocationService::class.java)
            .setAction(MockLocationService.ACTION_STOP)
        appContext.startService(intent)
    }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

    fun hasNotificationPermission(): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun canUseMockLocation(): Boolean {
        return runCatching {
            val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val provider = "mocklocation_permission_check"
            runCatching { locationManager.removeTestProvider(provider) }
            locationManager.addTestProvider(
                provider,
                ProviderProperties.Builder()
                    .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                    .setAccuracy(ProviderProperties.ACCURACY_FINE)
                    .build()
            )
            locationManager.removeTestProvider(provider)
            true
        }.getOrDefault(false)
    }

    fun isProviderEnabled(provider: String): Boolean =
        runCatching {
            val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.isProviderEnabled(provider)
        }.getOrDefault(false)

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.activeLocationProviders()
            .mapNotNull { provider ->
                runCatching {
                    locationManager.getLastKnownLocation(provider)
                }.getOrNull()
            }
            .maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return getLastKnownLocation() ?: withTimeoutOrNull(CURRENT_LOCATION_TIMEOUT_MS) {
            locationManager.activeLocationProviders()
                .firstNotNullOfOrNull { provider ->
                    runCatching {
                        locationManager.awaitCurrentLocation(provider)
                    }.getOrNull()
                }
        }
    }

    fun copyToClipboard(text: String) {
        val clipboardManager = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText("模拟定位诊断", text))
    }

    private fun LocationManager.activeLocationProviders(): List<String> =
        listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).filter { provider ->
            runCatching { isProviderEnabled(provider) }.getOrDefault(false)
        }

    @SuppressLint("MissingPermission")
    private suspend fun LocationManager.awaitCurrentLocation(provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation {
                cancellationSignal.cancel()
            }
            getCurrentLocation(provider, cancellationSignal, directExecutor) { location ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }
        }

    companion object {
        private const val CURRENT_LOCATION_TIMEOUT_MS = 5_000L
    }
}
