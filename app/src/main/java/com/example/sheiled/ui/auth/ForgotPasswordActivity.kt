package com.example.sheiled.ui.auth
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.example.sheiled.R
class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val auth = FirebaseAuth.getInstance()
        val email = findViewById<EditText>(R.id.etEmail)

        findViewById<Button>(R.id.btnSendOtp).setOnClickListener {

            auth.sendPasswordResetEmail(email.text.toString())
                .addOnSuccessListener {
                    Toast.makeText(this,
                        "OTP reset link sent to email",
                        Toast.LENGTH_LONG).show()
                    finish()
                }
        }
    }
}
