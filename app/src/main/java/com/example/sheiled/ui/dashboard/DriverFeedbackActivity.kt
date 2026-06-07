package com.example.sheiled.ui.dashboard

import android.os.Bundle
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.sheiled.R
import com.google.firebase.firestore.FirebaseFirestore

class DriverFeedbackActivity : AppCompatActivity() {

    private lateinit var rideId: String
    private lateinit var etFeedback: EditText
    private lateinit var btnSubmit: Button

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_driver_feedback)

        rideId = intent.getStringExtra("rideId") ?: ""

        etFeedback = findViewById(R.id.etFeedback)
        btnSubmit = findViewById(R.id.btnSubmitFeedback)

        /* Prevent back button */
        onBackPressedDispatcher.addCallback(this) {
            Toast.makeText(
                this@DriverFeedbackActivity,
                "Please submit feedback to complete the trip",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnSubmit.setOnClickListener {

            val feedbackText = etFeedback.text.toString().trim()

            if (feedbackText.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please enter feedback",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            saveFeedback(feedbackText)
        }
    }

    private fun saveFeedback(feedback: String) {

        /* Fetch ride data to get userId and driverId */

        db.collection("rides")
            .document(rideId)
            .get()
            .addOnSuccessListener { rideDoc ->

                if (!rideDoc.exists()) {
                    Toast.makeText(this, "Ride not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val rideData = rideDoc.data!!

                val driverId = rideData["driverId"] as? String ?: ""
                val userId = rideData["userId"] as? String ?: ""

                val feedbackData = hashMapOf(
                    "rideId" to rideId,
                    "driverId" to driverId,
                    "userId" to userId,
                    "feedback" to feedback,
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("driver_feedback")
                    .add(feedbackData)
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Feedback submitted successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()
                    }
                    .addOnFailureListener {

                        Toast.makeText(
                            this,
                            "Failed to save feedback",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
    }
}