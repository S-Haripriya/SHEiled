package com.example.sheiled.ui.menubook.periodtracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sheiled.R

class CalendarAdapter(
    private val days: List<Int?>,
    private val predicted: Set<Int>,
    private val actual: Set<Int>,
    private val missed: Set<Int>
) : RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDay: TextView = view.findViewById(R.id.tvDay)
        val indicator: View = view.findViewById(R.id.dayIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount() = days.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val day = days[position]

        if (day == null) {
            holder.tvDay.text = ""
            holder.indicator.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            return
        }

        holder.tvDay.text = day.toString()

        when {

            actual.contains(day) -> {
                holder.indicator.setBackgroundResource(R.drawable.bg_day_actual)
            }

            missed.contains(day) -> {
                holder.indicator.setBackgroundResource(R.drawable.bg_day_delay)
            }

            predicted.contains(day) -> {
                holder.indicator.setBackgroundResource(R.drawable.bg_day_predicted)
            }

            else -> {
                holder.indicator.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        }
    }
}