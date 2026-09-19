package com.example.fakegps

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import androidx.core.app.NotificationCompat

/**
 * Registers itself as Android's mock location provider (the same mechanism
 * every "Fake GPS" app on the Play Store uses) and repeatedly feeds it the
 * lat/lng the user chose, so any app reading location — Google Maps included —
 * sees that point instead of the real one.
 *
 * Requires the user to manually pick this app under
 * Developer options → Select mock location app. That's an Android platform
 * requirement, not something an app can do for itself.
 */
class FakeLocationService : Service() {

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
        private const val CHANNEL_ID = "fake_gps_channel"
        private const val NOTIFICATION_ID = 1001
        private const val UPDATE_INTERVAL_MS = 1000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var currentLat: Double = 0.0
    private var currentLng: Double = 0.0
    private var isRunning = false
    private val activeProviders = mutableListOf<String>()

    private lateinit var locationManager: LocationManager

    private val updateRunnable = object : Runnable {
        override fun run() {
            pushMockLocation()
            handler.postDelayed(this, UPDATE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        startForegroundWithNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.hasExtra(EXTRA_LAT) && it.hasExtra(EXTRA_LNG)) {
                currentLat = it.getDoubleExtra(EXTRA_LAT, currentLat)
                currentLng = it.getDoubleExtra(EXTRA_LNG, currentLng)
            }
        }

        if (!isRunning) {
            setupMockProviders()
            handler.post(updateRunnable)
            isRunning = true
        }

        return START_STICKY
    }

    private fun setupMockProviders() {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        for (provider in providers) {
            try {
                locationManager.addTestProvider(
                    provider,
                    false, false, false, false,
                    true, true, true,
                    Criteria.POWER_LOW,
                    Criteria.ACCURACY_FINE
                )
                locationManager.setTestProviderEnabled(provider, true)
                activeProviders.add(provider)
            } catch (e: SecurityException) {
                Toast.makeText(
                    this,
                    "Go to Developer options → Select mock location app and choose this app first",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: IllegalArgumentException) {
                // Provider was already registered from a previous run; safe to reuse.
                activeProviders.add(provider)
            }
        }
    }

    private fun pushMockLocation() {
        for (provider in activeProviders) {
            try {
                val location = Location(provider).apply {
                    latitude = currentLat
                    longitude = currentLng
                    altitude = 0.0
                    accuracy = 5f
                    bearing = 0f
                    speed = 0f
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }
                locationManager.setTestProviderLocation(provider, location)
            } catch (e: SecurityException) {
                // No longer set as the mock location app — stop rather than keep failing silently.
                stopSelf()
            } catch (e: IllegalArgumentException) {
                // Provider was removed elsewhere; skip this cycle.
            }
        }
    }

    private fun startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Fake GPS", NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fake GPS is active")
            .setContentText("Other apps will see the location you set")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(updateRunnable)
        for (provider in activeProviders) {
            try {
                locationManager.removeTestProvider(provider)
            } catch (e: Exception) {
                // Already removed, or was never fully registered — safe to ignore.
            }
        }
        activeProviders.clear()
        isRunning = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
