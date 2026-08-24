package com.lipengzhou.mocklocation.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.lipengzhou.mocklocation.MainActivity
import com.lipengzhou.mocklocation.R

class MockLocationService : Service() {

    private lateinit var locationManager: LocationManager
    private lateinit var workerThread: HandlerThread
    private lateinit var workerHandler: Handler

    private var currentLatitude = DEFAULT_LATITUDE
    private var currentLongitude = DEFAULT_LONGITUDE
    private var currentAltitude = DEFAULT_ALTITUDE
    private var isRunning = false
    private var updateCount = 0L
    private val activeProviders = linkedMapOf<String, Int>()
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) {
                return
            }

            val failedProviders = mutableListOf<String>()
            activeProviders.forEach { (provider, accuracy) ->
                if (!setMockLocation(provider, accuracy)) {
                    failedProviders += provider
                }
            }
            failedProviders.forEach { provider ->
                activeProviders.remove(provider)
                removeTestProvider(provider)
            }

            if (activeProviders.isEmpty()) {
                broadcastStatus(
                    isRunning = false,
                    message = "No mock provider is available. Check mock location settings."
                )
                stopSelf()
                return
            }

            updateCount += 1
            broadcastStatus(
                isRunning = true,
                message = "Mocking via ${activeProviders.keys.joinToString()} (#$updateCount)"
            )
            workerHandler.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        workerThread = HandlerThread("MockLocationWorker").apply { start() }
        workerHandler = Handler(workerThread.looper)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                currentLatitude = intent.getDoubleExtra(EXTRA_LATITUDE, DEFAULT_LATITUDE)
                currentLongitude = intent.getDoubleExtra(EXTRA_LONGITUDE, DEFAULT_LONGITUDE)
                currentAltitude = intent.getDoubleExtra(EXTRA_ALTITUDE, DEFAULT_ALTITUDE)
                startForegroundService()
                startMocking()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopMocking()
        workerThread.quitSafely()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startMocking() {
        if (isRunning) {
            broadcastStatus(
                isRunning = true,
                message = "Mocking via ${activeProviders.keys.joinToString()} (#$updateCount)"
            )
            return
        }
        isRunning = true
        updateCount = 0L
        workerHandler.post {
            try {
                addTestProviders()
                if (activeProviders.isEmpty()) {
                    throw IllegalStateException("No mock provider is available.")
                }
                updateRunnable.run()
            } catch (securityException: SecurityException) {
                isRunning = false
                broadcastStatus(
                    isRunning = false,
                    message = "Mock location permission is not enabled for this app."
                )
                stopSelf()
            } catch (exception: Exception) {
                isRunning = false
                broadcastStatus(
                    isRunning = false,
                    message = exception.message ?: "Failed to start mock location."
                )
                stopSelf()
            }
        }
        broadcastStatus(isRunning = true, message = "Starting mock providers...")
    }

    private fun stopMocking() {
        isRunning = false
        updateCount = 0L
        workerHandler.removeCallbacksAndMessages(null)
        removeTestProvider(LocationManager.GPS_PROVIDER)
        removeTestProvider(LocationManager.NETWORK_PROVIDER)
        activeProviders.clear()
        broadcastStatus(isRunning = false, message = "Mock location stopped")
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun addTestProviders() {
        activeProviders.clear()
        addTestProvider(
            provider = LocationManager.GPS_PROVIDER,
            accuracy = ProviderProperties.ACCURACY_FINE,
            properties = ProviderProperties.Builder()
                .setHasSatelliteRequirement(true)
                .setHasAltitudeSupport(true)
                .setHasSpeedSupport(true)
                .setHasBearingSupport(true)
                .setPowerUsage(ProviderProperties.POWER_USAGE_HIGH)
                .setAccuracy(ProviderProperties.ACCURACY_FINE)
                .build()
        )
        addTestProvider(
            provider = LocationManager.NETWORK_PROVIDER,
            accuracy = ProviderProperties.ACCURACY_COARSE,
            properties = ProviderProperties.Builder()
                .setHasNetworkRequirement(true)
                .setHasAltitudeSupport(true)
                .setHasSpeedSupport(true)
                .setHasBearingSupport(true)
                .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                .setAccuracy(ProviderProperties.ACCURACY_COARSE)
                .build()
        )
    }

    private fun addTestProvider(
        provider: String,
        accuracy: Int,
        properties: ProviderProperties,
    ) {
        try {
            removeTestProvider(provider)
            locationManager.addTestProvider(provider, properties)
            locationManager.setTestProviderEnabled(provider, true)
            activeProviders[provider] = accuracy
        } catch (securityException: SecurityException) {
            throw securityException
        } catch (exception: Exception) {
            broadcastStatus(
                isRunning = true,
                message = "$provider is unavailable: ${exception.message ?: "unknown error"}"
            )
        }
    }

    private fun setMockLocation(provider: String, accuracy: Int): Boolean {
        return runCatching {
            val location = Location(provider).apply {
                latitude = currentLatitude
                longitude = currentLongitude
                altitude = currentAltitude
                this.accuracy = if (accuracy == ProviderProperties.ACCURACY_FINE) 5f else 50f
                verticalAccuracyMeters = 3f
                speed = 0.1f
                speedAccuracyMetersPerSecond = 0.1f
                bearing = 0f
                bearingAccuracyDegrees = 1f
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    isMock = true
                }
                extras = Bundle().apply {
                    putInt("satellites", 12)
                    putInt("satellitesvalue", 12)
                }
            }
            locationManager.setTestProviderLocation(provider, location)
        }.isSuccess
    }

    private fun removeTestProvider(provider: String) {
        runCatching { locationManager.setTestProviderEnabled(provider, false) }
        runCatching { locationManager.removeTestProvider(provider) }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Mock location",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MockLocationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("MockLocation is running")
            .setContentText("$currentLatitude, $currentLongitude")
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun broadcastStatus(isRunning: Boolean, message: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_RUNNING, isRunning)
            .putString(KEY_STATUS_MESSAGE, message)
            .apply()

        val intent = Intent(ACTION_STATUS)
            .setPackage(packageName)
            .putExtra(EXTRA_STATUS_RUNNING, isRunning)
            .putExtra(EXTRA_STATUS_MESSAGE, message)
        sendBroadcast(intent)
    }

    companion object {
        const val ACTION_START = "com.lipengzhou.mocklocation.action.START"
        const val ACTION_STOP = "com.lipengzhou.mocklocation.action.STOP"
        const val ACTION_STATUS = "com.lipengzhou.mocklocation.action.STATUS"

        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_ALTITUDE = "extra_altitude"
        const val EXTRA_STATUS_RUNNING = "extra_status_running"
        const val EXTRA_STATUS_MESSAGE = "extra_status_message"

        private const val CHANNEL_ID = "mock_location"
        private const val NOTIFICATION_ID = 1001
        private const val UPDATE_INTERVAL_MS = 200L

        const val PREFS_NAME = "mock_location_state"
        const val KEY_IS_RUNNING = "is_running"
        const val KEY_STATUS_MESSAGE = "status_message"

        const val DEFAULT_LATITUDE = 39.908722
        const val DEFAULT_LONGITUDE = 116.397499
        const val DEFAULT_ALTITUDE = 45.0
    }
}
