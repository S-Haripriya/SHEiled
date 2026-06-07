package com.example.sheiled.ui.sos

import ShakeDetector
import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.*
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.example.sheiled.R
import com.example.sheiled.ui.sos.TrackingManager
import com.example.sheiled.ui.sos.TrackingUtils

class SensorService : Service() {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val GPS_TIMEOUT = 30000L
    private val TARGET_ACCURACY = 25f

    private var bestLocation: Location? = null
    private var isSmsSent = false
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        // 🔔 Foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground()
        else
            startForeground(1, Notification())

        // 📱 Sensor setup
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Inside SensorService
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)
        shakeDetector = ShakeDetector {
            vibrate()
            triggerSOS()

        }

        sensorManager.registerListener(
            shakeDetector,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    /* ================= SOS CORE ================= */

    @SuppressLint("MissingPermission")
    private fun triggerSOS() {

        val prefs = getSharedPreferences("PANIC_PREFS", MODE_PRIVATE)

        val alertMessage = prefs.getString(
            "ALERT_MESSAGE",
            "🚨 I am in danger. Please help me immediately!"
        ) ?: return

        val contacts =
            prefs.getStringSet("ALERT_CONTACTS", emptySet()) ?: emptySet()

        if (contacts.isEmpty()) {
            Log.e("SOS", "No emergency contacts found")
            return
        }
        TrackingManager.startTracking(this)
        Handler(Looper.getMainLooper()).postDelayed({

            TrackingManager.stopTracking(this)

            Log.d("SOS", "Live tracking stopped automatically after 30 minutes")

        }, 30 * 60 * 1000) // 30 minutes


        bestLocation = null
        isSmsSent = false

        startLocationUpdates(alertMessage, contacts)
    }

    /* ================= SEND SOS ================= */

    private fun sendSOSWithLocation(
        message: String,
        contacts: Set<String>,
        location: Location
    ) {

        val locationLink =
            "https://maps.google.com/?q=${location.latitude},${location.longitude}"
        val accuracy = location.accuracy.toInt()
        val liveLink = TrackingUtils.getTrackingLink()
        val finalMessage = """
$message

📍 Location:
$locationLink
📏 Accuracy: ${accuracy} meters
🔴 Live Tracking:
$liveLink
""".trimIndent()

        sendSMSAndCall(contacts, finalMessage)
    }

    private fun sendSOSWithoutLocation(
        message: String,
        contacts: Set<String>
    ) {
        val liveLink = TrackingUtils.getTrackingLink()
        val finalMessage = """
$message

📍 Location:
Unavailable
🔴 Live Tracking:
$liveLink
""".trimIndent()

        sendSMSAndCall(contacts, finalMessage)
    }
    private fun stopLocationUpdates() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
    /* ================= SMS + CALL ================= */
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
                    sendSOSWithoutLocation(message, contacts)
                }

            }

        }, GPS_TIMEOUT)
    }
    private fun finalizeSOS(
        message: String,
        contacts: Set<String>,
        location: Location
    ) {

        if (isSmsSent) return
        isSmsSent = true

        sendSOSWithLocation(message, contacts, location)
    }
    private fun sendSMSAndCall(
        contacts: Set<String>,
        message: String
    ) {

        val subscriptionId =
            android.telephony.SubscriptionManager
                .getDefaultSmsSubscriptionId()

        if (subscriptionId == -1) {
            Log.e("SOS", "No default SMS subscription set!")
            return
        }

        val smsManager =
            SmsManager.getSmsManagerForSubscriptionId(subscriptionId)

        contacts.forEach { number ->

            try {
                val parts = smsManager.divideMessage(message)

                smsManager.sendMultipartTextMessage(
                    number,
                    null,
                    parts,
                    null,
                    null
                )

                Log.d("SOS", "SMS SENT to $number")

            } catch (e: Exception) {
                Log.e("SOS", "SMS FAILED for $number", e)
            }

            // Call logic unchanged
            if (ContextCompat.checkSelfPermission(
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

    /* ================= VIBRATION ================= */

    private fun vibrate() {
        val vibrator =
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect =
                VibrationEffect.createPredefined(
                    VibrationEffect.EFFECT_DOUBLE_CLICK
                )
            vibrator.cancel()
            vibrator.vibrate(effect)
        } else {
            vibrator.vibrate(500)
        }
    }

    /* ================= FOREGROUND ================= */

    @SuppressLint("ForegroundServiceType")
    private fun startMyOwnForeground() {

        val channelId = "sos_shake_service"
        val channelName = "SOS Protection Service"

        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_MIN
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setOngoing(true)
            .setContentTitle("You are protected")
            .setContentText("Shake phone 3 times to send SOS")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .build()

        startForeground(1001, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        sendBroadcast(
            Intent(this, ReactivateService::class.java)
                .setAction("restartservice")
        )
    }
}