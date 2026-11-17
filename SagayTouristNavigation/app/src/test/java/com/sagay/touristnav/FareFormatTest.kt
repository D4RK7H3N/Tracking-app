package com.sagay.touristnav

import com.sagay.touristnav.domain.FareFormat
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A published fare is printed as published: the matrix has fares with
 * centavos (13.60 pesos), and rounding them up to a whole peso would
 * overcharge the passenger.
 */
class FareFormatTest {

    @Test
    fun `a whole peso amount prints without decimals`() {
        assertEquals("\u20B120", FareFormat.peso(20.0))
        assertEquals("\u20B116", FareFormat.peso(16.0))
        assertEquals("\u20B140", FareFormat.peso(40.0))
    }

    @Test
    fun `an amount with centavos keeps them`() {
        assertEquals("\u20B113.60", FareFormat.peso(13.6))
        assertEquals("\u20B124.80", FareFormat.peso(24.8))
        assertEquals("\u20B118.40", FareFormat.peso(18.4))
    }
}
