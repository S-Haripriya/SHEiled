package com.example.sheiled.ui.auth

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.sheiled.R
import com.example.sheiled.ui.dashboard.DashBoardActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()

        // User fields
        val name = findViewById<EditText>(R.id.regName)
        val phone = findViewById<EditText>(R.id.regPhone)
        val email = findViewById<EditText>(R.id.regEmail)
        val password = findViewById<EditText>(R.id.regPassword)

        // Emergency contacts (Name + Phone)
        val eName1 = findViewById<EditText>(R.id.regEmergencyName1)
        val ePhone1 = findViewById<EditText>(R.id.regEmergency1)

        val eName2 = findViewById<EditText>(R.id.regEmergencyName2)
        val ePhone2 = findViewById<EditText>(R.id.regEmergency2)

        findViewById<Button>(R.id.btnCreateAccount).setOnClickListener {

            // ✅ Basic validation
            if (email.text.isNullOrBlank() || password.text.length < 6) {
                Toast.makeText(
                    this,
                    "Enter valid email and password (min 6 chars)",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(
                email.text.toString(),
                password.text.toString()
            ).addOnSuccessListener {

                val uid = auth.currentUser!!.uid

                // ✅ Build emergency contacts list
                val emergencyContacts = mutableListOf<Map<String, String>>()

                if (eName1.text.isNotBlank() && ePhone1.text.isNotBlank()) {
                    emergencyContacts.add(
                        mapOf(
                            "id" to "c1",
                            "name" to eName1.text.toString(),
                            "phone" to ePhone1.text.toString()
                        )
                    )
                }

                if (eName2.text.isNotBlank() && ePhone2.text.isNotBlank()) {
                    emergencyContacts.add(
                        mapOf(
                            "id" to "c2",
                            "name" to eName2.text.toString(),
                            "phone" to ePhone2.text.toString()
                        )
                    )
                }

                // ✅ Final user map
                val userMap = hashMapOf(
                    "name" to name.text.toString(),
                    "phone" to phone.text.toString(),
                    "email" to email.text.toString(),
                    "status" to "allowed",
                    "emergency_contacts" to emergencyContacts,
                    "alert_messages" to emptyList<Any>()   // important for dashboard
                )

                db.collection("users").document(uid).set(userMap)

                auth.currentUser!!.sendEmailVerification()
                saveLoginStatus()

                // ✅ Ask permissions after registration
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.CALL_PHONE
                    )
                )

            }.addOnFailureListener {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Permission launcher
    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            startActivity(Intent(this, DashBoardActivity::class.java))
            finish()
        }

    private fun saveLoginStatus() {
        getSharedPreferences("SHEILD_PREFS", MODE_PRIVATE)
            .edit()
            .putBoolean("IS_LOGGED_IN", true)
            .apply()
    }
}
