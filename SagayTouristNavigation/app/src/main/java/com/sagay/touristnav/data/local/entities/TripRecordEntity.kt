package com.sagay.touristnav.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_record")
data class TripRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val destinationId: String,
    val destinationName: String,
    val originZone: String,
    val vehicleLabel: String,
    val fare: Double,
    val discounted: Boolean,
    val passengers: Int,
    val total: Double,
    val savedAt: Long,
)
