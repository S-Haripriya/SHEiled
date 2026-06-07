package com.example.sheiled.ui.menubook.cart



data class CartItem(

    val productId: String,
    val productName: String,
    val price: Double,
    var quantity: Int,
    val imageUrl: String,
    val partnerId: String

)