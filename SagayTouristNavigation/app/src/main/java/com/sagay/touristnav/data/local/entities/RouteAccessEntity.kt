package com.sagay.touristnav.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * How a passenger reaches a service from the area they are standing in.
 *
 * The owner-supplied rule lives in the bundled seed, so the corridor can be
 * corrected without changing code.
 */
@Entity(tableName = "route_access", primaryKeys = ["originZone", "destinationRouteId"])
data class RouteAccessEntity(
    /** The fare origin zone the rule applies from. */
    val originZone: String,
    /** The route that serves the destination. */
    val destinationRouteId: String,
    /** The route ridden first. */
    val viaRouteId: String,
    /** Where the passenger changes service. */
    val changeAt: String,
    /** The matrix destination the whole journey is priced against. */
    val fareZone: String,
)
