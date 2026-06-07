package com.example.sheiled.ui.menubook.periodtracker.fragment

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.sheiled.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CycleReportFragment : Fragment() {

    private lateinit var chart: BarChart
    private lateinit var historyContainer: LinearLayout

    private lateinit var tvAvgCycle: TextView
    private lateinit var tvAvgPeriod: TextView
    private lateinit var tvLastCycle: TextView
    private lateinit var tvPrediction: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_cycle_report,
            container,
            false
        )

        chart = view.findViewById(R.id.barChart)
        historyContainer = view.findViewById(R.id.historyContainer)

        tvAvgCycle = view.findViewById(R.id.tvAvgCycle)
        tvAvgPeriod = view.findViewById(R.id.tvAvgPeriod)
        tvLastCycle = view.findViewById(R.id.tvLastCycle)
        tvPrediction = view.findViewById(R.id.tvPrediction)

        loadHistory()

        return view
    }

    private fun loadHistory() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("cycle_history")
            .orderBy("startDate")
            .get()
            .addOnSuccessListener { docs ->

                if (docs.isEmpty) {
                    chart.setNoDataText("No cycle data available")
                    return@addOnSuccessListener
                }

                historyContainer.removeAllViews()

                val entries = mutableListOf<BarEntry>()
                val months = mutableListOf<String>()

                val cycleLengths = mutableListOf<Int>()
                val periodLengths = mutableListOf<Int>()

                val sdfMonth = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

                docs.forEachIndexed { index, doc ->

                    val start = doc.getLong("startDate") ?: return@forEachIndexed
                    val end = doc.getLong("endDate") ?: return@forEachIndexed

                    val cycleLength =
                        (doc.get("actualCycleLength") as? Number)?.toInt() ?: 0

                    val periodLength =
                        (doc.get("actualPeriodLength") as? Number)?.toInt() ?: 0

                    cycleLengths.add(cycleLength)
                    periodLengths.add(periodLength)

                    entries.add(
                        BarEntry(index.toFloat(), cycleLength.toFloat())
                    )

                    val month = sdfMonth.format(Date(start))
                    months.add(month)

                    addHistoryRow(
                        month,
                        sdfDate.format(Date(start)),
                        sdfDate.format(Date(end)),
                        cycleLength
                    )
                }

                updateInsights(cycleLengths, periodLengths)

                val dataSet = BarDataSet(entries, "Cycle Length")
                dataSet.valueTextSize = 12f
                dataSet.color =
                    ContextCompat.getColor(requireContext(), R.color.purple_500)

                val barData = BarData(dataSet)

                chart.data = barData
                chart.description.isEnabled = false
                chart.axisRight.isEnabled = false
                chart.legend.isEnabled = false

                chart.xAxis.granularity = 1f
                chart.xAxis.valueFormatter =
                    object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return months.getOrElse(value.toInt()) { "" }
                        }
                    }

                chart.axisLeft.axisMinimum = 0f

                chart.animateY(800)
                chart.invalidate()
            }
    }

    private fun updateInsights(
        cycles: List<Int>,
        periods: List<Int>
    ) {

        if (cycles.isEmpty()) return

        val avgCycle = cycles.average().toInt()
        val avgPeriod = periods.average().toInt()
        val lastCycle = cycles.last()

        tvAvgCycle.text = "$avgCycle days"
        tvAvgPeriod.text = "$avgPeriod days"
        tvLastCycle.text = "$lastCycle days"

        val prediction = when {
            lastCycle > avgCycle -> "Cycle slightly longer than usual"
            lastCycle < avgCycle -> "Cycle slightly shorter than usual"
            else -> "Cycle consistent"
        }

        tvPrediction.text = prediction
    }

    private fun addHistoryRow(
        month: String,
        start: String,
        end: String,
        cycle: Int
    ) {

        val row = layoutInflater.inflate(
            R.layout.item_cycle_history,
            historyContainer,
            false
        )

        row.findViewById<TextView>(R.id.tvMonth).text = month
        row.findViewById<TextView>(R.id.tvStart).text = start
        row.findViewById<TextView>(R.id.tvEnd).text = end
        row.findViewById<TextView>(R.id.tvCycle).text = "$cycle d"

        historyContainer.addView(row)
    }
}