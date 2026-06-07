package com.example.sheiled.ui.menubook

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.sheiled.R
import com.example.sheiled.ui.menubook.cart.CartManager

class CheckoutActivity : AppCompatActivity() {

    private lateinit var addressEditText: EditText
    private lateinit var contactEditText: EditText
    private lateinit var paymentGroup: RadioGroup
    private lateinit var placeOrderBtn: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var splitOrders: ArrayList<HashMap<String, Any>>

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        addressEditText = findViewById(R.id.addressEditText)
        contactEditText = findViewById(R.id.contactEditText)
        paymentGroup = findViewById(R.id.paymentRadioGroup)
        placeOrderBtn = findViewById(R.id.placeOrderBtn)

        // Receive orders created in CartActivity
        splitOrders =
            intent.getSerializableExtra("splitOrders")
                    as? ArrayList<HashMap<String, Any>> ?: arrayListOf()

        if (splitOrders.isEmpty()) {
            Toast.makeText(this, "No orders found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        placeOrderBtn.setOnClickListener {

            val address = addressEditText.text.toString().trim()
            val contact = contactEditText.text.toString().trim()
            if (address.isEmpty()) {
                Toast.makeText(this, "Enter delivery address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (contact.isEmpty() || contact.length < 10) {
                Toast.makeText(this, "Enter valid contact number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedPaymentId = paymentGroup.checkedRadioButtonId

            if (selectedPaymentId == -1) {
                Toast.makeText(this, "Select payment method", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val paymentMethod =
                findViewById<RadioButton>(selectedPaymentId).text.toString()

            if (paymentMethod.contains("Online", true)) {

                val intent = Intent(this, ProductPaymentActivity::class.java)

                intent.putExtra("address", address)
                intent.putExtra("contact", contact)
                intent.putExtra("paymentMethod", paymentMethod)
                intent.putExtra("splitOrders", splitOrders)

                startActivity(intent)

            } else {

                createOrders(address, contact, paymentMethod)

            }
        }
    }

    private fun createOrders(address: String, contact: String, paymentMethod: String) {

        val userId = auth.currentUser?.uid ?: return

        var successCount = 0
        val totalOrders = splitOrders.size

        for (order in splitOrders) {

            // Add delivery and payment info
            order["address"] = address
            order["contactNumber"] = contact
            order["paymentMethod"] = paymentMethod

            order["paymentStatus"] =
                if (paymentMethod.contains("COD", true))
                    "NOT_PAID"
                else
                    "PAID"

            order["status"] = "PENDING"
            order["timestamp"] = System.currentTimeMillis()
            order["userId"] = userId

            db.collection("orders")
                .add(order)
                .addOnSuccessListener {

                    successCount++

                    if (successCount == totalOrders) {

                        Toast.makeText(
                            this,
                            "Order placed successfully",
                            Toast.LENGTH_LONG
                        ).show()

                        // Clear cart after successful checkout
                        CartManager.clearCart()

                        finish()
                    }
                }
                .addOnFailureListener {

                    Toast.makeText(
                        this,
                        "Order failed. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}