package com.example.sheiled.ui.sos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class SOSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sosIntent = Intent(context, SOSTileService::class.java)
        sosIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startService(sosIntent)
    }
}
