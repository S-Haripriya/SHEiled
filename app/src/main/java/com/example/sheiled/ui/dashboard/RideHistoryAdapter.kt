package com.example.sheiled.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sheiled.R
import java.text.SimpleDateFormat
import java.util.*

class RideHistoryAdapter(
    private val rides: List<RideHistory>
) : RecyclerView.Adapter<RideHistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvRoute: TextView = itemView.findViewById(R.id.tvRoute)
        val tvUser: TextView = itemView.findViewById(R.id.tvUser)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvFare: TextView = itemView.findViewById(R.id.tvFare)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ride_history, parent, false)

        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {

        val ride = rides[position]

        holder.tvRoute.text = "${ride.pickupName} → ${ride.destName}"

        holder.tvUser.text = "Passenger: ${ride.userName}"

        holder.tvStatus.text = "Status: ${ride.status}"

        val earnings = when {
            ride.finalFare != null && ride.finalFare > 0 -> ride.finalFare
            ride.cancellationFee != null && ride.cancellationFee > 0 -> ride.cancellationFee
            else -> 0.0
        }

        holder.tvFare.text = "Earnings: ₹$earnings"

        val formattedDate = SimpleDateFormat(
            "dd MMM yyyy  hh:mm a",
            Locale.getDefault()
        ).format(Date(ride.createdAt))

        holder.tvDate.text = formattedDate
    }

    override fun getItemCount(): Int {
        return rides.size
    }
}