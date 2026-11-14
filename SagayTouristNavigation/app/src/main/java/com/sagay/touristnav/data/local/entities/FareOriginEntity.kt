package com.sagay.touristnav.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A zone the fare matrix lists fares "from". */
@Entity(tableName = "fare_origin")
data class FareOriginEntity(
    @PrimaryKey val zone: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusKm: Double,
)
