package com.example.sheiled.safety

import android.util.Log

object RiskEngine {

    fun calculateRiskScore(
        hour: Int,
        poiCount: Int,
        commercialCount: Int,
        nearestPoliceDistance: Double,
        nearestHospitalDistance: Double,
        speedKmph: Double,
        suddenStopDetected: Boolean
    ): Int {

        /* -------- TIME RISK -------- */
        val timeRisk = when {
            hour in 6..18 -> 5
            hour in 19..22 -> 15
            else -> 30
        }

        /* -------- POI DENSITY -------- */
        val poiRisk = when {
            poiCount >= 10 -> 5
            poiCount >= 5 -> 15
            else -> 25
        }

        /* -------- COMMERCIAL ACTIVITY -------- */
        val commercialRisk = when {
            commercialCount >= 6 -> 0     // Active crowded area
            commercialCount >= 3 -> 10
            else -> 25                    // Low/no activity
        }

        /* -------- POLICE DISTANCE -------- */
        val policeRisk = when {
            nearestPoliceDistance <= 800 -> 0
            nearestPoliceDistance <= 2000 -> 10
            nearestPoliceDistance <= 4000 -> 20
            else -> 30
        }

        /* -------- HOSPITAL DISTANCE -------- */
        val hospitalRisk =
            if (nearestHospitalDistance > 3000) 10 else 0

        /* -------- SPEED RISK -------- */
        val speedRisk =
            if (speedKmph > 80) 10 else 0

        /* -------- NORMALIZATION -------- */
        val normalizedTime = timeRisk / 30.0
        val normalizedPoi = poiRisk / 25.0
        val normalizedCommercial = commercialRisk / 25.0
        val normalizedPolice = policeRisk / 25.0
        val normalizedHospital = hospitalRisk / 10.0
        val normalizedSpeed = speedRisk / 10.0

        var score = (
                0.25 * normalizedTime +
                        0.20 * normalizedPoi +
                        0.15 * normalizedCommercial +
                        0.15 * normalizedPolice +
                        0.10 * normalizedHospital +
                        0.15 * normalizedSpeed
                ) * 100

        /* -------- CONTEXT AWARENESS -------- */
        val isWalking = speedKmph < 5
        if (isWalking && hour > 20 && poiCount < 5) {
            score += 10
        }

        /* -------- BEHAVIOR ADAPTATION -------- */
        if (score > 70 && suddenStopDetected) {
            score += 10
        }

        val finalScore = score.toInt().coerceIn(0, 100)

        Log.d( "RISK_DEBUG",
            """
            -------- RISK DEBUG --------
            Hour: $hour
            POI Count: $poiCount
            Commercial Count: $commercialCount
            Police Distance: $nearestPoliceDistance
            Hospital Distance: $nearestHospitalDistance

            Time Risk: $timeRisk
            POI Risk: $poiRisk
            Commercial Risk: $commercialRisk
            Police Risk: $policeRisk
            Hospital Risk: $hospitalRisk
            Speed Risk: $speedRisk

            FINAL SCORE: $finalScore
            ----------------------------
            """.trimIndent())

        return finalScore
    }
}