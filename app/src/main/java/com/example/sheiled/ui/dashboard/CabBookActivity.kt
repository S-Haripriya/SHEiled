package com.example.sheiled.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sheiled.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class CabBookActivity : AppCompatActivity() {

    private lateinit var rvDrivers: RecyclerView
    private lateinit var adapter: DriverAdapter

    private val allDrivers = mutableListOf<Driver>()
    private val visibleDrivers = mutableListOf<Driver>()

    private var activeRide: Booking? = null

    private var activeRideListener: ListenerRegistration? = null
    private var driverListener: ListenerRegistration? = null
    private var notificationListener: ListenerRegistration? = null

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_cab_booking)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.menu_book -> {
                    startActivity(Intent(this, CabBookActivity::class.java))
                    true
                }

                R.id.menu_history -> {
                    startActivity(Intent(this, UserRideHistoryActivity::class.java))
                    true
                }

                else -> false
            }
        }
        rvDrivers = findViewById(R.id.rvDrivers)
        rvDrivers.layoutManager = LinearLayoutManager(this)

        adapter = DriverAdapter(visibleDrivers) { driver ->
            handleDriverAction(driver)
        }

        rvDrivers.adapter = adapter
    }

    override fun onStart() {
        super.onStart()

        listenForDrivers()
        listenForActiveRide()
        listenForBookingNotifications()
    }

    override fun onStop() {
        super.onStop()

        driverListener?.remove()
        activeRideListener?.remove()
        notificationListener?.remove()

        driverListener = null
        activeRideListener = null
        notificationListener = null
    }

    /* ================= DRIVER LISTENER ================= */

    private fun listenForDrivers() {

        driverListener?.remove()

        driverListener = db.collection("drivers")
            .addSnapshotListener { snapshot, error ->

                if (error != null || snapshot == null) return@addSnapshotListener

                allDrivers.clear()

                for (doc in snapshot.documents) {

                    val driver = doc.toObject(Driver::class.java)

                    if (driver != null) {
                        allDrivers.add(driver.copy(id = doc.id))
                    }
                }

                updateDriverList()
            }
    }

    /* ================= ACTIVE RIDE LISTENER ================= */
    private fun listenForActiveRide() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        activeRideListener?.remove()

        activeRideListener = db.collection("rides")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->

                if (error != null || snapshot == null) return@addSnapshotListener

                var latestRide: Booking? = null

                for (doc in snapshot.documents) {

                    val ride = doc.toObject(Booking::class.java) ?: continue

                    /* Ignore cancelled or rejected rides */
                    if (ride.status == "CANCELLED" || ride.status == "REJECTED") {
                        continue
                    }

                    val rideWithId = ride.copy(id = doc.id)

                    val paymentStatus = ride.paymentStatus ?: "pending"

                    val isActiveRide =
                        ride.status in listOf("REQUESTED", "ACCEPTED", "ON_TRIP") ||
                                (ride.status == "COMPLETED" && paymentStatus != "paid")

                    if (isActiveRide) {

                        if (latestRide == null ||
                            ride.createdAt > latestRide.createdAt
                        ) {
                            latestRide = rideWithId
                        }
                    }
                }

                /* Set active ride */
                activeRide = latestRide

                updateDriverList()
            }
    }

    /* ================= UPDATE DRIVER UI ================= */

    private fun updateDriverList() {

        visibleDrivers.clear()

        for (driver in allDrivers) {

            if (driver.availability == "available"

            ) {
                visibleDrivers.add(driver)
            }
            if (activeRide?.driverId == driver.id &&
                !visibleDrivers.contains(driver)
            ) {
                visibleDrivers.add(driver)
            }
        }

        adapter.setActiveRide(activeRide)
        adapter.notifyDataSetChanged()
    }

    /* ================= DRIVER ACTION ================= */

    private fun handleDriverAction(driver: Driver) {

        val ride = activeRide

        when {

            /* No active ride */
            ride == null -> {

                BookingBottomSheet
                    .newInstance(driver)
                    .show(supportFragmentManager, "BOOK")
            }

            /* Trip completed but payment pending */
            ride.driverId == driver.id &&
                    ride.status == "COMPLETED" &&
                    ride.paymentStatus != "paid" -> {

                val intent = Intent(
                    this,
                    PaymentActivity::class.java
                )

                intent.putExtra("rideId", ride.id)
                intent.putExtra("amount", ride.finalFare ?: 0.0)

                startActivity(intent)
            }

            /* Ride ongoing */
            ride.driverId == driver.id &&
                    ride.status == "ON_TRIP" -> {

                val intent = Intent(
                    this,
                    RideTrackingActivity::class.java
                )

                intent.putExtra("bookingId", ride.id)

                startActivity(intent)
            }

            /* Show ride details */
            ride.driverId == driver.id -> {

                BookingDetails
                    .newInstance(ride)
                    .show(supportFragmentManager, "DETAILS")
            }

            else -> {

                Toast.makeText(
                    this,
                    "You already have an active ride",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /* ================= USER NOTIFICATIONS ================= */

    private fun listenForBookingNotifications() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        notificationListener?.remove()

        notificationListener = db.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, error ->

                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val msg = snapshot.getString("lastNotification")

                if (msg.isNullOrEmpty()) return@addSnapshotListener

                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

                db.collection("users")
                    .document(userId)
                    .update("lastNotification", null)
            }
    }
}