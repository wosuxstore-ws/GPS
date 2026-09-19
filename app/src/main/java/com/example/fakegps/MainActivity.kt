package com.example.fakegps

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var editLatitude: EditText
    private lateinit var editLongitude: EditText
    private lateinit var textStatus: TextView

    private val prefsName = "fake_gps_prefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editLatitude = findViewById(R.id.editLatitude)
        editLongitude = findViewById(R.id.editLongitude)
        textStatus = findViewById(R.id.textStatus)

        val btnStart: Button = findViewById(R.id.btnStart)
        val btnStop: Button = findViewById(R.id.btnStop)
        val btnDevSettings: Button = findViewById(R.id.btnDevSettings)

        restoreLastValues()
        requestNeededPermissions()

        btnStart.setOnClickListener { onStartClicked() }
        btnStop.setOnClickListener { onStopClicked() }
        btnDevSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
        }
    }

    private fun requestNeededPermissions() {
        val toRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            toRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            toRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        if (toRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toTypedArray(), 100)
        }
    }

    private fun onStartClicked() {
        val lat = editLatitude.text.toString().trim().toDoubleOrNull()
        val lng = editLongitude.text.toString().trim().toDoubleOrNull()

        if (lat == null || lat < -90.0 || lat > 90.0) {
            Toast.makeText(this, "Enter a valid latitude (-90 to 90)", Toast.LENGTH_SHORT).show()
            return
        }
        if (lng == null || lng < -180.0 || lng > 180.0) {
            Toast.makeText(this, "Enter a valid longitude (-180 to 180)", Toast.LENGTH_SHORT).show()
            return
        }

        saveLastValues(lat, lng)

        val intent = Intent(this, FakeLocationService::class.java).apply {
            putExtra(FakeLocationService.EXTRA_LAT, lat)
            putExtra(FakeLocationService.EXTRA_LNG, lng)
        }
        ContextCompat.startForegroundService(this, intent)

        textStatus.text = "Status: faking location at $lat, $lng"
    }

    private fun onStopClicked() {
        stopService(Intent(this, FakeLocationService::class.java))
        textStatus.text = "Status: stopped — apps will use your real GPS again"
    }

    private fun saveLastValues(lat: Double, lng: Double) {
        getSharedPreferences(prefsName, MODE_PRIVATE).edit()
            .putString("lat", lat.toString())
            .putString("lng", lng.toString())
            .apply()
    }

    private fun restoreLastValues() {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)
        prefs.getString("lat", null)?.let { editLatitude.setText(it) }
        prefs.getString("lng", null)?.let { editLongitude.setText(it) }
    }
}
