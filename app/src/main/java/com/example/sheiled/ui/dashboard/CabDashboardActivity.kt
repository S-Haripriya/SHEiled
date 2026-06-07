package com.example.sheiled.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sheiled.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class CabDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cab_dashboard)

        val bottomNav =
            findViewById<BottomNavigationView>(R.id.bottomNav)

        // Default screen
        if(savedInstanceState == null) {
            startActivity(Intent(this, CabBookActivity::class.java))
        }
        bottomNav.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.menu_book -> {

                    startActivity(
                        Intent(
                            this,
                            CabBookActivity::class.java
                        )
                    )
                    true
                }

                R.id.menu_history -> {

                    startActivity(
                        Intent(
                            this,
                            UserRideHistoryActivity::class.java
                        )
                    )
                    true
                }

                else -> false
            }
        }
    }
}