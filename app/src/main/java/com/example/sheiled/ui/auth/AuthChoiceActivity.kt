package com.example.sheiled.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.sheiled.R

class AuthChoiceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth_choice)

        findViewById<Button>(R.id.btnLogin)
            .setOnClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
            }

        findViewById<Button>(R.id.btnRegister)
            .setOnClickListener {
                showRegisterChoiceDialog()
            }
    }

    private fun showRegisterChoiceDialog() {
        val options = arrayOf(
            "Woman User",
            "Cab Driver"
        )

        AlertDialog.Builder(this)
            .setTitle("Register as")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Woman user registration
                        startActivity(
                            Intent(this, RegisterActivity::class.java)
                        )
                    }
                    1 -> {
                        // Cab driver registration
                        startActivity(
                            Intent(this, DriverRegisterActivity::class.java)
                        )
                    }
                }
            }
            .setCancelable(true)
            .show()
    }
}
