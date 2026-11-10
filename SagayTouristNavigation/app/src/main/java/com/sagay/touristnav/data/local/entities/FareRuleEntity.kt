package com.sagay.touristnav.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fare_rule")
data class FareRuleEntity(
    @PrimaryKey val id: Int,
    val baseFare: Double,
    val baseKm: Double,
    val extraPerKm: Double,
    val source: String,
    /** Optional ceiling (₱): computed fares are clamped to this value. */
    val maxFare: Double?,
)
