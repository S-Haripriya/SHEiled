package com.example.sheiled.safety

import android.content.Context
import org.json.JSONArray

class RiskDetectionManager {

    private var poiCount = 0
    private var commercialCount = 0
    private var nearestPoliceDistance = Double.MAX_VALUE
    private var nearestHospitalDistance = Double.MAX_VALUE

    fun reset() {
        poiCount = 0
        commercialCount = 0
        nearestPoliceDistance = Double.MAX_VALUE
        nearestHospitalDistance = Double.MAX_VALUE
    }

    fun updatePlace(
        types: JSONArray,
        distanceMeters: Double,
        isOpen: Boolean
    ) {

        poiCount++

        var isCommercial = false

        for (i in 0 until types.length()) {
            val type = types.getString(i)

            if (type == "police") {
                nearestPoliceDistance =
                    minOf(nearestPoliceDistance, distanceMeters)
            }

            if (type == "hospital") {
                nearestHospitalDistance =
                    minOf(nearestHospitalDistance, distanceMeters)
            }

            if (type in listOf(
                    "restaurant",
                    "shopping_mall",
                    "bus_station",
                    "train_station",
                    "cafe",
                    "movie_theater",
                    "supermarket"
                )
            ) {
                isCommercial = true
            }
        }

        if (isCommercial && isOpen) {
            commercialCount += 2
        } else if (isCommercial && !isOpen) {
            commercialCount += 0
        }
    }

    fun calculateRisk(
        hour: Int,
        speedKmph: Double,
        suddenStopDetected: Boolean
    ): Int {

        return RiskEngine.calculateRiskScore(
            hour,
            poiCount,
            commercialCount,
            nearestPoliceDistance,
            nearestHospitalDistance,
            speedKmph,
            suddenStopDetected
        )
    }
}