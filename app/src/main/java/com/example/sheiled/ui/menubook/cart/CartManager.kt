package com.example.sheiled.ui.menubook.cart



object CartManager {

    private val cartItems = mutableListOf<CartItem>()

    fun addToCart(item: CartItem) {

        val existingItem = cartItems.find { it.productId == item.productId }

        if (existingItem != null) {
            existingItem.quantity += 1
        } else {
            cartItems.add(item)
        }
    }

    fun removeFromCart(productId: String) {
        cartItems.removeAll { it.productId == productId }
    }

    fun getCartItems(): List<CartItem> {
        return cartItems
    }

    fun clearCart() {
        cartItems.clear()
    }

    fun getTotalAmount(): Double {
        return cartItems.sumOf { it.price * it.quantity }
    }

    fun isCartEmpty(): Boolean {
        return cartItems.isEmpty()
    }
}