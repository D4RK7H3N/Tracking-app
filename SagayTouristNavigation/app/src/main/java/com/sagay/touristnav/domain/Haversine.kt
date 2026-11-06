package com.sagay.touristnav.domain

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Haversine {

    private const val EARTH_RADIUS_KM = 6371.0

    fun km(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        return 2 * EARTH_RADIUS_KM * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    /**
     * Sanity check only (never used for fare): the road distance should be at
     * least the straight-line distance and no more than 3x it.
     */
    fun isRoadDistanceSuspicious(roadKm: Double, straightLineKm: Double): Boolean =
        straightLineKm > 0.0 && (roadKm < straightLineKm || roadKm > straightLineKm * 3.0)
}
