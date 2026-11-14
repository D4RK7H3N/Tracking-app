package com.sagay.touristnav.domain

import com.sagay.touristnav.domain.models.FareMatrixEntry
import com.sagay.touristnav.domain.models.FareOrigin

/**
 * Resolves which fare-matrix zone the passenger is currently in, and looks up
 * the official flat fare for a route in the Resolution No. 2026-117 matrix.
 */
object FareZoneDetector {

    /**
     * The origin zone whose radius covers the given position. Returns null
     * when the passenger is outside every matrix origin (the fare then falls
     * back to the road-distance estimate).
     */
    fun detect(lat: Double, lng: Double, origins: List<FareOrigin>): FareOrigin? {
        var best: FareOrigin? = null
        var bestDistance = Double.MAX_VALUE
        for (origin in origins) {
            val distance = Haversine.km(lat, lng, origin.lat, origin.lng)
            if (distance <= origin.radiusKm && distance < bestDistance) {
                bestDistance = distance
                best = origin
            }
        }
        return best
    }
}

/**
 * Looks up a route in the fare matrix. Route labels in the published matrix
 * are inconsistent ("SAN AGUSTIN" vs "SAN AGUSTIN (PROPER)", "BRGY.
 * PARAISO/FABRICA" vs "BRGY. FABRICA"), so matching compares the label, the
 * label without any parenthetical, and the last path segment.
 */
object FareMatrixLookup {

    fun find(
        entries: List<FareMatrixEntry>,
        originZone: String,
        destinationZone: String,
    ): FareMatrixEntry? {
        if (destinationZone.isBlank()) return null
        val destinationKeys = keysOf(destinationZone)
        return entries.firstOrNull { entry ->
            entry.originZone.equals(originZone, ignoreCase = true) &&
                keysOf(entry.destinationZone).any { it in destinationKeys }
        }
    }

    private fun keysOf(zone: String): Set<String> {
        val trimmed = zone.trim()
        val base = trimmed.substringBefore(" (").substringBefore("[").trim()
        val last = base.substringAfterLast('/').trim()
        val keys = mutableSetOf(trimmed, base, last)
        // "BRGY. FABRICA" should also match a row labelled "BRGY. PARAISO/FABRICA".
        if (base.startsWith("BRGY.", ignoreCase = true)) {
            keys += base.substringAfter('.').trim()
        }
        return keys
            .filter { it.isNotBlank() }
            .map { it.uppercase() }
            .toSet()
    }
}
