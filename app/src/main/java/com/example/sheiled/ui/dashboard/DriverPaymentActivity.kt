package com.example.sheiled.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.sheiled.R
import com.google.firebase.firestore.FirebaseFirestore

class DriverPaymentActivity : AppCompatActivity() {

    private lateinit var rideId: String
    private val db = FirebaseFirestore.getInstance()

    private lateinit var tvFare: TextView
    private lateinit var btnConfirmPayment: Button

    private var paymentProcessing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_driver_payment)

        rideId = intent.getStringExtra("rideId") ?: run {

            Toast.makeText(
                this,
                "Invalid ride",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        tvFare = findViewById(R.id.tvFare)
        btnConfirmPayment = findViewById(R.id.btnConfirmPayment)

        loadFare()

        /* -------- Prevent leaving payment screen -------- */

        onBackPressedDispatcher.addCallback(this) {

            finish()
        }

        btnConfirmPayment.setOnClickListener {

            if (paymentProcessing) return@setOnClickListener

            processPayment()
        }
    }

    /* ================= LOAD FARE ================= */

    private fun loadFare() {

        db.collection("rides")
            .document(rideId)
            .get()
            .addOnSuccessListener { doc ->

                val fare = doc.getDouble("finalFare") ?: 0.0
                val paymentStatus = doc.getString("paymentStatus") ?: "pending"
                val paymentConfirmed = doc.getString("paymentConfirmed") ?: "pending"

                tvFare.text =
                    "Payable Amount: ₹${String.format("%.2f", fare)}"

                /* Notify driver if user already paid */

                if (paymentStatus == "paid" && paymentConfirmed == "pending") {

                    Toast.makeText(
                        this,
                        "User paid successfully",
                        Toast.LENGTH_LONG
                    ).show()
                }

                /* If payment already confirmed */
                if (paymentConfirmed == "confirmed") {

                    Toast.makeText(
                        this,
                        "Payment already confirmed",
                        Toast.LENGTH_LONG
                    ).show()

                    openFeedback()
                }
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Failed to load fare",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
    /* ================= PROCESS PAYMENT ================= */

    private fun processPayment() {

        paymentProcessing = true
        btnConfirmPayment.isEnabled = false

        db.collection("rides")
            .document(rideId)
            .update("paymentConfirmed", "confirmed")

            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Payment Successful",
                    Toast.LENGTH_LONG
                ).show()

                openFeedback()
            }

            .addOnFailureListener {

                paymentProcessing = false
                btnConfirmPayment.isEnabled = true

                Toast.makeText(
                    this,
                    "Payment failed. Try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /* ================= OPEN FEEDBACK ================= */

    private fun openFeedback() {

        val intent = Intent(
            this,
            DriverFeedbackActivity::class.java
        )

        intent.putExtra("rideId", rideId)

        startActivity(intent)

        finish()
    }
}