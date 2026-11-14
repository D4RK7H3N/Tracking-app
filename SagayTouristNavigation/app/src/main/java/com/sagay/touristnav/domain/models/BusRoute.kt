package com.sagay.touristnav.domain.models

/**
 * A bus / jeepney route. Unlike tricycle trips, which are direct point-to-point
 * rides, a bus route has ordered [stops] that the map shows along the way.
 */
data class BusRoute(
    val id: String,
    val name: String,
    val terminalId: String,
    val colorHex: String,
    val stops: List<RouteStop>,
)

/** One stop of a [BusRoute], in travel order. */
data class RouteStop(
    val order: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
)
