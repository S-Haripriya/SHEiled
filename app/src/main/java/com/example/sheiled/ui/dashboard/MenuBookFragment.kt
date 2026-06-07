package com.example.sheiled.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.sheiled.R
import com.example.sheiled.ui.chatbot.ChatBotFragment
import com.example.sheiled.ui.menubook.periodtracker.PeriodTrackerActivity
import com.example.sheiled.ui.menubook.BrowseProductsActivity

class MenuBookFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_menu_book,
            container,
            false
        )

        // Chatbot
        view.findViewById<LinearLayout>(R.id.btnChatBot)
            .setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.mapContainer, ChatBotFragment())
                    .addToBackStack(null)
                    .commit()
            }

        view.findViewById<LinearLayout>(R.id.btnCabDashboard)
            .setOnClickListener {
                startActivity(Intent(requireContext(), CabDashboardActivity::class.java))
            }

        view.findViewById<LinearLayout>(R.id.btnBrowseProducts)
            .setOnClickListener {
                startActivity(Intent(requireContext(), BrowseProductsActivity::class.java))
            }

        view.findViewById<LinearLayout>(R.id.btnPeriodTracker)
            .setOnClickListener {
                startActivity(Intent(requireContext(), PeriodTrackerActivity::class.java))
            }

        return view
    }
}