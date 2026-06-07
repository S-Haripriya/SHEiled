package com.example.sheiled.ui.menubook


import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.example.sheiled.databinding.ItemOrderBinding

class OrdersAdapter(
    private val orders: List<Map<String, Any>>
) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(val binding: ItemOrderBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {

        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {

        val order = orders[position]

        val orderId = order["orderId"]?.toString() ?: return
        val status = order["status"]?.toString() ?: "PENDING"
        val paymentStatus = order["paymentStatus"]?.toString() ?: "NOT_PAID"

        val itemText = StringBuilder()

        val items = order["items"] as? List<Map<String, Any>>

        if (!items.isNullOrEmpty()) {

            var total = 0.0

            for (item in items) {

                val name = item["name"]?.toString() ?: ""
                val quantity = (item["quantity"] as? Number)?.toInt() ?: 0
                val price = (item["price"] as? Number)?.toDouble() ?: 0.0

                total += price * quantity
                itemText.append("$name (x$quantity)  ₹$price\n")
            }

            holder.binding.tvTotal.text = "Total: ₹$total"

        } else {

            val name = order["name"]?.toString() ?: ""
            val quantity = (order["quantity"] as? Number)?.toInt() ?: 1
            val price = (order["price"] as? Number)?.toDouble() ?: 0.0

            itemText.append("$name (x$quantity)  ₹$price")
            holder.binding.tvTotal.text = "Total: ₹${price * quantity}"
        }

        holder.binding.tvItems.text = itemText.toString()
        holder.binding.tvStatus.text = "Status: $status"

        val statusColor = when (status.uppercase()) {
            "ACCEPTED" -> "#4CAF50"
            "REJECTED" -> "#F44336"
            "OUT_FOR_DELIVERY" -> "#2196F3"
            "DELIVERED" -> "#4CAF50"
            else -> "#FF9800"
        }

        holder.binding.tvStatus.setTextColor(Color.parseColor(statusColor))

        if (status == "ACCEPTED" && paymentStatus == "NOT_PAID") {
            holder.binding.btnPay.visibility = View.VISIBLE
        } else {
            holder.binding.btnPay.visibility = View.GONE
        }

        holder.binding.btnPay.setOnClickListener {

            FirebaseFirestore.getInstance()
                .collection("orders")
                .document(orderId)
                .update(
                    mapOf(
                        "paymentStatus" to "PAID",
                        "status" to "READY_FOR_DELIVERY"
                    )
                )
        }
        holder.itemView.setOnClickListener {

            val intent = Intent(
                holder.itemView.context,
                OrderTrackingActivity::class.java
            )

            intent.putExtra("orderId", orderId)

            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = orders.size
}