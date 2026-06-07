package com.example.sheiled.ui.dashboard

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Driver(
    val id: String = "",
    val name: String = "",
    val vehicleNumber: String = "",
    val area: String = "",
    val baseFare: Double = 0.0,
    val status: String = "",
    val availability:String=""
) : Parcelable
