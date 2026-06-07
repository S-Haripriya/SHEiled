package com.example.sheiled.ui.dashboard

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sheiled.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*
import java.text.DecimalFormat

class HistoryFragment : Fragment() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var tvTotalEarnings: TextView

    private val rideList = mutableListOf<RideHistory>()
    private lateinit var adapter: RideHistoryAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_history, container, false)

        rvHistory = view.findViewById(R.id.rvHistory)
        tvTotalEarnings = view.findViewById(R.id.tvTotalEarnings)


        adapter = RideHistoryAdapter(rideList)

        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = adapter

        loadHistory()

        return view
    }

    private fun loadHistory() {

        val driverId = auth.currentUser?.uid ?: return

        db.collection("rides")
            .whereEqualTo("driverId", driverId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->

                rideList.clear()

                var totalEarnings = 0.0
                var weeklyEarnings = 0.0

                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val weekStart = calendar.timeInMillis

                for (doc in snapshot.documents) {

                    val ride = doc.toObject(RideHistory::class.java) ?: continue

                    val status = ride.status ?: ""

                    if (
                        status == "COMPLETED" ||
                        status == "PAID" ||
                        status == "CONFIRMED" ||
                        status == "CANCELLED_WITH_FEE"
                    ) {

                        val fare = ride.finalFare?.toDouble() ?: 0.0
                        val cancelFee = ride.cancellationFee?.toDouble() ?: 0.0
                        val earning = if (fare > 0) fare else cancelFee

                        totalEarnings += earning

                        if (ride.createdAt >= weekStart) {
                            weeklyEarnings += earning
                        }

                        // Fetch user name using userId
                        val userId = ride.userId

                        if (userId != null) {

                            db.collection("users")
                                .document(userId)
                                .get()
                                .addOnSuccessListener { userDoc ->

                                    val userName = userDoc.getString("name") ?: "Unknown"

                                    ride.userName = userName

                                    rideList.add(ride)

                                    adapter.notifyDataSetChanged()
                                }

                        } else {

                            ride.userName = "Unknown"
                            rideList.add(ride)
                        }
                    }
                }

                val df = DecimalFormat("#.00")
                tvTotalEarnings.text = "Total Earnings: ₹${df.format(totalEarnings)}"

            }
    }
}