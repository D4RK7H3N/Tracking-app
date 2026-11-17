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
     * The services to board, in the order the passenger boards them. Most
     * trips need one; the Old Sagay to Sagay City corridor needs the yellow
     * tricycle into the city and then the red tricycle to the destination.
     */
    val services: List<Route> = emptyList(),
    /** Where the passenger changes service, when the journey needs one. */
    val changeAt: String? = null,
) {
    /**
     * The transport route serving the destination: its colour (the colour of
     * the vehicle to ask for) and its stops. It is the last service of the
     * journey.
     */
    val route: Route? get() = services.lastOrNull()

    /**
     * The service whose owner-supplied stops are shown, or null when no
     * service has any.
     */
    val stopService: Route? get() = services.firstOrNull { it.stops.isNotEmpty() }
}
