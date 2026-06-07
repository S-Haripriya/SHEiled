package com.example.sheiled.ui.dashboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.sheiled.R
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BookingBottomSheet : BottomSheetDialogFragment() {

    private lateinit var driver: Driver

    private var pickupLatLng: LatLng? = null
    private var destLatLng: LatLng? = null

    private var pickupName: String = ""
    private var destName: String = ""

    private lateinit var tvPickup: TextView
    private lateinit var tvDest: TextView

    private val PICKUP_REQUEST = 101
    private val DEST_REQUEST = 102

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        driver = requireArguments().getParcelable("driver")!!
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.bottom_sheet_booking,
            container,
            false
        )

        val btnPickup = view.findViewById<Button>(R.id.btnSelectPickup)
        val btnDest = view.findViewById<Button>(R.id.btnSelectDestination)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmBooking)

        tvPickup = view.findViewById(R.id.tvPickupLocation)
        tvDest = view.findViewById(R.id.tvDestLocation)

        btnPickup.setOnClickListener {
            openMap(PICKUP_REQUEST)
        }

        btnDest.setOnClickListener {
            openMap(DEST_REQUEST)
        }

        btnConfirm.setOnClickListener {

            if (pickupLatLng == null || destLatLng == null) {

                Toast.makeText(
                    requireContext(),
                    "Select pickup & destination",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            checkActiveRideAndCreate()
        }

        return view
    }

    private fun openMap(code: Int) {

        startActivityForResult(
            Intent(
                requireContext(),
                SelectLocationActivity::class.java
            ),
            code
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        if (resultCode != Activity.RESULT_OK || data == null) return

        val lat = data.getDoubleExtra("lat", 0.0)
        val lng = data.getDoubleExtra("lng", 0.0)

        val name = data.getStringExtra("placeName") ?: "$lat,$lng"

        if (requestCode == PICKUP_REQUEST) {

            pickupLatLng = LatLng(lat, lng)
            pickupName = name

            tvPickup.text = "Pickup: $pickupName"

        } else if (requestCode == DEST_REQUEST) {

            destLatLng = LatLng(lat, lng)
            destName = name

            tvDest.text = "Destination: $destName"
        }
    }

    /* ================= ACTIVE RIDE CHECK ================= */

    private fun checkActiveRideAndCreate() {

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        db.collection("rides")
            .whereEqualTo("userId", userId)
            .whereIn(
                "status",
                listOf("REQUESTED", "ACCEPTED", "ON_TRIP")
            )
            .get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.isEmpty) {

                    Toast.makeText(
                        requireContext(),
                        "You already have an active ride",
                        Toast.LENGTH_LONG
                    ).show()

                    dismiss()
                    return@addOnSuccessListener
                }

                createBookingRequest()
            }
    }

    /* ================= CREATE RIDE ================= */

    private fun createBookingRequest() {

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { userDoc ->

                val userName = userDoc.getString("name") ?: "User"

                val booking = hashMapOf(

                    "userId" to userId,
                    "userName" to userName,

                    "driverId" to driver.id,
                    "driverName" to driver.name,   // from Driver object

                    "pickupLat" to pickupLatLng!!.latitude,
                    "pickupLng" to pickupLatLng!!.longitude,
                    "pickupName" to pickupName,

                    "destLat" to destLatLng!!.latitude,
                    "destLng" to destLatLng!!.longitude,
                    "destName" to destName,

                    "status" to "REQUESTED",
                    "createdAt" to System.currentTimeMillis(),

                    "paymentStatus" to "pending",
                    "paymentConfirmed" to "pending",
                    "trackingEnabled" to false,
                    "driverLocation" to null,
                    "tripStartTime" to null,
                    "tripEndTime" to null,
                    "finalFare" to null
                )
val ref = db.collection("rides").document()
              booking["id"] = ref.id
                  ref.set(booking)
                    .addOnSuccessListener {

                        Toast.makeText(
                            requireContext(),
                            "Ride request sent",
                            Toast.LENGTH_SHORT
                        ).show()

                        dismiss()
                    }
                    .addOnFailureListener {

                        Toast.makeText(
                            requireContext(),
                            "Failed to book ride",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
    }

    companion object {

        fun newInstance(driver: Driver): BookingBottomSheet {

            return BookingBottomSheet().apply {

                arguments = Bundle().apply {
                    putParcelable("driver", driver)
                }
            }
        }
    }
}