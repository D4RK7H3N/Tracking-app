package com.sagay.touristnav.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A bus / jeepney route served by a terminal. */
@Entity(tableName = "bus_route")
data class BusRouteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val terminalId: String,
    val colorHex: String,
)

/** One stop of a bus route, in travel order. */
@Entity(tableName = "bus_route_stop", primaryKeys = ["routeId", "stopOrder"])
data class BusRouteStopEntity(
    val routeId: String,
    val stopOrder: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
)
