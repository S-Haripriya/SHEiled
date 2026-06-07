package com.example.sheiled.ui.menubook

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.sheiled.databinding.ActivityPlaceOrderBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PlaceOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaceOrderBinding

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var productName = ""
    private var price = ""
    private var partnerId = ""
    private var productImageUrl = ""
    private var productId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaceOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Receive product data
        productName = intent.getStringExtra("productName") ?: ""
        price = intent.getStringExtra("price") ?: ""
        partnerId = intent.getStringExtra("partnerId") ?: ""
        productImageUrl = intent.getStringExtra("imageUrl") ?: ""
        productId = intent.getStringExtra("productId") ?: ""

        // Display product
        binding.tvProductName.text = productName
        binding.tvProductPrice.text = "₹$price"

        if (productImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(productImageUrl)
                .into(binding.imgProduct)
        }

        binding.btnPlaceOrder.setOnClickListener {
            validateAndProcessOrder()
        }
    }

    private fun validateAndProcessOrder() {

        val quantity = binding.etQuantity.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val pincode = binding.etPincode.text.toString().trim()
        val contact = binding.etContact.text.toString().trim()

        if (quantity.isEmpty() || address.isEmpty() || pincode.isEmpty() || contact.isEmpty()) {
            Toast.makeText(this, "Fill all delivery details", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedPaymentId = binding.paymentRadioGroup.checkedRadioButtonId

        if (selectedPaymentId == -1) {
            Toast.makeText(this, "Select payment method", Toast.LENGTH_SHORT).show()
            return
        }

        val paymentMethod =
            findViewById<RadioButton>(selectedPaymentId).text.toString()

        if (paymentMethod.contains("Online", true)) {

            val intent = Intent(this, ProductPaymentActivity::class.java)

            intent.putExtra("productName", productName)
            intent.putExtra("price", price)
            intent.putExtra("partnerId", partnerId)
            intent.putExtra("productId", productId)
            intent.putExtra("imageUrl", productImageUrl)

            intent.putExtra("quantity", quantity)
            intent.putExtra("address", address)
            intent.putExtra("pincode", pincode)
            intent.putExtra("contact", contact)

            startActivity(intent)

        } else {

            createOrder(quantity, address, pincode, contact, "COD")

        }
    }

    private fun createOrder(
        quantity: String,
        address: String,
        pincode: String,
        contact: String,
        paymentMethod: String
    ) {

        val user = auth.currentUser ?: return

        val qty = quantity.toIntOrNull() ?: 1
        val priceValue = price.toDoubleOrNull() ?: 0.0

        val itemsList = listOf(
            hashMapOf(
                "productId" to productId,
                "name" to productName,
                "price" to priceValue,
                "quantity" to qty,
                "imageUrl" to productImageUrl
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
            "paymentMethod" to paymentMethod,
            "paymentStatus" to "NOT_PAID",
            "status" to "PENDING",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("orders")
            .add(order)
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Order placed successfully",
                    Toast.LENGTH_SHORT
                ).show()

                startActivity(
                    Intent(this, OrdersActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )

                finish()
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Order failed",
                    Toast.LENGTH_SHORT
                ).show()

                binding.btnPlaceOrder.isEnabled = true
                binding.btnPlaceOrder.text = "Place Order"
            }
    }
}