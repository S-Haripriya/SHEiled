package com.example.sheiled.ui.sos

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.*
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import java.text.SimpleDateFormat
import java.util.*
import com.example.sheiled.ui.sos.TrackingManager
import com.example.sheiled.ui.sos.TrackingUtils

class SOSForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val CHANNEL_ID = "SOS_CHANNEL"
    private val GPS_TIMEOUT = 30000L
    private val TARGET_ACCURACY = 25f

    private var bestLocation: Location? = null
    private var isSmsSent = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(1, buildNotification("🚨 SOS Active..."))

        triggerSOS()

        return START_NOT_STICKY
    }

    /* ===================== SOS CORE ===================== */

    private fun triggerSOS() {

        val prefs = getSharedPreferences("PANIC_PREFS", MODE_PRIVATE)

        val message = prefs.getString(
            "ALERT_MESSAGE",
            "🚨 I am in danger. Please help immediately!"
        ) ?: return

        val contacts =
            prefs.getStringSet("ALERT_CONTACTS", emptySet())
                ?: emptySet()

        if (contacts.isEmpty()) {
            stopSelf()
            return
        }

        if (!hasLocationPermission()) {
            sendSOSWithoutLocation(message, contacts)
            return
        }

        if (!isLocationEnabled()) {
            sendSOSWithoutLocation(message, contacts)
            return
        }

        bestLocation = null
        isSmsSent = false
// 🔴 START LIVE TRACKING
        TrackingManager.startTracking(this)

// 🔴 AUTO STOP AFTER 30 MINUTES
        Handler(Looper.getMainLooper()).postDelayed({

            TrackingManager.stopTracking(this)

        }, 30 * 60 * 1000)
        startLocationUpdates(message, contacts)
    }

    /* ===================== LOCATION ===================== */

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(
        message: String,
        contacts: Set<String>
    ) {

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000
        )
            .setMinUpdateIntervalMillis(500)
            .setMinUpdateDistanceMeters(0f)
            .build()

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(result: LocationResult) {

                for (location in result.locations) {

                    if (bestLocation == null ||
                        location.accuracy < bestLocation!!.accuracy
                    ) {
                        bestLocation = location
                    }

                    if (location.accuracy <= TARGET_ACCURACY) {
                        stopLocationUpdates()
                        finalizeSOS(message, contacts, location)
                        return
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        Handler(Looper.getMainLooper()).postDelayed({
            if (!isSmsSent) {
                stopLocationUpdates()

                if (bestLocation != null) {
                    finalizeSOS(message, contacts, bestLocation!!)
                } else {
                    fetchLastKnownAndFinalize(message, contacts)
                }
            }
        }, GPS_TIMEOUT)
    }

    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLastKnownAndFinalize(
        message: String,
        contacts: Set<String>
    ) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                finalizeSOS(message, contacts, location)
            }
            .addOnFailureListener {
                sendSOSWithoutLocation(message, contacts)
            }
    }

    /* ===================== FINAL STEP ===================== */

    private fun finalizeSOS(
        message: String,
        contacts: Set<String>,
        location: Location?
    ) {
        if (isSmsSent) return
        isSmsSent = true

        val finalMessage = buildSOSMessage(message, location)
        sendSMSAndCall(contacts, finalMessage)

        stopSelf()
    }

    private fun buildSOSMessage(
        base: String,
        location: Location?
    ): String {

        val sb = StringBuilder()
        sb.append(base)
        sb.append("\n\n🚨 SOS ALERT")

        if (location != null) {
            val sdf =
                SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            sb.append("\n\n📍 Location:")
            sb.append(
                "\nhttps://maps.google.com/?q=${location.latitude},${location.longitude}"
            )
            sb.append("\n📏 Accuracy: ${location.accuracy.toInt()}m")
            sb.append("\n🕒 ${sdf.format(Date(location.time))}")
        } else {
            sb.append("\n\n📍 Location unavailable")
        }
        // 🔴 ADD LIVE TRACKING LINK
        val liveLink = TrackingUtils.getTrackingLink()

        sb.append("\n\n🔴 Live Tracking:")
        sb.append("\n$liveLink")
        return sb.toString()
    }

    /* ===================== SMS + CALL ===================== */

    private fun sendSMSAndCall(
        contacts: Set<String>,
        message: String
    ) {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        val smsManager = SmsManager.getDefault()

        if (smsManager == null) {
            stopSelf()
            return
        }

        contacts.forEachIndexed { index, number ->

            try {

                val parts = smsManager.divideMessage(message)

                smsManager.sendMultipartTextMessage(
                    number,
                    null,
                    parts,
                    null,
                    null
                )

            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (index == 0 &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$number")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(callIntent)
            }
        }
    }

    /* ===================== HELPERS ===================== */

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun isLocationEnabled(): Boolean {
        val lm =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun sendSOSWithoutLocation(
        message: String,
        contacts: Set<String>
    ) {
        sendSMSAndCall(
            contacts,
            "$message\n\n🚨 SOS ALERT\n📍 Location unavailable"
        )
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "SOS Emergency",
            NotificationManager.IMPORTANCE_HIGH
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Emergency SOS")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    override fun onBind(intent: Intent?) = null
}