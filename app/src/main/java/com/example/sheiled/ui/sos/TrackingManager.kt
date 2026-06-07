package com.example.sheiled.ui.sos

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object TrackingManager {

    fun startTracking(context: Context) {

        val intent = Intent(context, LiveTrackingService::class.java)

        ContextCompat.startForegroundService(context, intent)
    }

    fun stopTracking(context: Context) {

        val intent = Intent(context, LiveTrackingService::class.java)

        // Stop foreground tracking service
        context.stopService(intent)

        // Update Firestore status
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .update("live_location.status", "stopped")
    }
}