package com.example.sheiled.ui.menubook

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.sheiled.R
import com.example.sheiled.ui.menubook.cart.CartManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProductPaymentActivity : AppCompatActivity() {

    private lateinit var payButton: Button

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ---------- For CART checkout ----------
    private var splitOrders: ArrayList<HashMap<String, Any>> = arrayListOf()
    private var paymentMethod = ""

    // ---------- For SINGLE product order ----------
    private var productName = ""
    private var price = ""
    private var partnerId = ""
    private var productId = ""
    private var imageUrl = ""

    private var quantity = ""
    private var address = ""
    private var pincode = ""
    private var contact = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_payment)

        payButton = findViewById(R.id.payButton)

        // ---------- Detect CART orders ----------
        splitOrders =
            intent.getSerializableExtra("splitOrders")
                    as? ArrayList<HashMap<String, Any>> ?: arrayListOf()

        // ---------- If CART flow ----------
        if (splitOrders.isNotEmpty()) {

            address = intent.getStringExtra("address") ?: ""
            paymentMethod = intent.getStringExtra("paymentMethod") ?: "ONLINE"

        } else {

            // ---------- SINGLE PRODUCT FLOW ----------
            productName = intent.getStringExtra("productName") ?: ""
            price = intent.getStringExtra("price") ?: ""
            partnerId = intent.getStringExtra("partnerId") ?: ""
            productId = intent.getStringExtra("productId") ?: ""
            imageUrl = intent.getStringExtra("imageUrl") ?: ""

            quantity = intent.getStringExtra("quantity") ?: ""
            address = intent.getStringExtra("address") ?: ""
            pincode = intent.getStringExtra("pincode") ?: ""
            contact = intent.getStringExtra("contact") ?: ""
        }

        payButton.setOnClickListener {

            Toast.makeText(this, "Processing Payment...", Toast.LENGTH_SHORT).show()

            if (splitOrders.isNotEmpty()) {

                createCartOrdersAfterPayment()

            } else {

                createSingleOrderAfterPayment()

            }
        }
    }

    // ====================================================
    // CART CHECKOUT PAYMENT
    // ====================================================

    private fun createCartOrdersAfterPayment() {

        val userId = auth.currentUser?.uid ?: return

        var successCount = 0
        val totalOrders = splitOrders.size

        for (order in splitOrders) {

            order["address"] = address
            order["paymentMethod"] = paymentMethod
            order["paymentStatus"] = "PAID"
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
                            "Payment Successful & Orders Placed",
                            Toast.LENGTH_LONG
                        ).show()

                        CartManager.clearCart()

                        startActivity(
                            Intent(this, OrdersActivity::class.java)
                        )

                        finish()
                    }
                }
        }
    }

    // ====================================================
    // SINGLE PRODUCT PAYMENT
    // ====================================================

    private fun createSingleOrderAfterPayment() {

        val user = auth.currentUser ?: return

        val qty = quantity.toIntOrNull() ?: 1
        val priceValue = price.toDoubleOrNull() ?: 0.0

        val itemsList = listOf(
            hashMapOf(
                "productId" to productId,
                "name" to productName,
                "price" to priceValue,
                "quantity" to qty,
                "imageUrl" to imageUrl
            )
        )

        val totalAmount = priceValue * qty

        val order = hashMapOf(

            "userId" to user.uid,
            "partnerId" to partnerId,
            "items" to itemsList,
            "totalAmount" to totalAmount,
            "address" to address,
            "pincode" to pincode,
            "contactNumber" to contact,
            "paymentMethod" to "ONLINE",
            "paymentStatus" to "PAID",
            "status" to "PENDING",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("orders")
            .add(order)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Payment Successful & Order Placed",
                    Toast.LENGTH_LONG
                ).show()

                startActivity(
                    Intent(this, OrdersActivity::class.java)
                )

                finish()
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Order failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}