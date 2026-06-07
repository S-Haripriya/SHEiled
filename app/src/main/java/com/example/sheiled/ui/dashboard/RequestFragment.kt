package com.example.sheiled.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sheiled.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class RequestFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RideRequestAdapter

    private val rideList = mutableListOf<Booking>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var launchedFromResume = false
    private var rideListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_requests,
            container,
            false
        )

        recyclerView = view.findViewById(R.id.rvRequests)

        recyclerView.layoutManager =
            LinearLayoutManager(requireContext())

        adapter = RideRequestAdapter(
            rideList,
            ::startTrip,
            ::acceptRide,
            ::rejectRide
        )

        recyclerView.adapter = adapter

        return view
    }

    /* ================= FRAGMENT RESUME ================= */

    override fun onResume() {
        super.onResume()

        if (!launchedFromResume) {
            checkActiveTrip()
        }

        listenForRideRequests()
    }

    override fun onPause() {
        super.onPause()

        rideListener?.remove()
        rideListener = null
    }

    /* ================= RIDE REQUEST LISTENER ================= */

    private fun listenForRideRequests() {

        val driverId = auth.currentUser?.uid ?: return

        rideListener?.remove()

        rideListener = db.collection("rides")
            .whereEqualTo("driverId", driverId)
            .addSnapshotListener { snapshot, _ ->

                if (!isAdded) return@addSnapshotListener

                rideList.clear(

                )

                snapshot?.documents?.forEach { doc ->

                    val ride = doc.toObject(Booking::class.java) ?: return@forEach

                    val paymentStatus = ride.paymentStatus ?: ""
                    val paymentConfirmed = ride.paymentConfirmed ?: ""

                    val showRide =
                        ride.status in listOf(
                            "REQUESTED",
                            "ACCEPTED",
                            "ARRIVED",
                            "ON_TRIP"
                        ) ||
                                (
                                        ride.status == "COMPLETED" &&
                                                 paymentConfirmed != "confirmed")


                    if (showRide) {
                        rideList.add(ride.copy(id = doc.id))
                    }
                }

                adapter.notifyDataSetChanged()
            }
    }

    /* ================= ACCEPT RIDE ================= */

    private fun acceptRide(ride: Booking) {

        val driverId = auth.currentUser?.uid ?: return

        val batch = db.batch()

        val rideRef = db.collection("rides").document(ride.id)
        val driverRef = db.collection("drivers").document(driverId)

        batch.update(rideRef, "status", "ACCEPTED")
        batch.update(driverRef, "availability", "unavailable")

        batch.commit()
    }

    /* ================= START / RESUME TRIP ================= */

    private fun startTrip(ride: Booking) {

        launchedFromResume = true
        val intent = Intent(
            requireContext(),
            DriverNavigationActivity::class.java
        )

        intent.putExtra("rideId", ride.id)

        startActivity(intent)
    }

    /* ================= REJECT RIDE ================= */

    private fun rejectRide(ride: Booking) {

        db.collection("rides")
            .document(ride.id)
            .update("status", "REJECTED")
    }

    /* ================= ACTIVE RIDE CHECK ================= */

    private fun checkActiveTrip() {

        val driverId = auth.currentUser?.uid ?: return

        db.collection("rides")
            .whereEqualTo("driverId", driverId)
            .whereIn(
                "status",
                listOf(
                    "ACCEPTED",
                    "ARRIVED",
                    "ON_TRIP"

                )
            )
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->

                if (!isAdded || snapshot.isEmpty) return@addOnSuccessListener

                val doc = snapshot.documents[0]

                val rideId = doc.id
                val status = doc.getString("status") ?: return@addOnSuccessListener
                val paymentStatus = doc.getString("paymentStatus") ?: "pending"

                when (status) {

                    "ACCEPTED", "ARRIVED", "ON_TRIP" -> {

                        val intent = Intent(
                            requireContext(),
                            DriverNavigationActivity::class.java
                        )

                        intent.putExtra("rideId", rideId)

                        launchedFromResume = true
                        startActivity(intent)
                    }

                    "COMPLETED" -> {

                    }
                }
            }
    }
}