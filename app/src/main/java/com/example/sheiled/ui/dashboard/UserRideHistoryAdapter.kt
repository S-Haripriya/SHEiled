package com.example.sheiled.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sheiled.R
import java.text.SimpleDateFormat
import java.util.*

class UserRideHistoryAdapter(
    private val rides: List<RideHistory>
) : RecyclerView.Adapter<UserRideHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val tvDriver: TextView = view.findViewById(R.id.tvDriver)

        val tvRoute: TextView = view.findViewById(R.id.tvRoute)

        val tvFare: TextView = view.findViewById(R.id.tvFare)

        val tvStatus: TextView = view.findViewById(R.id.tvStatus)

        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_user_ride_history,
                parent,
                false
            )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val ride = rides[position]

        holder.tvDriver.text = "Driver: ${ride.driverName ?: "Unknown"}"

        holder.tvRoute.text =
            "${ride.pickupName} → ${ride.destName}"

        val date = SimpleDateFormat(
            "dd MMM yyyy  hh:mm a",
            Locale.getDefault()
        ).format(Date(ride.createdAt))

        holder.tvTime.text = date

        // Reset reused values
        holder.tvFare.text = ""
        holder.tvStatus.text = ""

        when (ride.status) {

            "COMPLETED" -> {

                val paidText =
                    if (ride.paymentStatus == "paid")
                        "Paid"
                    else
                        "Payment Pending"

                holder.tvFare.text =
                    "Fare: ₹${String.format("%.2f", ride.finalFare)}"

                holder.tvStatus.text =
                    "Trip Completed • $paidText"
            }

            "CANCELLED", "CANCELLED_WITH_FEE" -> {

                val fee = ride.cancellationFee ?: 0.0

                if (fee > 0) {
                    holder.tvFare.text =
                        "Cancellation Fee: ₹${String.format("%.2f", fee)}"
                } else {
                    holder.tvFare.text = "No cancellation fee"
                }

                holder.tvStatus.text = "Trip Cancelled"
            }

            else -> {
                holder.tvStatus.text = ride.status
            }
        }
    }

    override fun getItemCount(): Int = rides.size
}