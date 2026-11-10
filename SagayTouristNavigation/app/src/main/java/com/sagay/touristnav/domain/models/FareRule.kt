package com.sagay.touristnav.domain.models

/**
 * Working fare rule until the exact wording of Sagay City Resolution
 * No. 2026-117 is confirmed: [baseFare] covers the first [baseKm], every
 * started kilometer beyond adds [extraPerKm] (ceil), and the result is
 * clamped to [maxFare] when set (city rule: fares stay within 15-20).
 */
data class FareRule(
    val id: Int,
    val baseFare: Double,
    val baseKm: Double,
    val extraPerKm: Double,
    val source: String,
    val maxFare: Double? = null,
)
