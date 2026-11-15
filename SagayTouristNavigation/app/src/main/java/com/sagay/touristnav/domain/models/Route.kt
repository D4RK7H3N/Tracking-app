package com.sagay.touristnav.domain.models

/**
 * A transport route out of Sagay City.
 *
 * The colour belongs to the route, because it is the colour of the vehicle a
 * tourist has to ask for: the tricycle to Old Sagay is yellow, the bus to Vito
 * and to Bulanon is the same pink one, the tricycle inside Poblacion is red.
 * One terminal can therefore board several colours (Old Sagay Port boards the
 * yellow tricycle and the pink bus to Bulanon).
 */
data class Route(
    val id: String,
    val name: String,
    val terminalId: String,
    val colorHex: String,
    val colorName: String,
    val vehicle: VehicleType,
    /**
     * The owner-supplied stops of the route, in travel order. A direct ride
     * has none.
     */
    val stops: List<RouteStop> = emptyList(),
) {
    /** How the service is announced: "Yellow Tricycle", "Pink Bus". */
    val serviceLabel: String get() = "$colorName ${vehicle.displayName}"
}

/** One stop of a [Route], in travel order. */
data class RouteStop(
    val order: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
)
