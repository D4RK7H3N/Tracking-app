package com.sagay.touristnav.domain.models

data class Destination(
    val id: String,
    val name: String,
    val category: String,
    val lat: Double,
    val lng: Double,
    val terminalId: String,
    /**
     * True for island/reef destinations that cannot be reached by road.
     * The road route is computed to the serving port terminal instead, and
     * the trip result notes that a boat ride is required from that terminal.
     */
    val requiresBoat: Boolean = false,
)
