package com.sagay.touristnav.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A transport route out of Sagay City, with the colour of its vehicle. */
@Entity(tableName = "route")
data class RouteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val terminalId: String,
    val colorHex: String,
    val colorName: String,
    /** VehicleType.id of the vehicle that runs the route. */
    val vehicle: String,
)

/** One stop of a bus route, in travel order. */
@Entity(tableName = "route_stop", primaryKeys = ["routeId", "stopOrder"])
data class RouteStopEntity(
    val routeId: String,
    val stopOrder: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
)
