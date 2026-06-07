package com.example.sheiled.ui.dashboard

data class BookingRequest(
    var requestId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhone: String = "",
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val destLat: Double = 0.0,
    val destLng: Double = 0.0,
    val timestamp: Long = 0L,
    var status: String = "requested",
    val driverId: String = "",
    var tripStartTime: Long? = null,
    var tripEndTime: Long? = null,
    var tripDistanceKm: Double? = null,
    var tripDurationMinutes: Double? = null,
    var finalFare: Double? = null
)
