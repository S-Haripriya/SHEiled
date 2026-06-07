package com.example.sheiled.ui.menubook.periodtracker.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sheiled.ui.menubook.periodtracker.model.PeriodRecord
import java.util.*

class PeriodConfirmationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        val action = intent?.getStringExtra("action")

        // If alarm triggered → show confirmation notification
        if (action == null) {
            PeriodNotificationHelper.showConfirmationNotification(context)
            return
        }

        val repository = PeriodRepository()

        repository.getPeriod { record ->

            if (record == null) return@getPeriod

            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val today = todayCal.timeInMillis

            when (action) {

                // ======================
                // USER PRESSED YES
                // ======================

                "YES" -> {

                    if (!record.isPeriodOngoing) {

                        record.isPeriodOngoing = true
                        record.actualStart = today
                    }

                    if (!record.confirmedDays.contains(today)) {
                        record.confirmedDays.add(today)
                    }

                    repository.savePeriod(record)

                    // Ask again tomorrow
                    val tomorrow = Calendar.getInstance().apply {
                        timeInMillis = today
                        add(Calendar.DAY_OF_MONTH, 1)
                    }.timeInMillis

                    PeriodNotificationHelper
                        .scheduleConfirmationNotification(context, tomorrow)
                }

                // ======================
                // USER PRESSED NO
                // ======================

                "NO" -> {

                    // Period hasn't started yet
                    if (!record.isPeriodOngoing) {

                        if (!record.missedDays.contains(today)) {
                            record.missedDays.add(today)
                        }

                        repository.savePeriod(record)

                        val tomorrow = Calendar.getInstance().apply {
                            timeInMillis = today
                            add(Calendar.DAY_OF_MONTH, 1)
                        }.timeInMillis

                        PeriodNotificationHelper
                            .scheduleConfirmationNotification(context, tomorrow)

                        return@getPeriod
                    }

                    // ============================
                    // PERIOD FINISHED
                    // ============================

                    val actualStart = record.actualStart
                    val actualEnd = record.confirmedDays.lastOrNull() ?: today

                    val standardPeriodLength = record.periodLength
                    val standardCycleLength = record.cycleLength
                    val actualPeriodLength =
                        ((actualEnd - actualStart) / (1000 * 60 * 60 * 24)).toInt() + 1
                    repository.saveCompletedCycle(
                        actualStart,
                        actualEnd,
                        record.previousStartDate,
                        standardCycleLength,
                        standardPeriodLength,
                        actualPeriodLength
                    )

                    // ============================
                    // PREDICT NEXT CYCLE
                    // ============================

                    val nextStartCal = Calendar.getInstance()
                    nextStartCal.timeInMillis = actualStart
                    nextStartCal.add(Calendar.DAY_OF_MONTH, standardCycleLength)

                    val nextStart = nextStartCal.timeInMillis

                    val nextEndCal = Calendar.getInstance()
                    nextEndCal.timeInMillis = nextStart
                    nextEndCal.add(Calendar.DAY_OF_MONTH, standardPeriodLength - 1)

                    val nextEnd = nextEndCal.timeInMillis

                    record.previousStartDate = record.startDate
                    record.startDate = actualStart
                    record.endDate = actualEnd

                    record.predictedStart = nextStart
                    record.predictedEnd = nextEnd

                    record.isPeriodOngoing = false
                    record.actualStart = 0L

                    record.confirmedDays.clear()
                    record.missedDays.clear()

                    repository.savePeriod(record)

                    // schedule next prediction
                    val confirmCal = Calendar.getInstance()
                    confirmCal.timeInMillis = nextStart
                    confirmCal.set(Calendar.HOUR_OF_DAY, 20)
                    confirmCal.set(Calendar.MINUTE, 0)
                    confirmCal.set(Calendar.SECOND, 0)
                    confirmCal.set(Calendar.MILLISECOND, 0)

                    PeriodNotificationHelper.scheduleConfirmationNotification(
                        context,
                        confirmCal.timeInMillis
                    )
                }
            }

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

            manager.cancel(2001)
        }
    }
}