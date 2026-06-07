package com.example.sheiled.ui.menubook

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sheiled.databinding.ItemCartBinding
import com.example.sheiled.ui.menubook.cart.CartItem
import com.example.sheiled.ui.menubook.cart.CartManager

class CartAdapter(
    private val items: MutableList<CartItem>,
    private val onCartUpdated: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(val binding: ItemCartBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {

        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {

        val item = items[position]

        holder.binding.tvName.text = item.productName
        holder.binding.tvPrice.text = "₹${item.price}"
        holder.binding.tvQuantity.text = item.quantity.toString()

        holder.binding.btnIncrease.setOnClickListener {

            item.quantity++
            notifyItemChanged(position)
            onCartUpdated()
        }

        holder.binding.btnDecrease.setOnClickListener {

            if (item.quantity > 1) {
                item.quantity--
                notifyItemChanged(position)
                onCartUpdated()
            }
        }

        holder.binding.btnRemove.setOnClickListener {

            CartManager.removeFromCart(item.productId)
            items.removeAt(position)
            notifyItemRemoved(position)
            onCartUpdated()
        }
    }

    override fun getItemCount(): Int = items.size
}