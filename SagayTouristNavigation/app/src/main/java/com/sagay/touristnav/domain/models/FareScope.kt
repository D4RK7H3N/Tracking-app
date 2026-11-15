package com.sagay.touristnav.domain.models

/**
 * What the fare of a trip means, scoped to the vehicle that carries it.
 *
 * The city publishes fares for motorized tricycles only, so the published
 * matrix is applied to tricycle and Reporm 2 rides. A bus ride has no
 * published fare, and an island trip is a tricycle ride to the departure
 * port plus a boat leg whose fare is not published. The two legs are never
 * added up into a single total.
 */
sealed interface FareScope {

    /**
     * A tricycle or Reporm 2 ride: the whole trip is one motorized tricycle
     * journey, so the published tricycle fare applies to it.
     */
    data class MotorizedTricycle(
        /** The fare in pesos. */
        val amount: Double,
        /** True when [amount] comes from the official fare matrix. */
        val isOfficial: Boolean,
        /** Explains which rule produced [amount]. */
        val basis: String,
    ) : FareScope

    /** A bus ride: the city publishes no bus fare. */
    data object Bus : FareScope

    /**
     * An island trip: a tricycle ride to the departure port, plus a boat leg
     * whose fare is not published.
     */
    data class Island(
        /** The tricycle fare to the departure port. */
        val tricycleToPort: MotorizedTricycle,
    ) : FareScope

    /** The destination has no known route, so no fare can be claimed. */
    data object Unverified : FareScope
}
