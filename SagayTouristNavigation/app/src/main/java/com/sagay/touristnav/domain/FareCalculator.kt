package com.sagay.touristnav.domain

import com.sagay.touristnav.domain.models.FareRule
import kotlin.math.ceil

object FareCalculator {

    /**
     * Working rule until Resolution No. 2026-117's exact partial-kilometer
     * wording is confirmed: the base fare covers the first [FareRule.baseKm],
     * and every started kilometer beyond that adds [FareRule.extraPerKm]
     * (hence ceil). The result is clamped to [FareRule.maxFare] when set
     * (city rule: fares stay within baseFare..maxFare, currently 15-20).
     * Change only this function if the resolution says otherwise, and update
     * FareCalculatorTest with it.
     */
    fun compute(fareRule: FareRule, roadKm: Double): Double {
        val raw = if (roadKm <= fareRule.baseKm) {
            fareRule.baseFare
        } else {
            fareRule.baseFare + ceil(roadKm - fareRule.baseKm) * fareRule.extraPerKm
        }
        val cap = fareRule.maxFare ?: return raw
        return raw.coerceIn(fareRule.baseFare, cap)
    }
}
