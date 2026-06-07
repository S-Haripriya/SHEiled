package com.example.sheiled.ui.menubook.periodtracker.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.example.sheiled.R
import com.example.sheiled.ui.menubook.periodtracker.adapter.CalendarAdapter
import com.example.sheiled.ui.menubook.periodtracker.model.PeriodRecord
import com.example.sheiled.ui.menubook.periodtracker.utils.PeriodNotificationHelper
import com.example.sheiled.ui.menubook.periodtracker.utils.PeriodRepository
import java.text.SimpleDateFormat
import java.util.*
import android.app.TimePickerDialog
import android.media.RingtoneManager
import android.net.Uri
import android.content.Intent
import android.app.Activity

class CalendarFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var tvMonth: TextView
    private lateinit var btnEditDates: Button
    private val calendar = Calendar.getInstance()
    private val repository = PeriodRepository()
    private lateinit var btnReminder: Button
    private var record: PeriodRecord? = null
    private var pendingReminderTime: Long = 0
    private var pendingReminderNote: String = ""
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_calendar, container, false)
        btnReminder = view.findViewById(R.id.btnReminder)
        recycler = view.findViewById(R.id.recyclerCalendar)
        tvMonth = view.findViewById(R.id.tvMonth)
        btnEditDates = view.findViewById(R.id.btnEditDates)
        recycler.layoutManager = GridLayoutManager(requireContext(),7)
        recycler.setHasFixedSize(true)
        btnEditDates.setOnClickListener {
            openDateRangePicker()
        }
        btnReminder.setOnClickListener {

            val rec = record ?: return@setOnClickListener

            showReminderOptions(rec)
        }
        loadData()

        return view
    }

    private fun openDateRangePicker() {

        val picker = MaterialDatePicker.Builder
            .dateRangePicker()
            .setTitleText("Edit Period Dates")
            .build()

        picker.show(parentFragmentManager, "EDIT_DATES")

        picker.addOnPositiveButtonClickListener { selection ->

            val start = selection.first ?: return@addOnPositiveButtonClickListener
            val end = selection.second ?: return@addOnPositiveButtonClickListener

            // restart setup flow
            askCycleLength(start, end)
        }
    }
    private fun loadData(){

        repository.getPeriod { rec ->

            if(rec == null){

                record = PeriodRecord()
                drawCalendar()      // show empty calendar
                showFirstSetup()    // then ask user for dates

            }else{

                record = rec

                calendar.timeInMillis =
                    if (rec.isPeriodOngoing) rec.actualStart
                    else rec.predictedStart

                drawCalendar()
            }
        }
    }

    private fun showFirstSetup(){

        val picker = MaterialDatePicker.Builder
            .dateRangePicker()
            .setTitleText("Select Last Period")
            .build()

        picker.show(parentFragmentManager,"FIRST_SETUP")

        picker.addOnPositiveButtonClickListener { selection ->

            val start = selection.first ?: return@addOnPositiveButtonClickListener
            val end = selection.second ?: return@addOnPositiveButtonClickListener

            askCycleLength(start,end)
        }
    }

    private fun askCycleLength(start: Long, end: Long) {

        val input = EditText(requireContext())
        input.hint = "Cycle Length (ex 28)"

        AlertDialog.Builder(requireContext())
            .setTitle("Enter Cycle Length")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->

                val cycle = input.text.toString().toInt()

                val periodLength =
                    ((end - start) / (1000 * 60 * 60 * 24)).toInt() + 1

                val predictedStart =
                    start + cycle * 86400000L

                val predictedEnd =
                    predictedStart + (periodLength - 1) * 86400000L

                val rec = record ?: PeriodRecord()

                rec.startDate = start
                rec.endDate = end
                rec.cycleLength = cycle
                rec.periodLength = periodLength
                rec.predictedStart = predictedStart
                rec.predictedEnd = predictedEnd
                rec.confirmedDays.clear()
                rec.missedDays.clear()
                rec.isPeriodOngoing = false
                rec.actualStart = 0L
                repository.savePeriod(rec)
                calendar.timeInMillis = predictedStart
                scheduleNotifications(rec)
               loadData()
            }
            .show()
    }
    private fun showReminderOptions(record: PeriodRecord) {

        val options = if (record.reminderTime > 0)
            arrayOf("Set New Reminder", "Delete Reminder")
        else
            arrayOf("Set Reminder")

        AlertDialog.Builder(requireContext())
            .setTitle("Reminder Options")
            .setItems(options) { _, which ->

                when (options[which]) {

                    "Set Reminder" -> openReminderPicker(record)

                    "Set New Reminder" -> openReminderPicker(record)

                    "Delete Reminder" -> {

                        record.reminderTime = 0
                        record.reminderNote = ""

                        repository.savePeriod(record)

                        AlertDialog.Builder(requireContext())
                            .setMessage("Reminder deleted")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
            .show()
    }
    private fun openReminderPicker(record: PeriodRecord) {

        val view = layoutInflater.inflate(R.layout.reminder_dialog, null)
        val note = view.findViewById<EditText>(R.id.etNote)

        AlertDialog.Builder(requireContext())
            .setTitle("Set Reminder")
            .setView(view)
            .setPositiveButton("Next") { _, _ ->

                val datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select reminder date")
                    .build()

                datePicker.show(parentFragmentManager, "REMINDER_DATE")

                datePicker.addOnPositiveButtonClickListener { date ->

                    val cal = Calendar.getInstance()
                    cal.timeInMillis = date

                    // =====================
                    TimePickerDialog(
                        requireContext(),
                        { _, hour, minute ->

                            cal.set(Calendar.HOUR_OF_DAY, hour)
                            cal.set(Calendar.MINUTE, minute)
                            cal.set(Calendar.SECOND, 0)

                            val reminderTime = cal.timeInMillis

                            pendingReminderTime = reminderTime
                            pendingReminderNote = note.text.toString()

                            val rec = record ?: return@TimePickerDialog

                            rec.reminderNote = pendingReminderNote
                            rec.reminderTime = pendingReminderTime

                            repository.savePeriod(rec)

                            PeriodNotificationHelper.scheduleCustomReminder(
                                requireContext(),
                                pendingReminderTime,
                                pendingReminderNote
                            )

                            AlertDialog.Builder(requireContext())
                                .setMessage("Reminder set successfully")
                                .setPositiveButton("OK", null)
                                .show()

                        },
                        8,
                        0,
                        true
                    ).show()

                }

            }
            .setNegativeButton("Cancel", null)
            .show()
    }
     private fun drawCalendar(){

        val rec = record ?: return

        val monthFormat = SimpleDateFormat("MMMM yyyy",Locale.getDefault())
        tvMonth.text = monthFormat.format(calendar.time)

        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        val first = Calendar.getInstance()
        first.set(year,month,1)

        val offset = first.get(Calendar.DAY_OF_WEEK)-1
        val max = first.getActualMaximum(Calendar.DAY_OF_MONTH)

        val days = mutableListOf<Int?>()

        repeat(offset){ days.add(null) }

        for(i in 1..max){
            days.add(i)
        }

        recycler.adapter = CalendarAdapter(
            days,
            getPredicted(rec,month,year),
            getActual(rec,month,year),
            getMissed(rec,month,year)
        )


    }

    private fun scheduleNotifications(rec: PeriodRecord) {

        val now = System.currentTimeMillis()

        // =========================
        // Reminder 2 days before
        // =========================

        val reminderTwoDays = rec.predictedStart - (2 * 86400000L)

        if (reminderTwoDays > now) {
            PeriodNotificationHelper.schedulePredictionReminder(
                requireContext(),
                reminderTwoDays
            )
        }

        // =========================
        // Confirmation notification
        // =========================

        val cal = Calendar.getInstance()
        cal.timeInMillis = rec.predictedStart

        // Ask confirmation at 8 PM on predicted day
        cal.set(Calendar.HOUR_OF_DAY, 20)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val confirmationTime = cal.timeInMillis

        // If the predicted time already passed, ask immediately (after 5 seconds)
        val finalTime =
            if (confirmationTime < now)
                System.currentTimeMillis() + 5000
            else
                confirmationTime

        PeriodNotificationHelper.scheduleConfirmationNotification(
            requireContext(),
            finalTime
        )
    }
    private fun getPredicted(rec:PeriodRecord,month:Int,year:Int):Set<Int>{

        val set = mutableSetOf<Int>()

        val cal = Calendar.getInstance()
        cal.timeInMillis = rec.predictedStart

        repeat(rec.periodLength){

            if(cal.get(Calendar.MONTH)==month &&
                cal.get(Calendar.YEAR)==year){

                set.add(cal.get(Calendar.DAY_OF_MONTH))
            }

            cal.add(Calendar.DAY_OF_MONTH,1)
        }

        return set
    }

    private fun getActual(rec:PeriodRecord,month:Int,year:Int):Set<Int>{

        val set = mutableSetOf<Int>()

        rec.confirmedDays.forEach{

            val cal = Calendar.getInstance()
            cal.timeInMillis = it

            if(cal.get(Calendar.MONTH)==month &&
                cal.get(Calendar.YEAR)==year){

                set.add(cal.get(Calendar.DAY_OF_MONTH))
            }
        }

        return set
    }

    private fun getMissed(rec:PeriodRecord,month:Int,year:Int):Set<Int>{

        val set = mutableSetOf<Int>()

        rec.missedDays.forEach{

            val cal = Calendar.getInstance()
            cal.timeInMillis = it

            if(cal.get(Calendar.MONTH)==month &&
                cal.get(Calendar.YEAR)==year){

                set.add(cal.get(Calendar.DAY_OF_MONTH))
            }
        }

        return set
    }

}