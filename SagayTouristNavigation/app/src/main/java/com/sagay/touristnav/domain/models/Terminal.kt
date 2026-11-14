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
     * with a single vehicle reads as "the red tricycle" or "the white bus".
     */
    val vehicles: List<VehicleType> = emptyList(),
) {
    /**
     * How the service is announced to a tourist: "Red Tricycle",
     * "Yellow Reporm 2 Tricycle", "White Bus + Boat".
     *
     * Falls back to the colour name alone when the terminal has no vehicle
     * recorded, so an unseeded terminal still reads sensibly.
     */
    val serviceLabel: String
        get() {
            if (vehicles.isEmpty()) return colorName
            val primary = vehicles.first()
            val label = "$colorName ${primary.displayName}"
            val extra = vehicles.drop(1)
            return if (extra.isEmpty()) {
                label
            } else {
                "$label + " + extra.joinToString(", ") { it.displayName }
            }
        }
}
