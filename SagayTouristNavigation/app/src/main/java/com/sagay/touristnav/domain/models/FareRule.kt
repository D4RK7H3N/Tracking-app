package com.sagay.touristnav.domain.models

// TODO: Verify baseFare / baseKm / extraPerKm against Sagay City Resolution
//  No. 2026-117. The values in seed.json are PLACEHOLDERS for the demo.
data class FareRule(
    val id: Int,
    val baseFare: Double,
    val baseKm: Double,
    val extraPerKm: Double,
    val source: String,
)
