package com.example.sheiled.ui.menubook.periodtracker.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.sheiled.R
import com.example.sheiled.ui.menubook.periodtracker.adapter.FoodAdapter
import com.example.sheiled.ui.menubook.periodtracker.model.FoodModel

class FoodSuggestionsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view =
            inflater.inflate(R.layout.fragment_food_suggestion, container, false)

        val foods = listOf(
            FoodModel(
                "Spinach",
                "Rich in Iron & Magnesium. Helps reduce fatigue and prevents anemia during menstruation."
            ),
            FoodModel(
                "Banana",
                "High in Potassium & Vitamin B6. Reduces cramps and regulates mood swings."
            ),
            FoodModel(
                "Dark Chocolate",
                "Contains Magnesium & Antioxidants. Helps relieve cramps and improves mood."
            ),
            FoodModel(
                "Yogurt",
                "Rich in Calcium & Probiotics. Reduces bloating and supports digestion."
            ),
            FoodModel(
                "Nuts",
                "Contains Omega-3 & Magnesium. Reduces inflammation and period pain."
            ),
            FoodModel(
                "Salmon",
                "Rich in Omega-3 fatty acids. Reduces cramps and inflammation."
            ),
            FoodModel(
                "Beans",
                "High in Iron & Fiber. Maintains energy levels during periods."
            ),
            FoodModel(
                "Avocado",
                "Packed with Healthy fats & Vitamin E. Supports hormone balance."
            )
        )

        val recycler =
            view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerFood)

        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        recycler.adapter = FoodAdapter(foods)

        return view
    }
}