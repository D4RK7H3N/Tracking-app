package com.sagay.touristnav.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// TODO: baseFare / baseKm / extraPerKm seeded from assets/seed.json are
//  PLACEHOLDERS. Verify against Sagay City Resolution No. 2026-117.
@Entity(tableName = "fare_rule")
data class FareRuleEntity(
    @PrimaryKey val id: Int,
    val baseFare: Double,
    val baseKm: Double,
    val extraPerKm: Double,
    val source: String,
)
