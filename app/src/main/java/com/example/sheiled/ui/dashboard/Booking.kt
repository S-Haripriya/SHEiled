package com.example.sheiled.ui.dashboard

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Booking(

    val id: String = "",
    val userId: String = "",
    val driverId: String = "",
    val userName: String= "",
    val driverName: String = "",
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val pickupName: String = "",
    val destName: String = "",
    val destLat: Double = 0.0,
    val destLng: Double = 0.0,

    val status: String = "REQUESTED",

    val createdAt: Long = 0,

    val tripStartTime: Long? = null,
    val tripEndTime: Long? = null,
var trackingEnabled: Boolean? = null,
    var driverLocation: Map<String, Double>? = null,
    var finalFare: Double? = null,
    val paymentStatus: String? = null,
    var paymentConfirmed: String? = null,
    var tripDistanceKm: Double? = null,
    var tripDurationMinutes: Double? = null,
    val cancellationFee: Double? = null

) : Parcelable