package com.example.sheiled.ui.menubook.periodtracker.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PeriodReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        PeriodNotificationHelper.showNotification(
            context,
            "Period Reminder",
            "Your period may start in 2 days"
        )
    }
}