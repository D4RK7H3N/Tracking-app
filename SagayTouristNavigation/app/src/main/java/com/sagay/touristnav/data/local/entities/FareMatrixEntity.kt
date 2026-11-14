package com.sagay.touristnav.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row of the official fare matrix (Resolution No. 2026-117). */
@Entity(tableName = "fare_matrix", primaryKeys = ["originZone", "destinationZone"])
data class FareMatrixEntity(
    val originZone: String,
    val destinationZone: String,
    val regularFare: Double,
    val discountedFare: Double,
)
