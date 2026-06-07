package com.example.sheiled.ui.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.sheiled.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class DriverNavigationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var btnAction: Button
    private lateinit var tvRideInfo: TextView

    private lateinit var rideId: String
    private var ride: Booking? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var fusedLocation: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var driverMarker: Marker? = null
    private var pickupMarker: Marker? = null
    private var destMarker: Marker? = null

    private var routePolyline: Polyline? = null

    /* NEW: Track route status to avoid repeated API calls */
    private var lastRouteStatus: String? = null

    private var tripStartTime: Long = 0

    private lateinit var directionsKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_driver_navigation)

        rideId = intent.getStringExtra("rideId")!!
        if (savedInstanceState != null) {
            listenRide()
        }
        btnAction = findViewById(R.id.btnAction)
        tvRideInfo = findViewById(R.id.tvRideInfo)

        fusedLocation = LocationServices.getFusedLocationProviderClient(this)

        directionsKey = getString(R.string.google_maps_key)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
        listenRide()
    }

    /* ---------------- RIDE LISTENER ---------------- */

    private fun listenRide() {

        db.collection("rides")
            .document(rideId)
            .addSnapshotListener { doc, _ ->

                ride = doc?.toObject(Booking::class.java)?.copy(id = doc.id)

                updateUI()
                updateMarkers()

                ride?.let {

                    if (it.status != lastRouteStatus) {

                        lastRouteStatus = it.status

                        driverMarker?.position?.let { driverPos ->
                            drawRoute(driverPos)
                        }
                    }
                }
            }
    }

    override fun onMapReady(googleMap: GoogleMap) {

        map = googleMap
        startLocationUpdates()
    }

    /* ---------------- DRIVER LOCATION ---------------- */

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {

        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000
        ).build()

        locationCallback = object : LocationCallback() {

            override fun onLocationResult(result: LocationResult) {

                val location = result.lastLocation ?: return

                val latLng = LatLng(location.latitude, location.longitude)

                updateDriverMarker(latLng)

                updateDriverLocationFirestore(latLng)
            }
        }

        fusedLocation.requestLocationUpdates(
            request,
            locationCallback,
            mainLooper
        )
    }

    private fun updateDriverMarker(latLng: LatLng) {

        if (driverMarker == null) {

            driverMarker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Driver")
                    .icon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_BLUE
                        )
                    )
            )

            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(latLng, 15f)
            )
            drawRoute(latLng)
        } else {

            driverMarker!!.position = latLng
        }
    }

    private fun updateDriverLocationFirestore(latLng: LatLng) {

        db.collection("rides")
            .document(rideId)
            .update(
                "driverLocation",
                mapOf(
                    "lat" to latLng.latitude,
                    "lng" to latLng.longitude
                )
            )
    }

    /* ---------------- MARKERS ---------------- */

    private fun updateMarkers() {

        val rideData = ride ?: return

        val pickup = LatLng(rideData.pickupLat, rideData.pickupLng)
        val dest = LatLng(rideData.destLat, rideData.destLng)

        if (pickupMarker == null) {

            pickupMarker = map.addMarker(
                MarkerOptions()
                    .position(pickup)
                    .title("Pickup")
                    .icon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN
                        )
                    )
            )
        }

        if (destMarker == null) {

            destMarker = map.addMarker(
                MarkerOptions()
                    .position(dest)
                    .title("Destination")
                    .icon(
                        BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED
                        )
                    )
            )
        }
    }

    /* ---------------- ROUTE ---------------- */

    private fun drawRoute(driverPos: LatLng) {

        val rideData = ride ?: return

        val destination = when (rideData.status) {

            "ACCEPTED" -> LatLng(rideData.pickupLat, rideData.pickupLng)

            "ARRIVED" -> {

                runOnUiThread {
                    routePolyline?.remove()
                    routePolyline = null
                }

                return
            }

            "ON_TRIP" -> LatLng(rideData.destLat, rideData.destLng)

            else -> return
        }

        val url =
            "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=${driverPos.latitude},${driverPos.longitude}" +
                    "&destination=${destination.latitude},${destination.longitude}" +
                    "&mode=driving&key=$directionsKey"

        val request = Request.Builder().url(url).build()

        OkHttpClient().newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {

                val json = JSONObject(response.body!!.string())

                val routes = json.getJSONArray("routes")

                if (routes.length() == 0) return

                val leg = routes.getJSONObject(0)
                    .getJSONArray("legs")
                    .getJSONObject(0)

                val distanceMeters =
                    leg.getJSONObject("distance").getInt("value")

                val durationSeconds =
                    leg.getJSONObject("duration").getInt("value")

                val polyline =
                    routes.getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points")

                val points = decodePolyline(polyline)

                runOnUiThread {

                    routePolyline?.remove()

                    routePolyline = map.addPolyline(
                        PolylineOptions()
                            .addAll(points)
                            .width(10f)
                            .color(android.graphics.Color.BLUE)
                    )

                    val builder = LatLngBounds.Builder()
                    points.forEach { builder.include(it) }

                    val bounds = builder.build()

                    map.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(bounds, 120)
                    )
                }

                if (ride?.status == "ON_TRIP") {
                    calculateFare(distanceMeters, durationSeconds)
                }
            }
        })
    }

    /* ---------------- FARE CALCULATION ---------------- */

    private fun calculateFare(distanceMeters: Int, durationSeconds: Int) {

        val distanceKm = distanceMeters / 1000.0
        val durationMinutes = durationSeconds / 60.0

        val baseFare = 50
        val perKm = 12
        val perMinute = 2

        val fare =
            baseFare +
                    (distanceKm * perKm) +
                    (durationMinutes * perMinute)

        db.collection("rides")
            .document(rideId)
            .update(
                mapOf(
                    "tripDistanceKm" to distanceKm,
                    "tripDurationMinutes" to durationMinutes,
                    "finalFare" to fare
                )
            )
    }

    /* ---------------- UI STATE ---------------- */

    private fun updateUI() {

        val rideData = ride ?: return

        tvRideInfo.text =
            "${rideData.pickupName} → ${rideData.destName}"

        when (rideData.status) {

            "ACCEPTED" -> {

                btnAction.text = "ARRIVED"

                btnAction.setOnClickListener {

                    db.collection("rides")
                        .document(rideId)
                        .update("status", "ARRIVED")
                }
            }

            "ARRIVED" -> {

                btnAction.text = "START TRIP"

                btnAction.setOnClickListener {

                    tripStartTime = System.currentTimeMillis()

                    db.collection("rides")
                        .document(rideId)
                        .update(
                            mapOf(
                                "status" to "ON_TRIP",
                                "tripStartTime" to tripStartTime,
                                "trackingEnabled" to true
                            )
                        )
                }
            }

            "ON_TRIP" -> {

                btnAction.text = "COMPLETE TRIP"

                btnAction.setOnClickListener {

                    val endTime = System.currentTimeMillis()

                    val rideData = ride ?: return@setOnClickListener

                    db.collection("rides")
                        .document(rideId)
                        .update(
                            mapOf(
                                "status" to "COMPLETED",
                                "tripEndTime" to endTime,
                                "paymentStatus" to "pending",
                                "trackingEnabled" to false
                            )
                        )

                    db.collection("drivers")
                        .document(auth.currentUser!!.uid)
                        .update("availability", "available")

                    val intent =
                        Intent(this, DriverPaymentActivity::class.java)

                    intent.putExtra("rideId", rideId)

                    startActivity(intent)

                    finish()
                }
            }
        }
    }

    /* ---------------- POLYLINE DECODER ---------------- */

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
                if ((result and 1) != 0) (result shr 1).inv()
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
                if ((result and 1) != 0) (result shr 1).inv()
                else result shr 1

            lng += dlng

            poly.add(
                LatLng(
                    lat.toDouble() / 1E5,
                    lng.toDouble() / 1E5
                )
            )
        }

        return poly
    }

    override fun onDestroy() {

        super.onDestroy()

        fusedLocation.removeLocationUpdates(locationCallback)
    }
}