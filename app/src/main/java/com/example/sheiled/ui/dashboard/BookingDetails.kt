package com.example.sheiled.ui.dashboard

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.sheiled.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class BookingDetails : BottomSheetDialogFragment() {

    private var booking: Booking? = null
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        booking = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_BOOKING, Booking::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_BOOKING)
        }

        if (booking == null) {
            dismissAllowingStateLoss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val bookingData = booking ?: return null
        val view = inflater.inflate(R.layout.booking_details, container, false)

        val formattedDate = SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            Locale.getDefault()
        ).format(Date(bookingData.createdAt))

        view.findViewById<TextView>(R.id.tvTime).text =
            "Booked at: $formattedDate"

        view.findViewById<TextView>(R.id.tvPickup).text =
            "Pickup: ${bookingData.pickupName}"

        view.findViewById<TextView>(R.id.tvDest).text =
            "Destination: ${bookingData.destName}"

        view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            showCancelConfirm(bookingData)
        }

        return view
    }

    private fun showCancelConfirm(booking: Booking) {

        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this ride?")
            .setPositiveButton("Yes") { _, _ ->
                cancelBooking(booking)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun cancelBooking(booking: Booking) {

        val fee = when (booking.status) {

            "REQUESTED" -> 0.0

            "ACCEPTED" -> 50.0

            "ON_TRIP" -> 150.0

            "COMPLETED" -> booking.finalFare ?: 0.0

            else -> 0.0
        }

        if (fee > 0) {

            val intent = Intent(
                requireContext(),
                PaymentActivity::class.java
            )

            intent.putExtra("rideId", booking.id)
            intent.putExtra("amount", fee)
            intent.putExtra("cancelRide", true)

            startActivity(intent)

            dismiss()

        } else {

            db.collection("rides")
                .document(booking.id)
                .update("status", "CANCELLED")
                .addOnSuccessListener {

                    Toast.makeText(
                        requireContext(),
                        "Ride cancelled",
                        Toast.LENGTH_SHORT
                    ).show()

                    dismissAllowingStateLoss()
                }
                .addOnFailureListener {

                    Toast.makeText(
                        requireContext(),
                        "Failed to cancel ride",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    companion object {

        private const val ARG_BOOKING = "booking"

        fun newInstance(booking: Booking): BookingDetails {

            return BookingDetails().apply {

                arguments = Bundle().apply {
                    putParcelable(ARG_BOOKING, booking)
                }
            }
        }
    }
}