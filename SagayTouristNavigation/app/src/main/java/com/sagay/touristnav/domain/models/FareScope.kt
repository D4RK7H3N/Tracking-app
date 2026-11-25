package com.sagay.touristnav.domain.models

/**
 * What the fare of a trip means, scoped to the vehicle that carries it.
 *
 * The city publishes fares for motorized tricycles only, so the published
 * matrix is applied to tricycle and Reporm 2 rides. An island trip is a
 * tricycle ride to the departure port plus a boat leg whose fare is not
 * published. The two legs are never added up into a single total.
 */
sealed interface FareScope {

    /**
     * True when the published 20% student / senior citizen / PWD fare applies
     * to this fare.
     *
     * Only the official matrix publishes a discounted fare, so a distance
     * estimate keeps its single value and the discount must not be offered
     * for it.
     */
    val canDiscount: Boolean
        get() = when (this) {
            is MotorizedTricycle -> isOfficial
            is Island -> tricycleToPort.isOfficial
            Unverified -> false
        }

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
