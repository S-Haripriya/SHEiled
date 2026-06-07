package com.example.sheiled.ui.sos



import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class ReactivateService : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReactivateService", "Receiver Started")

        val serviceIntent = Intent(context, SensorService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
