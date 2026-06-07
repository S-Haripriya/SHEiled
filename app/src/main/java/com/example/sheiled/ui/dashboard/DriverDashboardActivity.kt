package com.example.sheiled.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.sheiled.R
import com.example.sheiled.ui.auth.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class DriverDashboardActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var profileListener: ListenerRegistration? = null
    private var isProfileLocked = false

    /* ================= REAL-TIME PROFILE CHECK ================= */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_dashboard)
        checkDriverStatusRealtime()
        bottomNav = findViewById(R.id.bottomNav)

        observeDriverProfile()

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_requests -> {
                    loadFragment(RequestFragment())
                    true
                }

                R.id.nav_history -> {
                    loadFragment(HistoryFragment())
                    true
                }

                R.id.nav_profile -> {
                    loadFragment(DriverProfileFragment())
                    true
                }

                else -> false
            }
        }

        /* ---------- LOAD DEFAULT FRAGMENT ---------- */

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_requests
        }
    }
    private fun checkDriverStatusRealtime() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("drivers")
            .document(uid)
            .addSnapshotListener { doc, _ ->
                if (FirebaseAuth.getInstance().currentUser == null) return@addSnapshotListener
                val status = doc?.getString("status")

                if (status != "verified") {

                    Toast.makeText(
                        this,
                        "Driver account blocked by admin",
                        Toast.LENGTH_LONG
                    ).show()

                    FirebaseAuth.getInstance().signOut()

                    startActivity(Intent(this, LoginActivity::class.java))

                    finishAffinity()
                }
            }
    }
    private fun observeDriverProfile() {
        val driverId = auth.currentUser?.uid ?: return

        profileListener = db.collection("drivers")
            .document(driverId)
            .addSnapshotListener { doc, _ ->

                if (doc == null || !doc.exists()) return@addSnapshotListener

                val baseFare = doc.getDouble("baseFare") ?: 0.0
                val area = doc.getString("area") ?: ""
                val profileCompleted =
                    doc.getBoolean("profileCompleted") ?: false

                val completed =
                    profileCompleted && baseFare > 0 && area.isNotBlank()

                if (!completed) {

                    if (!isProfileLocked) {
                        isProfileLocked = true
                        disableBottomNav()
                        loadFragment(DriverProfileFragment())
                        showMandatoryProfileDialog()
                    }

                } else {
                    if (isProfileLocked) {
                        isProfileLocked = false
                        enableBottomNav()
                        loadFragment(RequestFragment())
                    }
                }
            }
    }

    /* ================= UI HELPERS ================= */

    private fun showMandatoryProfileDialog() {
        AlertDialog.Builder(this)
            .setTitle("Profile Setup Required")
            .setMessage(
                "Please set your Base Fare and Area of Service.\n\n" +
                        "These fields are mandatory to accept ride requests."
            )
            .setCancelable(false)
            .setPositiveButton("Set Now", null)
            .show()
    }

    private fun disableBottomNav() {
        bottomNav.menu.findItem(R.id.nav_requests).isEnabled = false
        bottomNav.menu.findItem(R.id.nav_history).isEnabled = false
        bottomNav.menu.findItem(R.id.nav_profile).isEnabled = true
    }

    private fun enableBottomNav() {
        bottomNav.menu.findItem(R.id.nav_requests).isEnabled = true
        bottomNav.menu.findItem(R.id.nav_history).isEnabled = true
        bottomNav.menu.findItem(R.id.nav_profile).isEnabled = true
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        profileListener?.remove()
    }
}
