package com.sagay.touristnav

import com.sagay.touristnav.domain.FareCalculator
import com.sagay.touristnav.domain.models.FareRule
import org.junit.Assert.assertEquals
import org.junit.Test

class FareCalculatorTest {

    private val rule = FareRule(
        id = 1,
        baseFare = 15.0,
        baseKm = 3.0,
        extraPerKm = 2.0,
        source = "Sagay City Resolution No. 2026-117",
    )

    @Test
    fun `exactly 3 km charges only the base fare`() {
        assertEquals(15.0, FareCalculator.compute(rule, 3.0), 0.001)
    }

    @Test
    fun `3 point 1 km rounds up to one extra step`() {
        // ceil(3.1 - 3.0) = 1 step -> 15 + 1 * 2 = 17
        assertEquals(17.0, FareCalculator.compute(rule, 3.1), 0.001)
    }

    @Test
    fun `5 km adds two extra steps`() {
        // ceil(5.0 - 3.0) = 2 steps -> 15 + 2 * 2 = 19
        assertEquals(19.0, FareCalculator.compute(rule, 5.0), 0.001)
    }

    @Test
    fun `exactly one kilometer above base charges exactly one step`() {
        // ceil(4.0 - 3.0) = 1 step -> 17 (boundary: no second step)
        assertEquals(17.0, FareCalculator.compute(rule, 4.0), 0.001)
    }

    @Test
    fun `just above the step boundary adds the next step`() {
        // ceil(4.01 - 3.0) = 2 steps -> 19
        assertEquals(19.0, FareCalculator.compute(rule, 4.01), 0.001)
    }

    @Test
    fun `large distance adds one step per started kilometer`() {
        // ceil(12.0 - 3.0) = 9 steps -> 15 + 9 * 2 = 33
        assertEquals(33.0, FareCalculator.compute(rule, 12.0), 0.001)
    }

    @Test
    fun `zero km charges only the base fare`() {
        assertEquals(15.0, FareCalculator.compute(rule, 0.0), 0.001)
    }
}
