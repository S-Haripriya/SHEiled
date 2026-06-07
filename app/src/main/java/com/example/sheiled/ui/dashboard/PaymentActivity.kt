package com.example.sheiled.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.sheiled.R
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PaymentActivity : AppCompatActivity() {

    private lateinit var rideId: String
    private var amount = 0.0
    private var cancelRide = false

    private lateinit var radioGroup: RadioGroup
    private lateinit var tvAmount: TextView
    private lateinit var btnConfirm: Button

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_payment)

        rideId = intent.getStringExtra("rideId") ?: return
        amount = intent.getDoubleExtra("amount", 0.0)
        cancelRide = intent.getBooleanExtra("cancelRide", false)

        tvAmount = findViewById(R.id.tvAmount)
        btnConfirm = findViewById(R.id.btnConfirmPayment)
        radioGroup = findViewById(R.id.paymentOptions)

        tvAmount.text = "Amount to Pay: ₹${String.format("%.2f", amount)}"

        btnConfirm.setOnClickListener {

            if (radioGroup.checkedRadioButtonId == -1) {

                Toast.makeText(
                    this,
                    "Select payment method",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            processPayment()
        }
    }

    /* ================= PROCESS PAYMENT ================= */

    private fun processPayment() {

        Toast.makeText(
            this,
            "Processing payment...",
            Toast.LENGTH_SHORT
        ).show()

        Handler(Looper.getMainLooper()).postDelayed({

            completePayment()

        }, 2000)
    }

    /* ================= COMPLETE PAYMENT ================= */

    private fun completePayment() {

        val selectedMethod = findViewById<RadioButton>(
            radioGroup.checkedRadioButtonId
        ).text.toString()

        val rideRef = db.collection("rides").document(rideId)

        rideRef.get().addOnSuccessListener { doc ->

            val driverId = doc.getString("driverId") ?: return@addOnSuccessListener

            val driverRef = db.collection("drivers").document(driverId)

            db.runTransaction { transaction ->

                if (cancelRide) {

                    transaction.update(
                        rideRef,
                        mapOf(
                            "status" to "CANCELLED_WITH_FEE",
                            "cancellationFee" to amount,
                            "paymentStatus" to "paid",
                            "paymentMethod" to selectedMethod,
                            "paidAt" to System.currentTimeMillis()
                        )
                    )

                } else {

                    transaction.update(
                        rideRef,
                        mapOf(
                            "paymentStatus" to "paid",
                            "paymentMethod" to selectedMethod,
                            "paidAt" to System.currentTimeMillis()
                        )
                    )
                }

                /* Update driver earnings */
                transaction.update(
                    driverRef,
                    "earnings",
                    FieldValue.increment(amount)
                )

                /* Make driver available again */
                transaction.update(
                    driverRef,
                    "availability",
                    "available"
                )

                null
            }
                .addOnSuccessListener {

                    Toast.makeText(
                        this,
                        "Payment Successful",
                        Toast.LENGTH_LONG
                    ).show()

                    if (!cancelRide) {

                        val intent = Intent(this, RatingActivity::class.java)

                        intent.putExtra("rideId", rideId)
                        intent.putExtra("driverId", driverId)

                        startActivity(intent)
                    }

                    finish()
                }

                .addOnFailureListener {

                    Toast.makeText(
                        this,
                        "Payment Failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}