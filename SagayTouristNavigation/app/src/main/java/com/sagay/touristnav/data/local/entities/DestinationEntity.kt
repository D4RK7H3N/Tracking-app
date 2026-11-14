package com.sagay.touristnav.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "destination")
data class DestinationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val lat: Double,
    val lng: Double,
    val terminalId: String,
    val requiresBoat: Boolean,
    /** Fare-matrix route this destination belongs to (null = not on the matrix). */
    val fareZone: String?,
)
