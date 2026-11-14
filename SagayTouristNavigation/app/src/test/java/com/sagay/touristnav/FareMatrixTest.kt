package com.sagay.touristnav

import com.sagay.touristnav.domain.FareMatrixLookup
import com.sagay.touristnav.domain.FareZoneDetector
import com.sagay.touristnav.domain.models.FareMatrixEntry
import com.sagay.touristnav.domain.models.FareOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FareMatrixTest {

    private val origins = listOf(
        FareOrigin("CITY_PROPER", "City Proper (Poblacion)", 10.8890, 123.4140, 3.5),
        FareOrigin("CROSSING_VITO", "Crossing Vito", 10.89862, 123.51183, 1.5),
        FareOrigin("PARAISO", "Paraiso", 10.89206, 123.35503, 1.5),
        FareOrigin("OLD_SAGAY", "Brgy. Old Sagay", 10.94691, 123.4260, 2.5),
        FareOrigin("BATO", "Brgy. Bato", 10.81109, 123.38001, 2.5),
    )

    private val matrix = listOf(
        FareMatrixEntry("CITY_PROPER", "TANKE", 15.0, 12.0),
        FareMatrixEntry("CITY_PROPER", "BRGY. VITO", 40.0, 32.0),
        FareMatrixEntry("CITY_PROPER", "BRGY. OLD SAGAY", 20.0, 16.0),
        FareMatrixEntry("CITY_PROPER", "BRGY. TABA-AO", 25.0, 20.0),
        FareMatrixEntry("CITY_PROPER", "SAN AGUSTIN", 25.0, 20.0),
        FareMatrixEntry("CROSSING_VITO", "VITO PORT", 23.0, 18.4),
        FareMatrixEntry("CROSSING_VITO", "BRGY. ANDRES BONIFACIO", 15.0, 12.0),
        FareMatrixEntry("PARAISO", "SAN AGUSTIN (PROPER)", 19.0, 15.2),
        FareMatrixEntry("PARAISO", "BRGY. FABRICA", 15.0, 12.0),
        FareMatrixEntry("OLD_SAGAY", "BRGY. TABA-AO", 15.0, 12.0),
        FareMatrixEntry("BATO", "BRGY. MAQUILING", 17.0, 13.6),
    )

    @Test
    fun `poblacion is detected as the city proper origin`() {
        val origin = FareZoneDetector.detect(10.8890, 123.4140, origins)
        assertEquals("CITY_PROPER", origin?.zone)
    }

    @Test
    fun `vito and bato are detected as their own origins`() {
        assertEquals(
            "CROSSING_VITO",
            FareZoneDetector.detect(10.89862, 123.51183, origins)?.zone,
        )
        assertEquals(
            "BATO",
            FareZoneDetector.detect(10.81109, 123.38001, origins)?.zone,
        )
    }

    @Test
    fun `a position outside every origin radius has no zone`() {
        // Out in the Visayan Sea, west of the city.
        assertNull(FareZoneDetector.detect(10.9800, 123.3600, origins))
    }

    @Test
    fun `matrix lookup finds the official fare for a route`() {
        val entry = FareMatrixLookup.find(matrix, "CITY_PROPER", "BRGY. VITO")
        assertEquals(40.0, entry!!.regularFare, 0.001)
        assertEquals(32.0, entry.discountedFare, 0.001)
    }

    @Test
    fun `matrix lookup uses the origin table of the passenger`() {
        assertEquals(
            23.0,
            FareMatrixLookup.find(matrix, "CROSSING_VITO", "VITO PORT")!!.regularFare,
            0.001,
        )
        // Brgy. Taba-ao is 15 from Old Sagay but 25 from the city proper.
        assertEquals(
            15.0,
            FareMatrixLookup.find(matrix, "OLD_SAGAY", "BRGY. TABA-AO")!!.regularFare,
            0.001,
        )
        assertEquals(
            25.0,
            FareMatrixLookup.find(matrix, "CITY_PROPER", "BRGY. TABA-AO")!!.regularFare,
            0.001,
        )
    }

    @Test
    fun `matrix lookup tolerates the published label variations`() {
        // "SAN AGUSTIN" in one table, "SAN AGUSTIN (PROPER)" in another.
        assertEquals(
            19.0,
            FareMatrixLookup.find(matrix, "PARAISO", "SAN AGUSTIN")!!.regularFare,
            0.001,
        )
        // "BRGY. PARAISO/FABRICA" vs "BRGY. FABRICA".
        assertEquals(
            15.0,
            FareMatrixLookup.find(matrix, "PARAISO", "BRGY. PARAISO/FABRICA")!!.regularFare,
            0.001,
        )
    }

    @Test
    fun `unknown origin or route returns no fare`() {
        assertNull(FareMatrixLookup.find(matrix, "CITY_PROPER", "BRGY. MOLABOCOC"))
        assertNull(FareMatrixLookup.find(matrix, "PARAISO", "BRGY. VITO"))
        assertNull(FareMatrixLookup.find(matrix, "CITY_PROPER", ""))
    }

    @Test
    fun `every published matrix route is priced within fifteen and forty pesos`() {
        matrix.forEach { entry ->
            assertTrue(
                "${entry.destinationZone} regular fare out of range",
                entry.regularFare in 15.0..40.0,
            )
            assertTrue(
                "${entry.destinationZone} discounted fare out of range",
                entry.discountedFare in 12.0..32.0,
            )
        }
    }
}
