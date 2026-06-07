package com.example.sheiled.ui.menubook.periodtracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sheiled.R
import com.example.sheiled.ui.menubook.periodtracker.model.FoodModel

class FoodAdapter(private val foodList: List<FoodModel>) :
    RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvName: TextView = itemView.findViewById(R.id.tvFoodName)
        val tvDesc: TextView = itemView.findViewById(R.id.tvFoodDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foodList[position]
        holder.tvName.text = food.name
        holder.tvDesc.text = food.description

    }

    override fun getItemCount() = foodList.size
}