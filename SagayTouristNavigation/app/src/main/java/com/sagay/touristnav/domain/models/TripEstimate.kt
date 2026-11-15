package com.sagay.touristnav.domain.models

data class TripEstimate(
    val destination: Destination,
    val terminal: Terminal,
    val roadKm: Double,
    val durationMinutes: Double,
    /**
     * The fare of this trip, scoped to the vehicle that carries it: a
     * tricycle ride is priced from the published matrix, a bus ride is not
     * priced at all, and an island trip prices only the tricycle leg to the
     * port.
     */
    val fare: FareScope,
    val routePoints: List<RoutePoint>,
    val straightLineKm: Double,
    val distanceWarning: Boolean,
    /** Set when the destination is offshore: boat ride required from the terminal. */
    val boatNote: String? = null,
    /** True when the 20% student / senior citizen / PWD fare was applied. */
    val fareDiscounted: Boolean = false,
    /** Matrix zone the passenger was detected in, when known. */
    val originZoneName: String? = null,
    /**
     * The transport route serving this destination: its colour (the colour of
     * the vehicle to ask for) and its stops.
     */
    val route: Route? = null,
)
