package com.example.sheiled.ui.dashboard

data class RideHistory(

    val id: String = "",

    val userId: String = "",
    val driverId: String = "",
    var driverName: String = "",

    val pickupName: String = "",
    var userName:String = "",
    val destName: String = "",

    val finalFare: Double? = 0.0,

    val cancellationFee: Double? = 0.0,

    val status: String = "",

    val cancelledBy: String? = null,

    val paymentStatus: String? = null,

    val createdAt: Long = 0
)