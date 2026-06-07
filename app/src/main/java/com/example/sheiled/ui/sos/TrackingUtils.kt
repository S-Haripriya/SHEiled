package com.example.sheiled.ui.sos

import com.google.firebase.auth.FirebaseAuth

object TrackingUtils {

    fun getTrackingLink(): String {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        return "https://sheiled.web.app/Ride-Tracking-web/live.html?uid=$uid"
    }
}