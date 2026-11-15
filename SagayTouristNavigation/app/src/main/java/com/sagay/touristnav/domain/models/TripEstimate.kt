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
    /** Human-readable explanation of how the fare was obtained. */
    val fareBasis: String = "",
    /** True when the fare comes from the official fare matrix. */
    val fareIsOfficial: Boolean = false,
    /** True when the 20% student / senior citizen / PWD fare was applied. */
    val fareDiscounted: Boolean = false,
    /** Matrix zone the passenger was detected in, when known. */
    val originZoneName: String? = null,
    /**
     * The transport route serving this destination: its colour (the colour of
     * the vehicle to ask for) and, for bus routes, its stops.
     */
    val route: Route? = null,
)
