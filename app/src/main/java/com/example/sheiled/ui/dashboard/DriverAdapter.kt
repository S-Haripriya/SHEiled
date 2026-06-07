package com.example.sheiled.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sheiled.R
import java.text.SimpleDateFormat
import java.util.*

class DriverAdapter(
    private val drivers: List<Driver>,
    private val onDriverClick: (Driver) -> Unit
) : RecyclerView.Adapter<DriverAdapter.DriverViewHolder>() {

    private var activeRide: Booking? = null

    fun setActiveRide(ride: Booking?) {
        activeRide = ride
    }

    inner class DriverViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvVehicle: TextView = itemView.findViewById(R.id.tvVehicle)
        val tvArea: TextView = itemView.findViewById(R.id.tvArea)
        val tvFare: TextView = itemView.findViewById(R.id.tvFare)

        val tvRideDetails: TextView =
            itemView.findViewById(R.id.tvRideDetails)

        val btnBook: Button = itemView.findViewById(R.id.btnBook)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DriverViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_driver, parent, false)

        return DriverViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: DriverViewHolder,
        position: Int
    ) {

        val driver = drivers[position]

        holder.tvName.text = driver.name
        holder.tvVehicle.text = "Vehicle: ${driver.vehicleNumber}"
        holder.tvArea.text = "Area: ${driver.area}"
        holder.tvFare.text = "Base Fare: ₹${driver.baseFare}"

        val ride = activeRide

        /* ================= SHOW RIDE DETAILS ================= */

        if (ride != null && ride.driverId == driver.id) {

            val time = SimpleDateFormat(
                "dd MMM, hh:mm a",
                Locale.getDefault()
            ).format(Date(ride.createdAt))

            holder.tvRideDetails.visibility = View.VISIBLE

            holder.tvRideDetails.text =
                """
Pickup: ${ride.pickupName}
Destination: ${ride.destName}
Time: $time
Status: ${ride.status}
""".trimIndent()

        } else {

            holder.tvRideDetails.visibility = View.GONE
        }

        /* ================= BUTTON STATE ================= */

        when {

            /* No ride active */
            ride == null -> {

                holder.btnBook.text = "BOOK"
                holder.btnBook.isEnabled = true
            }

            /* Ride requested */
            ride.driverId == driver.id &&
                    ride.status == "REQUESTED" -> {

                holder.btnBook.text = "CANCEL"
                holder.btnBook.isEnabled = true
            }

            /* Ride accepted */
            ride.driverId == driver.id &&
                    ride.status == "ACCEPTED" -> {

                holder.btnBook.text = "CANCEL"
                holder.btnBook.isEnabled = true
            }

            /* Ride ongoing */
            ride.driverId == driver.id &&
                    ride.status == "ON_TRIP" -> {

                holder.btnBook.text = "TRACK RIDE"
                holder.btnBook.isEnabled = true
            }

            /* Trip completed but payment pending */
            /* Trip completed but payment pending */
            ride.driverId == driver.id &&
                    ride.status == "COMPLETED" &&
                    (ride.paymentStatus ?: "pending") != "paid" -> {

                holder.btnBook.text = "PAY NOW"
                holder.btnBook.isEnabled = true
            }

            /* Trip completed and payment done */
            ride.driverId == driver.id &&
                    ride.status == "COMPLETED" &&
                    ride.paymentStatus == "paid" -> {

                holder.btnBook.text = "BOOK"
                holder.btnBook.isEnabled = true
            }
        }

        holder.btnBook.setOnClickListener {
            onDriverClick(driver)
        }
    }

    override fun getItemCount(): Int = drivers.size
}