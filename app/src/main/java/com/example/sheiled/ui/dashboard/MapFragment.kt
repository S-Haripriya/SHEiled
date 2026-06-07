package com.example.sheiled.ui.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.sheiled.R
import com.example.sheiled.safety.SafeModeActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread
import com.example.sheiled.ui.sos.TrackingManager
import com.example.sheiled.ui.sos.TrackingUtils
class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedClient: FusedLocationProviderClient
    private var cts = CancellationTokenSource()

    private var currentLatLng: LatLng? = null
    private var currentPolyline: Polyline? = null
    private val markersList = mutableListOf<Marker>()

    private lateinit var searchBar: LinearLayout
    private lateinit var safeContainer: LinearLayout

    private var lastFiredLocation: Location? = null
    private var isFetching = false
    private lateinit var btnShareFloat: ImageButton
    private lateinit var btnNearbyFloat: ImageButton
    private var nearbyVisible = false
    private lateinit var placeScroll: ScrollView
    private lateinit var placesLabel: TextView
    private lateinit var placesPanel: LinearLayout
    private lateinit var btnStopShare: ImageButton
    private val trackingHandler = Handler(Looper.getMainLooper())
    private var trackingRunnable: Runnable? = null

    private var dX = 0f
    private var dY = 0f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.layout_map, container, false)

        searchBar = view.findViewById(R.id.searchBar)
        safeContainer = view.findViewById(R.id.safePlacesContainer)
        btnShareFloat = view.findViewById(R.id.btnShareFloat)
        btnNearbyFloat = view.findViewById(R.id.btnNearbyFloat)
        placeScroll = view.findViewById(R.id.placeScroll)
        placesLabel = view.findViewById(R.id.placesLabel)
        placesPanel = view.findViewById(R.id.placesPanel)
        btnNearbyFloat.bringToFront()
        btnShareFloat.bringToFront()
        btnStopShare = view.findViewById(R.id.btnStopShare)
        btnStopShare.bringToFront()

        makeDraggable(btnStopShare) {
            stopLiveTracking()
        }
        val btnSafeMode = view.findViewById<ImageButton>(R.id.btnSafeMode)
        fusedClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also {
                childFragmentManager.beginTransaction().replace(R.id.mapContainer, it).commit()
            }
        mapFragment.getMapAsync(this)

        searchBar.setOnClickListener {
            showRouteDialog()
        }

        handleBackNavigation()
        makeDraggable(btnShareFloat) {
            shareLocationAction()
        }
        makeDraggable(btnSafeMode) {
            startActivity(Intent(requireContext(), SafeModeActivity::class.java))
        }

        makeDraggable(btnNearbyFloat) {
            if (!nearbyVisible) {

                placesPanel.visibility = View.VISIBLE
                placesPanel.alpha = 0f
                placesPanel.animate().alpha(1f).setDuration(250).start()


                refreshSafetyData()
                nearbyVisible = true

            } else {
                clearNearbyPlaces()
            }
        }

        return view
    }

    private fun handleBackNavigation() {

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    when {
                        currentPolyline != null -> {
                            currentPolyline?.remove()
                            currentPolyline = null
                            Toast.makeText(requireContext(), "Route cleared", Toast.LENGTH_SHORT).show()
                        }

                        nearbyVisible -> {
                            clearNearbyPlaces()
                        }

                        else -> {
                            isEnabled = false
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            })
    }


    @SuppressLint("MissingInflatedId")
    private fun showRouteDialog() {

        val dialogView = layoutInflater.inflate(R.layout.dialog_route_search, null)

        val etFrom = dialogView.findViewById<EditText>(R.id.etFrom)
        val etTo = dialogView.findViewById<EditText>(R.id.etTo)
        val btnUseCurrent = dialogView.findViewById<Button>(R.id.btnUseCurrent)

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Find Route")
            .setView(dialogView)
            .setPositiveButton("Navigate") { _, _ ->

                val fromText = etFrom.text.toString()
                val toText = etTo.text.toString()

                if (toText.isEmpty()) {
                    Toast.makeText(requireContext(),"Enter destination",Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                thread {

                    val geocoder = Geocoder(requireContext(), Locale.getDefault())

                    val fromLatLng = if (fromText == "Current Location") {
                        currentLatLng
                    } else {
                        val fromAddress = geocoder.getFromLocationName(fromText,1)
                        if (!fromAddress.isNullOrEmpty())
                            LatLng(fromAddress[0].latitude, fromAddress[0].longitude)
                        else null
                    }

                    val toAddress = geocoder.getFromLocationName(toText,1)

                    if (!toAddress.isNullOrEmpty() && fromLatLng != null) {

                        val dest = LatLng(
                            toAddress[0].latitude,
                            toAddress[0].longitude
                        )

                        requireActivity().runOnUiThread {

                            markersList.forEach { it.remove() }
                            markersList.clear()

                            val marker = map.addMarker(
                                MarkerOptions()
                                    .position(dest)
                                    .title(toText)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            )

                            marker?.let { markersList.add(it) }

                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(dest,14f))

                            drawRouteFrom(fromLatLng,dest)
                        }
                    }
                }
            }
            .setNegativeButton("Cancel",null)
            .create()

        btnUseCurrent.setOnClickListener {
            etFrom.setText("Current Location")
        }

        dialog.show()
    }
    private fun drawRouteFrom(origin: LatLng, dest: LatLng) {

        val apiKey = getString(R.string.google_maps_key)

        val url =
            "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${dest.latitude},${dest.longitude}" +
                    "&key=$apiKey"

        thread {
            try {

                val connection = URL(url).openConnection() as java.net.HttpURLConnection
                val response = connection.inputStream.bufferedReader().readText()

                val json = JSONObject(response)

                if (json.getString("status") == "OK") {

                    val route = json.getJSONArray("routes").getJSONObject(0)
                    val polylineEncoded =
                        route.getJSONObject("overview_polyline").getString("points")

                    val path = PolyUtil.decode(polylineEncoded)

                    requireActivity().runOnUiThread {

                        currentPolyline?.remove()

                        currentPolyline = map.addPolyline(
                            PolylineOptions()
                                .addAll(path)
                                .color(Color.parseColor("#6200EE"))
                                .width(14f)
                                .geodesic(true)
                        )
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun drawRoute(dest: LatLng) {
        val origin = currentLatLng ?: return
        val apiKey = getString(R.string.google_maps_key)
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origin.latitude},${origin.longitude}" +
                "&destination=${dest.latitude},${dest.longitude}" +
                "&key=$apiKey"

        thread {
            try {
                val connection = URL(url).openConnection() as java.net.HttpURLConnection



                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                if (json.getString("status") == "OK") {
                    val route = json.getJSONArray("routes").getJSONObject(0)
                    val polylineEncoded = route.getJSONObject("overview_polyline").getString("points")
                    val path = PolyUtil.decode(polylineEncoded)

                    requireActivity().runOnUiThread {
                        currentPolyline?.remove()
                        currentPolyline = map.addPolyline(PolylineOptions()
                            .addAll(path)
                            .color(Color.parseColor("#6200EE"))
                            .width(14f)
                            .geodesic(true))
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        updateLocationUI()
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        map.isMyLocationEnabled = true
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                location?.let {
                    currentLatLng = LatLng(it.latitude, it.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng!!, 15f))

                }
            }
    }

    private fun refreshSafetyData() {
        val currentLoc = currentLatLng ?: return
        val lastLoc = lastFiredLocation

        // 1. Convert LatLng to Location for distance check
        val newLocation = Location("").apply {
            latitude = currentLoc.latitude
            longitude = currentLoc.longitude
        }


        // 2. Only fetch if we haven't fetched yet OR user moved > 500 meters
        if (lastLoc == null || newLocation.distanceTo(lastLoc) > 500) {
            if (isFetching) return // Already a thread running? Skip.

            isFetching = true
            lastFiredLocation = newLocation

            markersList.forEach { it.remove() }
            markersList.clear()
            safeContainer.removeAllViews()

            listOf("police", "hospital", "pharmacy").forEach { type ->
                fetchPlacesApi(type)
            }
            listOf(
                "womens hostel",
                "ladies hostel",
                "women hostel",
                "girls hostel",
                "ladies pg",
                "working women hostel"
            ).forEach { keyword ->
                fetchPlacesApiByKeyword(keyword)
            }
            // Reset flag after a delay to allow requests to finish
            view?.postDelayed({ isFetching = false }, 2000)
        }


    }

    private fun fetchPlacesApi(type: String) {
        val apiKey = getString(R.string.google_maps_key)
        val loc = currentLatLng ?: return

        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${loc.latitude},${loc.longitude}" +
                "&radius=3000&type=$type&key=$apiKey"

        thread {
            try {
                val connection = URL(url).openConnection() as java.net.HttpURLConnection
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                val status = json.getString("status")
                Log.d("PLACES_STATUS", "$type -> $status")

                if (status != "OK") {
                    return@thread
                }

                val results = json.getJSONArray("results")

                if (isAdded) {
                    requireActivity().runOnUiThread {

                        val limit = minOf(3, results.length())

                        for (i in 0 until limit) {
                            addPlaceToUI(results.getJSONObject(i))
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private fun fetchPlacesApiByKeyword(keyword: String) {

        val apiKey = getString(R.string.google_maps_key)
        val loc = currentLatLng ?: return

        val url =
            "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                    "location=${loc.latitude},${loc.longitude}" +
                    "&radius=9000&keyword=$keyword&key=$apiKey"

        thread {
            try {

                val connection = URL(url).openConnection() as java.net.HttpURLConnection
                val response = connection.inputStream.bufferedReader().readText()

                val json = JSONObject(response)

                val status = json.getString("status")

                if (status != "OK") return@thread

                val results = json.getJSONArray("results")

                if (isAdded) {

                    requireActivity().runOnUiThread {

                        val limit = minOf(3, results.length())

                        for (i in 0 until limit) {
                            addPlaceToUI(results.getJSONObject(i))
                        }
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- UPDATED: DISTANCE CALCULATION RESTORED ---
    private fun addPlaceToUI(json: JSONObject) {
        val name = json.getString("name")
        val locJson = json.getJSONObject("geometry").getJSONObject("location")
        val destLatLng = LatLng(locJson.getDouble("lat"), locJson.getDouble("lng"))

        // Add Marker
        val marker = map.addMarker(MarkerOptions()
            .position(destLatLng)
            .title(name)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
        marker?.let { markersList.add(it) }

        // Inflate List Item
        val itemView = layoutInflater.inflate(R.layout.item_safe_place, safeContainer, false)
        itemView.findViewById<TextView>(R.id.tvPlaceName).text = name

        // --- DISTANCE LOGIC ---
        val tvDistance = itemView.findViewById<TextView>(R.id.tvDistance)

        var distanceInMeters = 0.0

        currentLatLng?.let { userPos ->

            val results = FloatArray(1)

            Location.distanceBetween(
                userPos.latitude,
                userPos.longitude,
                destLatLng.latitude,
                destLatLng.longitude,
                results
            )

            distanceInMeters = results[0].toDouble()

            val distanceInKm = distanceInMeters / 1000.0

            tvDistance.text = String.format(
                Locale.getDefault(),
                "%.2f km away",
                distanceInKm
            )
        }


        itemView.findViewById<Button>(R.id.btnNavigate).setOnClickListener {
            drawRoute(destLatLng)
        }
        safeContainer.addView(itemView)


    }

    @SuppressLint("ClickableViewAccessibility")
    private fun makeDraggable(view: View, onClick: (() -> Unit)? = null) {

        view.setOnTouchListener(object : View.OnTouchListener {

            private var dX = 0f
            private var dY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {

                    MotionEvent.ACTION_DOWN -> {
                        dX = v.x - event.rawX
                        dY = v.y - event.rawY
                    }

                    MotionEvent.ACTION_MOVE -> {
                        v.animate()
                            .x(event.rawX + dX)
                            .y(event.rawY + dY)
                            .setDuration(0)
                            .start()
                    }

                    MotionEvent.ACTION_UP -> {
                        val clickDuration = event.eventTime - event.downTime
                        if (clickDuration < 200) {
                            onClick?.invoke()
                        }
                    }
                }
                return true
            }
        })
    }


    private fun clearNearbyPlaces() {

        markersList.forEach { it.remove() }
        markersList.clear()

        safeContainer.removeAllViews()

        currentPolyline?.remove()
        currentPolyline = null
        placesPanel.animate()
            .alpha(0f)
            .setDuration(250)
            .withEndAction {
                placesPanel.visibility = View.GONE
            }.start()


        placesLabel.visibility = View.GONE
        nearbyVisible = false
        lastFiredLocation = null
        Toast.makeText(requireContext(), "Nearby Places Cleared", Toast.LENGTH_SHORT).show()
    }



    private fun shareLocationAction() {

        // Start tracking
        TrackingManager.startTracking(requireContext())

        // Show stop button
        btnStopShare.visibility = View.VISIBLE

        // Cancel previous timer if any
        trackingRunnable?.let {
            trackingHandler.removeCallbacks(it)
        }

        // Auto stop after 30 minutes
        trackingRunnable = Runnable {
            stopLiveTracking()
        }

        trackingHandler.postDelayed(
            trackingRunnable!!,
            30 * 60 * 1000
        )

        val liveLink = TrackingUtils.getTrackingLink()

        val shareMessage = """
🚨 SHEiled Live Safety Tracking

Track my location in real time:
$liveLink
""".trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }

        startActivity(Intent.createChooser(intent, "Share via"))
    }
    private fun stopLiveTracking() {

        // Stop foreground tracking service
        TrackingManager.stopTracking(requireContext())

        // Hide stop button
        btnStopShare.visibility = View.GONE

        // Cancel 30 min timer
        trackingRunnable?.let {
            trackingHandler.removeCallbacks(it)
        }

        Toast.makeText(
            requireContext(),
            "Live tracking stopped",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cts.cancel()
    }
}

