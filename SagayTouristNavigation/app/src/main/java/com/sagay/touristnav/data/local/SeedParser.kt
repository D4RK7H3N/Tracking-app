package com.sagay.touristnav.data.local

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Pure parsing of assets/seed.json. No Android imports so it can be unit
 * tested against the bundled asset file.
 */
object SeedParser {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun parse(raw: String): SeedFile =
        json.decodeFromString<SeedFile>(raw)
}

@Serializable
data class SeedFile(
    val note: String? = null,
    val terminals: List<SeedTerminal> = emptyList(),
    val destinations: List<SeedDestination> = emptyList(),
    @SerialName("fare_rule") val fareRule: SeedFareRule? = null,
    @SerialName("fare_origins") val fareOrigins: List<SeedFareOrigin> = emptyList(),
    @SerialName("fare_matrix") val fareMatrix: List<SeedFareMatrixRow> = emptyList(),
    @SerialName("bus_routes") val busRoutes: List<SeedBusRoute> = emptyList(),
)

@Serializable
data class SeedBusRoute(
    val id: String,
    val name: String,
    val terminalId: String,
    val colorHex: String,
    val stops: List<SeedBusRouteStop> = emptyList(),
)

@Serializable
data class SeedBusRouteStop(
    val name: String,
    val lat: Double,
    val lng: Double,
)

@Serializable
data class SeedFareOrigin(
    val zone: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusKm: Double,
)

@Serializable
data class SeedFareMatrixRow(
    val origin: String,
    val destination: String,
    val regular: Double,
    val discounted: Double,
)

@Serializable
data class SeedTerminal(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val colorHex: String,
    val colorName: String,
    val serviceArea: String,
    /**
     * Vehicles boarding here, as [com.sagay.touristnav.domain.models.VehicleType.id]
     * values. Unknown ids are ignored when the row is mapped to the domain.
     */
    val vehicles: List<String> = emptyList(),
)

@Serializable
data class SeedDestination(
    val id: String,
    val name: String,
    val category: String,
    val lat: Double,
    val lng: Double,
    val terminalId: String,
    /** Island/reef destinations: road leg ends at the serving port terminal. */
    val boat: Boolean = false,
    /** Fare-matrix route this destination belongs to (null = not on matrix). */
    val zone: String? = null,
)

@Serializable
data class SeedFareRule(
    val id: Int,
    val baseFare: Double,
    val baseKm: Double,
    val extraPerKm: Double,
    val source: String,
    val maxFare: Double? = null,
)
