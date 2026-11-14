package com.sagay.touristnav.domain.models

/** One row of the official Sagay City tricycle fare matrix. */
data class FareMatrixEntry(
    val originZone: String,
    val destinationZone: String,
    val regularFare: Double,
    val discountedFare: Double,
)

/**
 * A fare-matrix origin (a terminal / barangay the matrix lists fares "from").
 * [radiusKm] is the area around the origin point that counts as being in
 * that zone; it is inferred from public map data, not published.
 */
data class FareOrigin(
    val zone: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusKm: Double,
)
