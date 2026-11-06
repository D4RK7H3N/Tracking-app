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
)

@Serializable
data class SeedFareRule(
    val id: Int,
    val baseFare: Double,
    val baseKm: Double,
    val extraPerKm: Double,
    val source: String,
)
