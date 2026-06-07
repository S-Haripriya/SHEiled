package com.example.sheiled.ui.dashboard


import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.sheiled.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RatingActivity : AppCompatActivity() {

    private lateinit var rideId: String
    private lateinit var driverId: String

    private lateinit var ratingBar: RatingBar
    private lateinit var etFeedback: EditText
    private lateinit var btnSubmit: Button

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_rating)

        rideId = intent.getStringExtra("rideId") ?: return
        driverId = intent.getStringExtra("driverId") ?: return

        ratingBar = findViewById(R.id.ratingBar)
        etFeedback = findViewById(R.id.etFeedback)
        btnSubmit = findViewById(R.id.btnSubmitRating)

        btnSubmit.setOnClickListener {

            val rating = ratingBar.rating
            val feedback = etFeedback.text.toString().trim()

            if (rating == 0f) {

                Toast.makeText(
                    this,
                    "Please give a rating",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            submitRating(rating, feedback)
        }
    }

    private fun submitRating(rating: Float, feedback: String) {

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val ratingData = hashMapOf(

            "rideId" to rideId,
            "userId" to userId,
            "driverId" to driverId,
            "rating" to rating,
            "feedback" to feedback,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("ratings")
            .add(ratingData)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Thank you for your feedback!",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Failed to submit rating",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}