package com.example.sheiled.ui.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.sheiled.R
import com.example.sheiled.ui.auth.AuthChoiceActivity
import com.example.sheiled.ui.auth.LoginActivity
import com.example.sheiled.ui.sos.ReactivateService
import com.example.sheiled.ui.sos.SensorService
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashBoardActivity : AppCompatActivity() {

    private lateinit var container: FrameLayout
    private lateinit var title: TextView

    private lateinit var btnHome: MaterialButton
    private lateinit var btnLocation: MaterialButton
    private lateinit var btnMenuBook: MaterialButton
    private lateinit var btnProfile: MaterialButton

    private val IGNORE_BATTERY_OPTIMIZATION_REQUEST = 1002

    // Permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { /* No UI callback required */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        checkUserStatusRealtime()
        checkAndRequestPermissions()
        requestBatteryOptimizationExemption()
        startSensorServiceIfNeeded()

        // Views
        container = findViewById(R.id.mapContainer)
        title = findViewById(R.id.tvHome)

        btnHome = findViewById(R.id.btnHome)
        btnLocation = findViewById(R.id.btnLocation)
        btnMenuBook = findViewById(R.id.btnMenuBook)
        btnProfile = findViewById(R.id.btnProfile)

        // Default fragment
        loadFragment(HomeFragment(), "Home")

        btnHome.setOnClickListener { loadFragment(HomeFragment(), "Home") }
        btnLocation.setOnClickListener { loadFragment(MapFragment(), "Map") }
        btnMenuBook.setOnClickListener { loadFragment(MenuBookFragment(), "Menu Book") }
        btnProfile.setOnClickListener { loadFragment(ProfileFragment(), "Profile") }
    }

    // ================= FRAGMENT HANDLING =================
    private fun checkUserStatusRealtime() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .addSnapshotListener { doc, _ ->
                if (FirebaseAuth.getInstance().currentUser == null) return@addSnapshotListener
                val status = doc?.getString("status")

                if (status != "allowed") {

                    Toast.makeText(
                        this,
                        "Your account has been blocked by admin",
                        Toast.LENGTH_LONG
                    ).show()

                    FirebaseAuth.getInstance().signOut()

                    startActivity(Intent(this, AuthChoiceActivity::class.java))

                    finishAffinity()
                }
            }
    }
    private fun loadFragment(fragment: androidx.fragment.app.Fragment, titleText: String) {
        title.text = titleText
        supportFragmentManager.beginTransaction()
            .replace(R.id.mapContainer, fragment)
            .commit()
    }

    // ================= PERMISSIONS =================

    private fun checkAndRequestPermissions() {

        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val toRequest = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }


        if (toRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(toRequest.toTypedArray())
        }
    }

    // ================= BATTERY OPTIMIZATION =================

    private fun requestBatteryOptimizationExemption() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivityForResult(intent, IGNORE_BATTERY_OPTIMIZATION_REQUEST)
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Please disable battery optimization manually",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // ================= SENSOR SERVICE =================

    private fun startSensorServiceIfNeeded() {

        if (!isMyServiceRunning(SensorService::class.java)) {

            val intent = Intent(this, SensorService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    // ================= AUTO-RESTART =================

    override fun onDestroy() {
        super.onDestroy()
        sendBroadcast(
            Intent(this, ReactivateService::class.java)
                .setAction("restartservice")
        )
    }
}
