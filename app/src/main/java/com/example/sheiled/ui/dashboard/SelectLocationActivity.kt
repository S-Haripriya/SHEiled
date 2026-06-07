package com.example.sheiled.ui.dashboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.sheiled.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import java.io.IOException
import java.util.*

class SelectLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedClient: FusedLocationProviderClient

    private var selectedLatLng: LatLng? = null
    private var selectedName: String = ""

    private var marker: Marker? = null

    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnConfirm: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_select_location)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        etSearch = findViewById(R.id.etSearchLocation)
        btnSearch = findViewById(R.id.btnSearch)
        btnConfirm = findViewById(R.id.btnConfirmLocation)

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map)
                    as SupportMapFragment

        mapFragment.getMapAsync(this)

        /* -------- SEARCH BY PLACE NAME -------- */

        btnSearch.setOnClickListener {

            val locationName = etSearch.text.toString().trim()

            if (locationName.isNotEmpty()) {

                searchLocationByName(locationName)

            } else {

                Toast.makeText(
                    this,
                    "Enter location name",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        /* -------- CONFIRM LOCATION -------- */

        btnConfirm.setOnClickListener {

            if (selectedLatLng == null) {

                Toast.makeText(
                    this,
                    "Please select a location",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val data = Intent().apply {

                putExtra("lat", selectedLatLng!!.latitude)
                putExtra("lng", selectedLatLng!!.longitude)
                putExtra("placeName", selectedName)
            }

            setResult(RESULT_OK, data)

            finish()
        }
    }

    /* ================= MAP READY ================= */

    override fun onMapReady(googleMap: GoogleMap) {

        map = googleMap

        map.uiSettings.isZoomControlsEnabled = true

        requestLocationPermission()

        /* -------- USER TAP SELECT -------- */

        map.setOnMapClickListener { latLng ->

            val placeName = getAddress(latLng)

            updateMarker(latLng, placeName)
        }
    }

    /* ================= SEARCH LOCATION ================= */

    private fun searchLocationByName(locationName: String) {

        val geocoder = Geocoder(this, Locale.getDefault())

        try {

            val addressList = geocoder.getFromLocationName(locationName, 1)

            if (addressList.isNullOrEmpty()) {

                Toast.makeText(
                    this,
                    "Location not found",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            val address = addressList[0]

            val latLng = LatLng(address.latitude, address.longitude)

            val placeName = address.getAddressLine(0)

            updateMarker(latLng, placeName)

            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(latLng, 16f)
            )

        } catch (e: IOException) {

            Toast.makeText(
                this,
                "Search failed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /* ================= LOCATION PERMISSION ================= */

    private fun requestLocationPermission() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )

            return
        }

        map.isMyLocationEnabled = true

        fusedClient.lastLocation.addOnSuccessListener { location ->

            location?.let {

                val latLng = LatLng(it.latitude, it.longitude)

                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                )
            }
        }
    }

    /* ================= MARKER UPDATE ================= */

    private fun updateMarker(latLng: LatLng, title: String) {

        selectedLatLng = latLng
        selectedName = title

        marker?.remove()

        marker = map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title)
        )
    }

    /* ================= GET ADDRESS ================= */

    private fun getAddress(latLng: LatLng): String {

        return try {

            val geocoder = Geocoder(this, Locale.getDefault())

            val list = geocoder.getFromLocation(
                latLng.latitude,
                latLng.longitude,
                1
            )

            list?.get(0)?.getAddressLine(0)
                ?: "${latLng.latitude}, ${latLng.longitude}"

        } catch (e: Exception) {

            "${latLng.latitude}, ${latLng.longitude}"
        }
    }
}