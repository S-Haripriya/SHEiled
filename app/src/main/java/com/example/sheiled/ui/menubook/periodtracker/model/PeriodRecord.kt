package com.example.sheiled.ui.menubook.periodtracker.model

data class PeriodRecord(



    var startDate: Long = 0L,     // predicted cycle base
    var endDate: Long = 0L,

    var cycleLength: Int = 28,    // user provided standard cycle
    var periodLength: Int = 5,    // user provided standard period length

    var predictedStart: Long = 0L,
    var predictedEnd: Long = 0L,
    var previousStartDate: Long = 0L,
    var actualStart: Long = 0L,

    var isPeriodOngoing: Boolean = false,

    var confirmedDays: MutableList<Long> = mutableListOf(),
    var missedDays: MutableList<Long> = mutableListOf(),
    var reminderNote: String = "",
    var reminderTime: Long = 0L,
    var ringtone: String = ""
)