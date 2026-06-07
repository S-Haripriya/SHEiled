package com.example.sheiled.ui.menubook.periodtracker.utils

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object PeriodNotificationHelper {

    private const val CHANNEL = "period_channel"

    /**
     * Create notification channel (Android 8+)
     */
    private fun createChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

            val channel = NotificationChannel(
                CHANNEL,
                "Period Tracker",
                NotificationManager.IMPORTANCE_HIGH
            )

            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Schedule reminder 2 days before predicted period
     */
    @SuppressLint("ScheduleExactAlarm")
    fun schedulePredictionReminder(context: Context, time: Long) {

        val intent = Intent(context, PeriodReceiver::class.java)

        val pending = PendingIntent.getBroadcast(
            context,
            101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarm =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarm.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            pending
        )
    }

    /**
     * Custom reminder set by user
     */
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleCustomReminder(context: Context, time: Long, note: String) {

        val intent = Intent(context, ReminderReceiver::class.java)
        intent.putExtra("note", note)

        val pending = PendingIntent.getBroadcast(
            context,
            102,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarm =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarm.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            pending
        )
    }

    /**
     * Schedule confirmation notification on predicted start day
     */
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleConfirmationNotification(context: Context, time: Long) {

        val intent = Intent(context, PeriodConfirmationReceiver::class.java)

        val pending = PendingIntent.getBroadcast(
            context,
            103,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarm =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarm.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            time,
            pending
        )
    }

    /**
     * Normal reminder notification
     */
    fun showNotification(context: Context, title: String, message: String) {

        createChannel(context)

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(2000, notification)
    }

    /**
     * Confirmation notification with YES / NO buttons
     */
    fun showConfirmationNotification(context: Context) {

        createChannel(context)

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        val yesIntent = Intent(context, PeriodConfirmationReceiver::class.java)
        yesIntent.putExtra("action", "YES")

        val noIntent = Intent(context, PeriodConfirmationReceiver::class.java)
        noIntent.putExtra("action", "NO")

        val yesPendingIntent = PendingIntent.getBroadcast(
            context,
            201,
            yesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val noPendingIntent = PendingIntent.getBroadcast(
            context,
            202,
            noIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Period Check")
            .setContentText("Did you get your period today?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "YES", yesPendingIntent)
            .addAction(0, "NO", noPendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(2001, notification)
    }
}