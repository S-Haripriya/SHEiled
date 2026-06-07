package com.example.sheiled.ui.menubook.periodtracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.sheiled.R
import com.example.sheiled.ui.menubook.periodtracker.fragment.CycleReportFragment
import com.example.sheiled.ui.menubook.periodtracker.fragment.FoodSuggestionsFragment
import com.example.sheiled.ui.menubook.periodtracker.fragments.CalendarFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class PeriodTrackerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_period_tracker)

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(CalendarFragment())
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        bottomNav.setOnItemSelectedListener { item ->

            val selectedFragment: Fragment = when (item.itemId) {
                R.id.nav_calendar -> CalendarFragment()
                R.id.nav_report -> CycleReportFragment()
                else -> CalendarFragment()
            }

            loadFragment(selectedFragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}