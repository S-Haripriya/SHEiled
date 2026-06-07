package com.example.sheiled.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.sheiled.R
import com.example.sheiled.ui.dashboard.DashBoardActivity
import com.example.sheiled.ui.dashboard.DriverDashboardActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val email = findViewById<EditText>(R.id.etEmail)
        val password = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {

            if (email.text.isNullOrBlank() || password.text.isNullOrBlank()) {
                toast("Enter email and password")
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(
                email.text.toString().trim(),
                password.text.toString().trim()
            ).addOnSuccessListener {

                val uid = auth.currentUser!!.uid
                verifyRoleAndLogin(uid)

            }.addOnFailureListener {
                toast("Login failed: ${it.message}")
            }
        }

        findViewById<TextView>(R.id.tvForgotPassword)
            .setOnClickListener {
                startActivity(Intent(this, ForgotPasswordActivity::class.java))
            }
    }

    // 🔎 ROLE VERIFICATION (ONLINE ONLY)
    private fun verifyRoleAndLogin(uid: String) {

        // 1️⃣ Check Driver First
        db.collection("drivers").document(uid).get()
            .addOnSuccessListener { driverDoc ->

                if (driverDoc.exists()) {

                    val status = driverDoc.getString("status")

                    when (status) {

                        "verified" -> {
                            saveSession("driver")
                            startActivity(Intent(this, DriverDashboardActivity::class.java))
                            finish()
                        }

                        "pending_verification" -> {
                            auth.signOut()
                            toast("Driver verification pending")
                        }

                        "blocked" -> {
                            auth.signOut()
                            toast("Driver account blocked by admin")
                        }

                        else -> {
                            auth.signOut()
                            toast("Driver not approved")
                        }
                    }

                } else {

                    // 2️⃣ Check Users Collection
                    // 2️⃣ Check Users Collection
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { userDoc ->

                            if (userDoc.exists()) {

                                val status = userDoc.getString("status")

                                if (status == "allowed") {

                                    saveSession("user")
                                    startActivity(Intent(this, DashBoardActivity::class.java))
                                    finish()

                                } else {

                                    auth.signOut()
                                    toast("Your account has been blocked by admin")

                                }

                            } else {
                                auth.signOut()
                                toast("Account not properly registered")
                            }
                        }
                        .addOnFailureListener {
                            auth.signOut()
                            toast("Unable to verify account")
                        }
                    }
            }
            .addOnFailureListener {
                auth.signOut()
                toast("Unable to verify account")
            }
    }

    // 💾 SAVE ROLE LOCALLY
    private fun saveSession(role: String) {
        getSharedPreferences("SHEILD_PREFS", MODE_PRIVATE)
            .edit()
            .putBoolean("IS_LOGGED_IN", true)
            .putString("ROLE", role)
            .apply()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}