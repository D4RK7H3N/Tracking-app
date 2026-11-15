package com.sagay.touristnav.domain.models

/**
 * The kinds of vehicle that carry passengers around Sagay City.
 *
 * [id] is the stable value written to `seed.json` and stored in the Room
 * `terminal.vehicles` column, so [displayName] may be reworded freely but
 * changing an [id] would orphan the stored data.
 */
enum class VehicleType(val id: String, val displayName: String) {
    /** Jeepney / bus. */
    BUS("bus", "Bus"),

    /**
     * Motorcycle with a sidecar: the common tricycle. It runs the Poblacion
     * rides and the yellow Sagay City - Old Sagay corridor.
     */
    TRICYCLE("tricycle", "Tricycle"),

    /**
     * The Reporm 2, the motorized tricycle body used across Negros
     * Occidental: a motorcycle with an open passenger sidecar. It is neither
     * the closed tricycle nor a bus, so it is announced on its own.
     */
    REPORM_2("reporm2", "Reporm 2"),

    /** Outrigger boat for the offshore islands and reefs. */
    BOAT("boat", "Boat");

    companion object {
        /** Resolves a stored id, or null when the id is unknown or malformed. */
        fun fromId(id: String): VehicleType? = entries.firstOrNull { it.id == id }
    }
}
