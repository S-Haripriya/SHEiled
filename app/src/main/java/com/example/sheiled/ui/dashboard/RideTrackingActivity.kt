package com.example.sheiled.ui.dashboard

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sheiled.R
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread
import com.google.android.material.floatingactionbutton.FloatingActionButton

class RideTrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var rideId: String

    private val db = FirebaseFirestore.getInstance()
    private var rideListener: ListenerRegistration? = null

    private var driverMarker: Marker? = null
    private var pickupMarker: Marker? = null
    private var destMarker: Marker? = null

    private var polyline: Polyline? = null

    private var pickupLatLng: LatLng? = null
    private var destLatLng: LatLng? = null
    private var rideStatus: String = ""
    /* ROUTE CACHE */
    private var routePoints: List<LatLng> = emptyList()

    /* API THROTTLE */
    private var lastRouteUpdate = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_ride_tracking)

        rideId = intent.getStringExtra("bookingId") ?: run {
            finish()
            return
        }
        listenForPayment()
        val shareBtn = findViewById<FloatingActionButton>(R.id.btnShareRide)

        shareBtn.setOnClickListener {
            shareTrackingLink()
        }

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map)
                    as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {

        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true

        listenToRideUpdates()
    }

    /* ================= FIRESTORE REALTIME ================= */

    private fun listenToRideUpdates() {

        rideListener = db.collection("rides")
            .document(rideId)
            .addSnapshotListener { doc, _ ->

                if (doc == null || !doc.exists()) return@addSnapshotListener

                val newStatus = doc.getString("status") ?: return@addSnapshotListener

                if (newStatus != rideStatus) {
                    routePoints = emptyList()
                }

                rideStatus = newStatus

                if (pickupLatLng == null) {

                    val pLat = doc.getDouble("pickupLat")
                    val pLng = doc.getDouble("pickupLng")
                    val dLat = doc.getDouble("destLat")
                    val dLng = doc.getDouble("destLng")

                    if (pLat == null || pLng == null || dLat == null || dLng == null) {
                        return@addSnapshotListener
                    }

                    pickupLatLng = LatLng(pLat, pLng)
                    destLatLng = LatLng(dLat, dLng)

                    showPickupDestination()
                }

                val driverLoc =
                    doc.get("driverLocation") as? Map<*, *> ?: return@addSnapshotListener

                val lat = driverLoc["lat"] as? Double ?: return@addSnapshotListener
                val lng = driverLoc["lng"] as? Double ?: return@addSnapshotListener

                val driverPos = LatLng(lat, lng)

                updateDriverMarker(driverPos)

                updateRouteIfNeeded(driverPos)

                if (rideStatus == "COMPLETED") {

                    val finalFare = doc.getDouble("finalFare") ?: 0.0

                    Toast.makeText(
                        this,
                        "Trip Completed\nFare: ₹$finalFare",
                        Toast.LENGTH_LONG
                    ).show()

                    rideListener?.remove()
                    finish()
                }
            }
    }

    /* ================= MARKERS ================= */

    private fun showPickupDestination() {

        val pickup = pickupLatLng ?: return
        val dest = destLatLng ?: return

        pickupMarker = map.addMarker(
            MarkerOptions()
                .position(pickup)
                .title("Pickup")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )

        destMarker = map.addMarker(
            MarkerOptions()
                .position(dest)
                .title("Destination")
        )

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(pickup, 14f)
        )
    }

    /* ================= DRIVER MARKER ANIMATION ================= */

    private fun updateDriverMarker(newPosition: LatLng) {

        if (driverMarker == null) {

            driverMarker = map.addMarker(
                MarkerOptions()
                    .position(newPosition)
                    .title("Driver")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )

            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(newPosition, 16f)
            )

            return
        }

        val start = driverMarker!!.position
        val end = newPosition

        val animator = ValueAnimator.ofFloat(0f, 1f)

        animator.duration = 1500
        animator.interpolator = LinearInterpolator()

        animator.addUpdateListener {

            val fraction = it.animatedFraction

            val lat = (end.latitude - start.latitude) * fraction + start.latitude
            val lng = (end.longitude - start.longitude) * fraction + start.longitude

            val newLatLng = LatLng(lat, lng)

            driverMarker!!.position = newLatLng
        }

        animator.start()
    }

    /* ================= ROUTE UPDATE CONTROL ================= */

    private fun updateRouteIfNeeded(driverPos: LatLng) {

        val now = System.currentTimeMillis()

        if (routePoints.isEmpty() ||
            isDriverOffRoute(driverPos) ||
            now - lastRouteUpdate > 15000
        ) {

            drawActualRoute(driverPos)

            lastRouteUpdate = now
        }
    }

    /* ================= DEVIATION DETECTION ================= */

    private fun isDriverOffRoute(driver: LatLng): Boolean {

        if (routePoints.isEmpty()) return true

        var minDistance = Double.MAX_VALUE

        for (point in routePoints) {

            val results = FloatArray(1)

            Location.distanceBetween(
                driver.latitude,
                driver.longitude,
                point.latitude,
                point.longitude,
                results
            )

            if (results[0] < minDistance) {
                minDistance = results[0].toDouble()
            }
        }

        return minDistance > 50
    }

    /* ================= DIRECTIONS API ================= */
    private fun drawActualRoute(driverPos: LatLng) {

        val apiKey = getString(R.string.google_maps_key)

        val destination = when (rideStatus) {
            "ON_TRIP" -> destLatLng
            else -> pickupLatLng
        } ?: return

        val url =
            "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=${driverPos.latitude},${driverPos.longitude}" +
                    "&destination=${destination.latitude},${destination.longitude}" +
                    "&mode=driving&key=$apiKey"

        thread {

            try {

                val response = URL(url).readText()

                val json = JSONObject(response)

                val routes = json.getJSONArray("routes")

                if (routes.length() == 0) return@thread

                val points =
                    routes.getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points")

                val decodedPath = decodePolyline(points)

                routePoints = decodedPath

                runOnUiThread {

                    polyline?.remove()

                    polyline = map.addPolyline(
                        PolylineOptions()
                            .addAll(decodedPath)
                            .width(10f)
                            .color(Color.BLUE)
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    /* ================= POLYLINE DECODER ================= */

    private fun decodePolyline(encoded: String): List<LatLng> {

        val poly = ArrayList<LatLng>()

        var index = 0
        val len = encoded.length

        var lat = 0
        var lng = 0

        while (index < len) {

            var b: Int
            var shift = 0
            var result = 0

            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlat =
                if (result and 1 != 0) (result shr 1).inv()
                else result shr 1

            lat += dlat

            shift = 0
            result = 0

            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)

            val dlng =
                if (result and 1 != 0) (result shr 1).inv()
                else result shr 1

            lng += dlng

            val p = LatLng(
                lat / 1E5,
                lng / 1E5
            )

            poly.add(p)
        }

        return poly
    }

    /* ================= SHARE RIDE ================= */

    private fun shareTrackingLink() {

        val trackingLink =
            "https://sheiled.web.app/Ride-Tracking-web/track.html?rideId=$rideId"

        val intent = Intent(Intent.ACTION_SEND)

        intent.type = "text/plain"

        intent.putExtra(
            Intent.EXTRA_TEXT,
            """
🚕 Live Ride Tracking

Track my cab in real time:
$trackingLink
""".trimIndent()
        )

        startActivity(Intent.createChooser(intent, "Share via"))
    }

    override fun onDestroy() {
        super.onDestroy()
        rideListener?.remove()
    }
    private fun listenForPayment() {

        db.collection("rides")
            .document(rideId)
            .addSnapshotListener { doc, _ ->

                if (doc == null || !doc.exists()) return@addSnapshotListener

                val status = doc.getString("status")
                val paymentStatus = doc.getString("paymentStatus")

                if (status == "COMPLETED" && paymentStatus == "pending") {

                    val fare = doc.getDouble("finalFare") ?: 0.0

                    val intent = Intent(
                        this,
                        PaymentActivity::class.java
                    )

                    intent.putExtra("rideId", rideId)
                    intent.putExtra("amount", fare)

                    startActivity(intent)
                }
            }
    }
}