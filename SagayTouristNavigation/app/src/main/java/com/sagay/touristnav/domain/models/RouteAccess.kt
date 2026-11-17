package com.sagay.touristnav.domain.models

/**
 * How a passenger reaches a service from the area they are standing in.
 *
 * The Old Sagay to Sagay City corridor needs two services: the yellow
 * tricycle carries the passenger into the city, and the red tricycle serves
 * the destination there. The rule is owner-supplied and lives in the bundled
 * seed, so the owner can correct it without a code change.
 */
data class RouteAccess(
    /** The fare origin zone the rule applies from. */
    val originZone: String,
    /** The route that serves the destination. */
    val destinationRouteId: String,
    /** The route ridden first. */
    val viaRouteId: String,
    /** Where the passenger changes service. */
    val changeAt: String,
    /**
     * The matrix destination the whole journey is priced against. The two
     * services are one tricycle journey, so they are priced as one fare
     * rather than added together.
     */
    val fareZone: String,
)
