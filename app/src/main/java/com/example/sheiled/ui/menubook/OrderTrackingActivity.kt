package com.example.sheiled.ui.menubook


import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.sheiled.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderTrackingActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_order_tracking)

        container = findViewById(R.id.timelineContainer)

        val orderId = intent.getStringExtra("orderId") ?: return

        loadTracking(orderId)
    }

    private fun loadTracking(orderId: String) {

        db.collection("orders")
            .document(orderId)
            .addSnapshotListener { snap, _ ->

                if (snap == null || !snap.exists()) return@addSnapshotListener

                val history =
                    snap.get("trackingHistory") as? List<Map<String, Any>>

                container.removeAllViews()

                history
                    ?.sortedBy { it["time"] as Long }
                    ?.forEach {

                        val view = layoutInflater.inflate(
                            R.layout.item_tracking,
                            container,
                            false
                        )

                        val status =
                            it["status"].toString()

                        val message =
                            it["message"].toString()
                        val timeMillis =
                            (it["time"] as? Number)?.toLong() ?: 0L
                        val formattedTime = formatTime(timeMillis)

                        view.findViewById<TextView>(R.id.trackStatus)
                            .text = status

                        view.findViewById<TextView>(R.id.trackMessage)
                            .text = message
                        view.findViewById<TextView>(R.id.trackTime)
                            .text = formattedTime
                        container.addView(view)
                    }
            }
    }
    private fun formatTime(time: Long): String {

        if (time == 0L) return ""

        val sdf = SimpleDateFormat(
            "dd MMM yyyy, hh:mm a",
            Locale.getDefault()
        )

        return sdf.format(Date(time))
    }
}
