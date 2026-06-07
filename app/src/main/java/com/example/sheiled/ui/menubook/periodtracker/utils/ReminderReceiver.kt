package com.example.sheiled.ui.menubook.periodtracker.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        val note = intent?.getStringExtra("note") ?: "Period Reminder"

        PeriodNotificationHelper.showNotification(
            context,
            "Reminder",
            note
        )
    }
}