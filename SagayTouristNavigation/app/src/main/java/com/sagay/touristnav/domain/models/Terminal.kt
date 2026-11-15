package com.sagay.touristnav.domain.models

data class Terminal(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val colorHex: String,
    val colorName: String,
    val serviceArea: String,
    /**
     * Vehicles boarding at this terminal, in the order they should be shown.
     * The colour of the terminal is the colour of the route, so a terminal
     * with a single vehicle reads as "the red tricycle" or "the yellow bus".
     */
    val vehicles: List<VehicleType> = emptyList(),
) {
    /**
     * How the vehicles boarding here are announced to a tourist: "Tricycle",
     * "Reporm 2 + Tricycle + Boat".
     *
     * The colour is deliberately left out: it belongs to the route, and a
     * terminal boards several routes of different colours. Falls back to the
     * colour name alone when the terminal has no vehicle recorded, so an
     * unseeded terminal still reads sensibly.
     */
    val serviceLabel: String
        get() {
            if (vehicles.isEmpty()) return colorName
            return vehicles.joinToString(" + ") { it.displayName }
        }
}
