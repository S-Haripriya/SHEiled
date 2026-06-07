package com.example.sheiled.safety

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.sheiled.R
import android.Manifest
import androidx.annotation.RequiresApi

class SafeModeActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvScore: TextView
    private lateinit var btnToggle: Button

    private var isSafeModeOn = false

    // =============================
    // Broadcast Receiver
    // =============================
    private val riskReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val score = intent?.getIntExtra("risk_score", 0) ?: 0

            tvScore.text = "Current Safety Score: $score"

            when (score) {
                in 0..30 -> tvStatus.text = "🟢 SAFE AREA"
                in 31..60 -> tvStatus.text = "🟡 MODERATE RISK"
                in 61..80 -> tvStatus.text = "🟠 HIGH RISK"
                else -> tvStatus.text = "🔴 DANGER ZONE"
            }
        }
    }

    // =============================
    // Activity Lifecycle
    // =============================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_safe_mode)

        tvStatus = findViewById(R.id.tvStatus)
        tvScore = findViewById(R.id.tvScore)
        btnToggle = findViewById(R.id.btnToggle)

        // Check service state using static flag
        isSafeModeOn = SafetyForegroundService.isServiceRunning

        updateButtonUI()

        btnToggle.setOnClickListener {
            if (isSafeModeOn) {
                stopSafeMode()
            } else {
                startSafeMode()
            }
        }
    }

    // =============================
    // Start Safe Mode
    // =============================
    private fun startSafeMode() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            101
        )
        val intent = Intent(this, SafetyForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)

        isSafeModeOn = true
        updateButtonUI()
    }

    // =============================
    // Stop Safe Mode
    // =============================
    private fun stopSafeMode() {
        stopService(Intent(this, SafetyForegroundService::class.java))

        isSafeModeOn = false
        tvStatus.text = "Safe Mode OFF"
        tvScore.text = ""
        updateButtonUI()
    }

    // =============================
    // Update Button Text
    // =============================
    private fun updateButtonUI() {
        btnToggle.text =
            if (isSafeModeOn) "TURN OFF SAFE MODE"
            else "START SAFE MODE"
    }

    // =============================
    // Register Receiver Safely
    // =============================
    override fun onResume() {
        super.onResume()

        val filter =
            IntentFilter("com.example.sheiled.RISK_UPDATE")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                riskReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(riskReceiver, filter)
        }
    }
    override fun onPause() {
        super.onPause()
        unregisterReceiver(riskReceiver)
    }
}