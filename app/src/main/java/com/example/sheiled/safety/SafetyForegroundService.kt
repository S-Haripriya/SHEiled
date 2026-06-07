package com.example.sheiled.safety

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.sheiled.R
import com.google.android.gms.location.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import kotlin.concurrent.thread
import kotlin.math.abs

class SafetyForegroundService : Service() {

    companion object {
        var isServiceRunning = false
        const val CHANNEL_ID = "SAFE_MODE_CHANNEL"
    }

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val riskManager = RiskDetectionManager()

    /* ------------ SPEED TRACKING ------------ */
    private val speedHistory = ArrayDeque<Double>()
    private val MAX_SPEED_SAMPLES = 3

    /* ------------ API THROTTLING ------------ */
    private var lastPlacesFetchTime = 0L
    private val PLACES_REFRESH_INTERVAL = 3 * 60 * 1000L // 3 minutes

    private var placesPending = 0

    /* ------------ NOTIFICATION CONTROL ------------ */
    private var lastScore = -1

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        fusedClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(
            1,
            buildNotification("Safe Mode Active - Monitoring...")
        )

        startLocationUpdates()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        if (::locationCallback.isInitialized) {
            fusedClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onBind(intent: Intent?) = null

    /* ======================================================
       LOCATION (Battery Optimized)
    ======================================================= */

    private fun startLocationUpdates() {

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            15000L
        )
            .setMinUpdateIntervalMillis(10000L)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    evaluateSafety(it)
                }
            }
        }

        fusedClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    /* ======================================================
       MAIN SAFETY EVALUATION
    ======================================================= */

    private fun evaluateSafety(location: Location) {

        val hour = Calendar.getInstance()
            .get(Calendar.HOUR_OF_DAY)

        val speed = if (location.hasSpeed()) {
            location.speed * 3.6
        } else 0.0

        val suddenStopDetected = detectSuddenStop(speed)

        val currentTime = System.currentTimeMillis()

        if (currentTime - lastPlacesFetchTime > PLACES_REFRESH_INTERVAL) {
            lastPlacesFetchTime = currentTime
            fetchAllPlaces(location)
        }

        val score = riskManager.calculateRisk(
            hour,
            speed,
            suddenStopDetected
        )

        publishRisk(score)
    }

    /* ======================================================
       SPEED BEHAVIOR LOGIC (Smarter)
    ======================================================= */

    private fun detectSuddenStop(currentSpeed: Double): Boolean {

        if (speedHistory.size == MAX_SPEED_SAMPLES)
            speedHistory.removeFirst()

        speedHistory.addLast(currentSpeed)

        if (speedHistory.size < MAX_SPEED_SAMPLES)
            return false

        val first = speedHistory.first()
        val last = speedHistory.last()

        return (first - last) > 25 && first > 40
    }

    /* ======================================================
       GOOGLE PLACES (THROTTLED + SAFE)
    ======================================================= */

    private fun fetchAllPlaces(location: Location) {

        riskManager.reset()
        placesPending = 3

        fetchPlaces(location, "police")
        fetchPlaces(location, "hospital")
        fetchPlaces(location, "restaurant")
    }

    private fun fetchPlaces(location: Location, type: String) {

        val apiKey = getString(R.string.google_maps_key)

        val urlString =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                    "location=${location.latitude},${location.longitude}" +
                    "&radius=2500&type=$type&key=$apiKey"

        thread {
            try {

                val connection =
                    URL(urlString).openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                connection.connect()

                val response =
                    connection.inputStream.bufferedReader().use {
                        it.readText()
                    }

                val json = JSONObject(response)

                if (json.optString("status") == "OK") {

                    val results = json.getJSONArray("results")

                    for (i in 0 until results.length()) {

                        val place = results.getJSONObject(i)
                        val types = place.getJSONArray("types")

                        val locJson =
                            place.getJSONObject("geometry")
                                .getJSONObject("location")

                        val placeLocation = Location("").apply {
                            latitude = locJson.getDouble("lat")
                            longitude = locJson.getDouble("lng")
                        }

                        val distance =
                            location.distanceTo(placeLocation).toDouble()

                        val isOpen =
                            place.optJSONObject("opening_hours")
                                ?.optBoolean("open_now", false)
                                ?: false

                        riskManager.updatePlace(
                            types,
                            distance,
                            isOpen
                        )
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                synchronized(this) {
                    placesPending--
                }
            }
        }
    }

    /* ======================================================
       RISK OUTPUT
    ======================================================= */

    private fun publishRisk(score: Int) {

        if (abs(score - lastScore) >= 5) {
            updateNotification(score)
            sendRiskBroadcast(score)
            lastScore = score
        }
    }

    private fun sendRiskBroadcast(score: Int) {
        val intent = Intent("com.example.sheiled.RISK_UPDATE")
        intent.putExtra("risk_score", score)
        sendBroadcast(intent)
    }

    /* ======================================================
       NOTIFICATION
    ======================================================= */

    private fun updateNotification(score: Int) {

        val status = when {
            score < 30 -> "🟢 Low Risk Area"
            score < 65 -> "🟡 Medium Risk Area"
            else -> "🔴 High Risk Area"
        }

        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        manager.notify(
            1,
            buildNotification("Current Safety: $status ($score)")
        )
    }

    private fun buildNotification(content: String): Notification {

        val intent = Intent(this, SafeModeActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Shield - Safe Mode")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Safe Mode Service",
                NotificationManager.IMPORTANCE_HIGH
            )

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}