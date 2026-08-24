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
            return
        }
        isRunning = true
        workerHandler.post {
            try {
                addTestProviders()
                while (isRunning) {
                    setMockLocation(LocationManager.GPS_PROVIDER, ProviderProperties.ACCURACY_FINE)
                    setMockLocation(LocationManager.NETWORK_PROVIDER, ProviderProperties.ACCURACY_COARSE)
                    Thread.sleep(UPDATE_INTERVAL_MS)
                }
            } catch (securityException: SecurityException) {
                broadcastStatus(
                    isRunning = false,
                    message = "Mock location permission is not enabled for this app."
                )
                stopSelf()
            } catch (exception: Exception) {
                broadcastStatus(
                    isRunning = false,
                    message = exception.message ?: "Failed to start mock location."
                )
                stopSelf()
            }
        }
        broadcastStatus(isRunning = true, message = "Mocking location")
    }

    private fun stopMocking() {
        isRunning = false
        workerHandler.removeCallbacksAndMessages(null)
        removeTestProvider(LocationManager.GPS_PROVIDER)
        removeTestProvider(LocationManager.NETWORK_PROVIDER)
        broadcastStatus(isRunning = false, message = "Mock location stopped")
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun addTestProviders() {
        addTestProvider(
            provider = LocationManager.GPS_PROVIDER,
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

    private fun addTestProvider(provider: String, properties: ProviderProperties) {
        runCatching { locationManager.removeTestProvider(provider) }
        locationManager.addTestProvider(provider, properties)
        locationManager.setTestProviderEnabled(provider, true)
    }

    private fun setMockLocation(provider: String, accuracy: Int) {
        val location = Location(provider).apply {
            latitude = currentLatitude
            longitude = currentLongitude
            altitude = currentAltitude
            this.accuracy = if (accuracy == ProviderProperties.ACCURACY_FINE) 5f else 50f
            speed = 0f
            bearing = 0f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        locationManager.setTestProviderLocation(provider, location)
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
        private const val UPDATE_INTERVAL_MS = 1_000L

        const val DEFAULT_LATITUDE = 39.908722
        const val DEFAULT_LONGITUDE = 116.397499
        const val DEFAULT_ALTITUDE = 45.0
    }
}
