package com.sagay.touristnav.domain

import com.sagay.touristnav.domain.models.RoutePoint
import kotlin.math.cos
import kotlin.math.sqrt

data class GuidanceSnapshot(
    val travelledKm: Double,
    val remainingKm: Double,
    val progress: Float,
    val nextPrompt: String,
    val arrived: Boolean,
)

object JourneyGuidance {

    fun snapshot(
        points: List<RoutePoint>,
        currentLat: Double,
        currentLng: Double,
        destinationName: String,
        changeAtName: String? = null,
        changeAtLat: Double? = null,
        changeAtLng: Double? = null,
    ): GuidanceSnapshot {
        if (points.size < 2) {
            return GuidanceSnapshot(
                travelledKm = 0.0,
                remainingKm = 0.0,
                progress = 0f,
                nextPrompt = "Route unavailable for guidance",
                arrived = false,
            )
        }
        val segments = segments(points)
        val total = segments.last().endKm
        val position = project(currentLat, currentLng, segments)
        val remaining = (total - position).coerceAtLeast(0.0)
        val arrived = remaining <= ARRIVAL_KM
        val changeKm = if (
            changeAtName != null && changeAtLat != null && changeAtLng != null
        ) {
            project(changeAtLat, changeAtLng, segments)
        } else {
            null
        }
        val nextPrompt = when {
            arrived -> "You have arrived at $destinationName"
            changeKm != null && position < changeKm - CHANGE_TOLERANCE_KM ->
                "Change at $changeAtName"
            changeKm != null -> "Continue to $destinationName"
            else -> "Continue to $destinationName"
        }
        return GuidanceSnapshot(
            travelledKm = position,
            remainingKm = remaining,
            progress = (position / total).coerceIn(0.0, 1.0).toFloat(),
            nextPrompt = nextPrompt,
            arrived = arrived,
        )
    }

    private data class Segment(
        val startKm: Double,
        val endKm: Double,
        val aLat: Double,
        val aLng: Double,
        val bLat: Double,
        val bLng: Double,
    )

    private fun segments(points: List<RoutePoint>): List<Segment> {
        val result = mutableListOf<Segment>()
        var travelled = 0.0
        for (index in 1 until points.size) {
            val a = points[index - 1]
            val b = points[index]
            val length = Haversine.km(a.lat, a.lng, b.lat, b.lng)
            result.add(
                Segment(travelled, travelled + length, a.lat, a.lng, b.lat, b.lng),
            )
            travelled += length
        }
        return result
    }

    private fun project(lat: Double, lng: Double, segments: List<Segment>): Double {
        var bestKm = 0.0
        var bestDistance = Double.MAX_VALUE
        for (segment in segments) {
            val cosLat = cos(Math.toRadians((segment.aLat + segment.bLat) / 2.0))
            val ax = segment.aLng * cosLat * KM_PER_DEGREE
            val ay = segment.aLat * KM_PER_DEGREE
            val bx = segment.bLng * cosLat * KM_PER_DEGREE
            val by = segment.bLat * KM_PER_DEGREE
            val px = lng * cosLat * KM_PER_DEGREE
            val py = lat * KM_PER_DEGREE
            val dx = bx - ax
            val dy = by - ay
            val lengthSq = dx * dx + dy * dy
            val t = if (lengthSq == 0.0) {
                0.0
            } else {
                ((px - ax) * dx + (py - ay) * dy) / lengthSq
            }.coerceIn(0.0, 1.0)
            val cx = ax + t * dx
            val cy = ay + t * dy
            val distance = sqrt((px - cx) * (px - cx) + (py - cy) * (py - cy))
            if (distance < bestDistance) {
                bestDistance = distance
                bestKm = segment.startKm + t * (segment.endKm - segment.startKm)
            }
        }
        return bestKm
    }

    private const val KM_PER_DEGREE = 111.32
    private const val ARRIVAL_KM = 0.15
    private const val CHANGE_TOLERANCE_KM = 0.05
}
