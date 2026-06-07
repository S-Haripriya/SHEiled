package com.example.sheiled.ui.menubook



import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.sheiled.R
import com.example.sheiled.ui.menubook.cart.CartManager

class CartActivity : AppCompatActivity() {

    private lateinit var totalText: TextView
    private lateinit var checkoutBtn: Button
    private lateinit var bottomNav: BottomNavigationView

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        totalText = findViewById(R.id.totalAmountText)
        checkoutBtn = findViewById(R.id.checkoutBtn)
        bottomNav = findViewById(R.id.bottomNavigation)

        val recyclerView =
            findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.cartRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = CartAdapter(CartManager.getCartItems().toMutableList()) {
            updateTotal()
        }

        recyclerView.adapter = adapter

        updateTotal()
        setupBottomNavigation()

        checkoutBtn.setOnClickListener {

            if (CartManager.isCartEmpty()) {
                Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createSplitOrders()
        }
    }

    private fun setupBottomNavigation() {

        bottomNav.selectedItemId = R.id.nav_products

        bottomNav.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_products -> {
                    startActivity(Intent(this, BrowseProductsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_cart -> true

                R.id.nav_orders -> {
                    startActivity(Intent(this, OrdersActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                else -> false
            }
        }
    }

    private fun updateTotal() {
        totalText.text = "Total: ₹${CartManager.getTotalAmount()}"
    }

    private fun createSplitOrders() {

        val user = auth.currentUser ?: return
        val cartItems = CartManager.getCartItems()

        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val grouped = cartItems.groupBy { it.partnerId }

        val splitOrders = ArrayList<HashMap<String, Any>>()

        for ((partnerId, items) in grouped) {

            val itemsList = items.map {

                hashMapOf(
                    "productId" to it.productId,
                    "name" to it.productName,
                    "price" to it.price,
                    "quantity" to it.quantity,
                    "imageUrl" to it.imageUrl
                )
            }

            val totalAmount = items.sumOf { it.price * it.quantity }

            val orderData = hashMapOf(

                "userId" to user.uid,
                "partnerId" to partnerId,
                "items" to itemsList,
                "totalAmount" to totalAmount,
                "status" to "PENDING",
                "paymentStatus" to "NOT_PAID",
                "timestamp" to System.currentTimeMillis()
            )

            splitOrders.add(orderData)
        }

        val intent = Intent(this, CheckoutActivity::class.java)
        intent.putExtra("splitOrders", splitOrders)
        startActivity(intent)
    }
}