package com.example.sheiled.ui.menubook

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sheiled.ui.menubook.cart.CartItem
import com.example.sheiled.ui.menubook.cart.CartManager
import com.example.sheiled.databinding.ItemWomenProductBinding

class WomenProductAdapter(
    private val products: List<Map<String, Any>>,
    private val onBuyClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<WomenProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(val binding: ItemWomenProductBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {

        val binding = ItemWomenProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {

        val context = holder.itemView.context
        val product = products[position]

        val name = product["name"]?.toString() ?: ""
        val priceString = product["price"]?.toString() ?: "0"
        val imageUrl = product["imageUrl"]?.toString() ?: ""
        val partnerId = product["partnerId"]?.toString() ?: ""
        val productId = product["id"]?.toString() ?: ""

        val price = priceString.toDoubleOrNull() ?: 0.0

        holder.binding.tvName.text = name
        holder.binding.tvPrice.text = "₹$price"

        if (imageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(imageUrl)
                .into(holder.binding.imgProduct)
        }

        /* =======================
           ADD TO CART
        ======================= */

        holder.binding.btnAddToCart.setOnClickListener {

            val cartItem = CartItem(
                productId = productId,
                productName = name,
                price = price,
                quantity = 1,
                imageUrl = imageUrl,
                partnerId = partnerId
            )

            CartManager.addToCart(cartItem)

            Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show()
        }

        /* =======================
           BUY NOW
        ======================= */

        holder.binding.btnBuy.setOnClickListener {

            onBuyClick(
                mapOf(
                    "productId" to productId,
                    "name" to name,
                    "price" to priceString,
                    "imageUrl" to imageUrl,
                    "partnerId" to partnerId
                )
            )
        }
    }

    override fun getItemCount(): Int = products.size
}