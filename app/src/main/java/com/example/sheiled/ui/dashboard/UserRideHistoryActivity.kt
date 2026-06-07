package com.example.sheiled.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sheiled.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UserRideHistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var adapter: UserRideHistoryAdapter

    private val rideList = mutableListOf<RideHistory>()

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_user_ride_history)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.menu_book -> {

                    val intent = Intent(this, CabBookActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivity(intent)
                    true
                }

                R.id.menu_history -> {
                    true
                }

                else -> false
            }
        }

        rvHistory = findViewById(R.id.rvHistory)

        adapter = UserRideHistoryAdapter(rideList)

        rvHistory.layoutManager = LinearLayoutManager(this)

        rvHistory.adapter = adapter

        loadRideHistory()
    }

    private fun loadRideHistory() {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("rides")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->

                rideList.clear()

                for (doc in snapshot.documents) {

                    val ride = doc.toObject(RideHistory::class.java) ?: continue

                    val rideWithId = ride.copy(id = doc.id)

                    val isCompletedRide =
                        ride.status == "COMPLETED"

                    val isCancelledRide =
                        ride.status == "CANCELLED" ||
                                ride.status == "CANCELLED_WITH_FEE"

                    if (isCompletedRide || isCancelledRide) {

                        val driverId = ride.driverId

                        if (driverId != null) {

                            db.collection("drivers")
                                .document(driverId)
                                .get()
                                .addOnSuccessListener { driverDoc ->

                                    val driverName =
                                        driverDoc.getString("name") ?: "Unknown"

                                    val rideWithDriver =
                                        rideWithId.copy(driverName = driverName)

                                    rideList.add(rideWithDriver)

                                    rideList.sortByDescending { it.createdAt }

                                    adapter.notifyDataSetChanged()
                                }

                        } else {

                            rideList.add(rideWithId)

                        }
                    }
                }
            }
    }
}