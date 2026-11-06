package com.sagay.touristnav.domain.models

data class RouteSummary(
    val roadKm: Double,
    val durationMinutes: Double,
    val points: List<RoutePoint>,
)
