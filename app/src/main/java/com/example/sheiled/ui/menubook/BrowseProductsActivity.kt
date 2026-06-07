package com.example.sheiled.ui.menubook

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.sheiled.R
import com.example.sheiled.databinding.ActivityBrowseProductsBinding

class BrowseProductsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowseProductsBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowseProductsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView
        binding.rvProducts.layoutManager = GridLayoutManager(this, 2)

        // Setup Bottom Navigation
        setupBottomNavigation()

        loadProducts()
    }

    private fun setupBottomNavigation() {

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        bottomNav.selectedItemId = R.id.nav_products

        bottomNav.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_products -> true

                R.id.nav_cart -> {
                    startActivity(Intent(this, CartActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                R.id.nav_orders -> {
                    startActivity(Intent(this, OrdersActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }

                else -> false
            }
        }
    }

    private fun loadProducts() {

        db.collection("products")
            .get()
            .addOnSuccessListener { result ->

                val products = result.documents.mapNotNull { doc ->

                    val data = doc.data ?: return@mapNotNull null

                    val productMap = HashMap(data)
                    productMap["id"] = doc.id

                    productMap
                }

                val adapter = WomenProductAdapter(products) { product ->

                    val intent = Intent(this, PlaceOrderActivity::class.java)
                    intent.putExtra("productId", product["id"]?.toString() ?: "")
                    intent.putExtra("productName", product["name"]?.toString() ?: "")
                    intent.putExtra("price", product["price"]?.toString() ?: "")
                    intent.putExtra("partnerId", product["partnerId"]?.toString() ?: "")
                    intent.putExtra("imageUrl", product["imageUrl"]?.toString() ?: "")

                    startActivity(intent)
                }

                binding.rvProducts.adapter = adapter
            }
    }
}