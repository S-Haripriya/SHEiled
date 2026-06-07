package com.example.sheiled.ui.menubook.periodtracker.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.sheiled.ui.menubook.periodtracker.model.PeriodRecord

class PeriodRepository {

    private val db = FirebaseFirestore.getInstance()

    private val uid
        get() = FirebaseAuth.getInstance().currentUser?.uid

    fun savePeriod(record: PeriodRecord){

        val user = uid ?: return

        db.collection("users")
            .document(user)
            .collection("periods")
            .document("current")
            .set(record)
    }

    fun getPeriod(callback:(PeriodRecord?)->Unit){

        val user = uid ?: return

        db.collection("users")
            .document(user)
            .collection("periods")
            .document("current")
            .get()
            .addOnSuccessListener {

                if(it.exists()){

                    val record =
                        it.toObject(PeriodRecord::class.java)

                    callback(record)

                }else{
                    callback(null)
                }
            }
    }
    fun saveCompletedCycle(
        startDate: Long,
        endDate: Long,
        previousStartDate: Long,
        standardCycle: Int,
        standardPeriod: Int,
        actualPeriodLength: Int
    ) {

        val user = uid ?: return

        val actualPeriodLength =
            ((endDate - startDate) / (1000 * 60 * 60 * 24)).toInt() + 1

        val actualCycleLength =
            if(previousStartDate == 0L) standardCycle
            else((startDate - previousStartDate) / (1000 * 60 * 60 * 24)).toInt()

        val periodVariation = actualPeriodLength - standardCycle
        val cycleVariation = actualCycleLength - standardCycle

        val month = java.text.SimpleDateFormat(
            "MMM yyyy",
            java.util.Locale.getDefault()
        ).format(java.util.Date(startDate))

        val data = hashMapOf(
            "startDate" to startDate,
            "endDate" to endDate,
            "actualPeriodLength" to actualPeriodLength,
            "standardPeriodLength" to standardPeriod,
            "periodVariation" to periodVariation,
            "actualCycleLength" to actualCycleLength,
            "standardCycleLength" to standardCycle,
            "cycleVariation" to cycleVariation,
            "month" to month
        )

        db.collection("users")
            .document(user)
            .collection("cycle_history")
            .add(data)
    }
}