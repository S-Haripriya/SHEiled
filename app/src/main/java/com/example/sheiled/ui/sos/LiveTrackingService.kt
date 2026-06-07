package com.example.sheiled.ui.sos

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.sheiled.R
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class LiveTrackingService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate() {
        super.onCreate()

        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        startForeground(101, createNotification())

        startTracking()
    }

    private fun createNotification(): Notification {

        val channelId = "live_tracking_channel"

        val manager = getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            channelId,
            "Live Tracking",
            NotificationManager.IMPORTANCE_LOW
        )

        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("SHEiled Live Tracking")
            .setContentText("Your live location is being shared")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private fun startTracking() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).build()

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(result: LocationResult) {

                val location = result.lastLocation ?: return

                updateFirestore(location)
            }
        }

        fusedClient.requestLocationUpdates(
            request,
            locationCallback!!,
            mainLooper
        )
    }

    private fun updateFirestore(location: Location) {

        val uid = auth.currentUser?.uid ?: return

        val data = mapOf(
            "lat" to location.latitude,
            "lng" to location.longitude,
            "timestamp" to System.currentTimeMillis(),
            "status" to "allowed"
        )

        db.collection("users")
            .document(uid)
            .set(
                mapOf("live_location" to data),
                SetOptions.merge()
            )
    }

    override fun onDestroy() {
        super.onDestroy()

        locationCallback?.let {
            fusedClient.removeLocationUpdates(it)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}