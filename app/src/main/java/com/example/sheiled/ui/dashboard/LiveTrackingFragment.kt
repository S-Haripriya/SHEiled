package com.example.sheiled.ui.dashboard


import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.sheiled.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class LiveTrackingFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var driverMarker: Marker? = null

    private val db = FirebaseFirestore.getInstance()
    private lateinit var rideId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view =
            inflater.inflate(R.layout.status_tracker, container, false)

        // 🔑 rideId MUST be passed
        rideId = requireArguments().getString("rideId")
            ?: throw IllegalArgumentException("rideId missing")

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map)
                    as SupportMapFragment
        mapFragment.getMapAsync(this)

        view.findViewById<Button>(R.id.btnShareTracking)
            .setOnClickListener { shareTrackingLink() }

        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        listenToRideUpdates()
    }

    /* ================= FIRESTORE LISTENER ================= */

    private fun listenToRideUpdates() {

        db.collection("rides")
            .document(rideId)
            .addSnapshotListener { doc, error ->

                if (error != null || doc == null || !doc.exists()) return@addSnapshotListener

                val trackingEnabled =
                    doc.getBoolean("trackingEnabled") ?: false

                if (!trackingEnabled) {
                    Toast.makeText(
                        requireContext(),
                        "Ride completed. Tracking stopped.",
                        Toast.LENGTH_LONG
                    ).show()

                    driverMarker?.remove()
                    return@addSnapshotListener
                }

                val loc =
                    doc.get("driverLocation") as Map<*, *>? ?: return@addSnapshotListener

                val lat = loc["lat"] as? Double ?: return@addSnapshotListener
                val lng = loc["lng"] as? Double ?: return@addSnapshotListener

                val pos = LatLng(lat, lng)

                if (driverMarker == null) {
                    driverMarker = googleMap.addMarker(
                        MarkerOptions()
                            .position(pos)
                            .title("Driver Location")
                    )

                    googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(pos, 16f)
                    )
                } else {
                    driverMarker!!.position = pos
                }
            }
    }

    /* ================= SHARE LINK ================= */

    private fun shareTrackingLink() {

        // 🔗 universal link (guardian / friend)
        val trackingLink =
            "https://sheiled.app/track?rideId=$rideId"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                """
🚕 Live Ride Tracking

Track my cab in real time using the link below:
$trackingLink

⛔ Tracking will stop once the ride ends.
""".trimIndent()
            )
        }

        startActivity(Intent.createChooser(intent, "Share via"))
    }
}
