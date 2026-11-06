package com.sagay.touristnav.domain.models

data class TripEstimate(
    val destination: Destination,
    val terminal: Terminal,
    val roadKm: Double,
    val durationMinutes: Double,
    val fare: Double,
    val routePoints: List<RoutePoint>,
    val straightLineKm: Double,
    val distanceWarning: Boolean,
    val fareSource: String,
    /** Set when the destination is offshore: boat ride required from the terminal. */
    val boatNote: String? = null,
)
