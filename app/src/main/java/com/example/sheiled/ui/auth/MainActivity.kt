package com.example.sheiled.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.sheiled.R
import com.example.sheiled.ui.dashboard.DashBoardActivity
import com.example.sheiled.ui.dashboard.DriverDashboardActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        val prefs = getSharedPreferences("SHEILD_PREFS", MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("IS_LOGGED_IN", false)
        val role = prefs.getString("ROLE", null)

        val firebaseUser = auth.currentUser
        Handler(Looper.getMainLooper()).postDelayed({
            if (firebaseUser != null && isLoggedIn && role != null) {

                if (role == "driver") {
                    startActivity(Intent(this, DriverDashboardActivity::class.java))
                } else {
                    startActivity(Intent(this, DashBoardActivity::class.java))
                }

            } else {
                startActivity(Intent(this, AuthChoiceActivity::class.java))
            }

            finish()
        }, 2000)
    }
}