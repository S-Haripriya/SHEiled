package com.example.sheiled.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sheiled.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class RideRequestAdapter(
    private val rides: List<Booking>,
    private val onStartTrip: (Booking) -> Unit,
    private val onAccept: (Booking) -> Unit,
    private val onReject: (Booking) -> Unit
) : RecyclerView.Adapter<RideRequestAdapter.RequestViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    inner class RequestViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvUserPhone: TextView = view.findViewById(R.id.tvUserPhone)

        val tvPickup: TextView = view.findViewById(R.id.tvPickup)
        val tvDestination: TextView = view.findViewById(R.id.tvDestination)
        val tvTime: TextView = view.findViewById(R.id.tvTime)

        val btnCall: Button = view.findViewById(R.id.btnCall)
        val btnAccept: Button = view.findViewById(R.id.btnAccept)
        val btnReject: Button = view.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RequestViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ride_request, parent, false)

        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {

        val ride = rides[position]
        holder.btnAccept.visibility = View.VISIBLE
        holder.btnReject.visibility = View.VISIBLE
        holder.tvPickup.text = "Pickup: ${ride.pickupName}"
        holder.tvDestination.text = "Destination: ${ride.destName}"

        val time = SimpleDateFormat(
            "hh:mm a",
            Locale.getDefault()
        ).format(Date(ride.createdAt))

        holder.tvTime.text = "Requested at $time"

        db.collection("users")
            .document(ride.userId)
            .get()
            .addOnSuccessListener {

                val name = it.getString("name") ?: "Passenger"
                val phone = it.getString("phone") ?: ""

                holder.tvUserName.text = "Passenger: $name"
                holder.tvUserPhone.text = "Phone: $phone"

                holder.btnCall.setOnClickListener {

                    val intent = Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse("tel:$phone")
                    )

                    holder.itemView.context.startActivity(intent)
                }
            }

        when (ride.status) {

            "REQUESTED" -> {

                holder.btnReject.visibility = View.VISIBLE
                holder.btnAccept.isEnabled = true
                holder.btnAccept.text = "Accept"

                holder.btnAccept.setOnClickListener {
                    onAccept(ride)
                }

                holder.btnReject.setOnClickListener {
                    onReject(ride)
                }
            }

            "ACCEPTED" -> {

                holder.btnReject.visibility = View.GONE
                holder.btnAccept.text = "Start Trip"

                holder.btnAccept.setOnClickListener {
                    onStartTrip(ride)
                }
            }

            "ARRIVED", "ON_TRIP" -> {

                holder.btnReject.visibility = View.GONE
                holder.btnAccept.text = "Resume Trip"

                holder.btnAccept.setOnClickListener {
                    onStartTrip(ride)
                }
            }

            "COMPLETED" -> {
holder.btnAccept.visibility = View.VISIBLE
                holder.btnReject.visibility = View.GONE


                val paymentConfirmed = ride.paymentConfirmed ?: "pending"

                if (paymentConfirmed == "pending") {

                    holder.btnAccept.text = "Confirm Payment"

                    holder.btnAccept.setOnClickListener {

                        val context = holder.itemView.context

                        val intent = Intent(
                            context,
                            DriverPaymentActivity::class.java
                        )

                        intent.putExtra("rideId", ride.id)

                        context.startActivity(intent)
                    }

                } else {

                    holder.btnAccept.visibility = View.GONE
                }
            }
        }
    }

    override fun getItemCount(): Int = rides.size
}