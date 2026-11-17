package com.sagay.touristnav

import com.sagay.touristnav.data.local.SeedParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Parses the actual bundled seed asset and checks the row counts and
 * integrity that SeedLoader relies on.
 */
class SeedParserTest {

    private val seed: String =
        File("src/main/assets/seed.json").readText()

    @Test
    fun `parses the expected row counts`() {
        val file = SeedParser.parse(seed)
        assertEquals(3, file.terminals.size)
        assertEquals(38, file.destinations.size)
        assertTrue(file.fareRule != null)
    }

    @Test
    fun `schools are included as destinations`() {
        val file = SeedParser.parse(seed)
        val schools = file.destinations.filter { it.category == "School" }
        assertTrue("expected Sagay National High School in seed", schools.any { it.name == "Sagay National High School" })
        assertEquals(21, schools.size)
    }

    @Test
    fun `every destination terminal id resolves`() {
        val file = SeedParser.parse(seed)
        val terminalIds = file.terminals.map { it.id }.toSet()
        file.destinations.forEach { destination ->
            assertTrue(
                "destination ${destination.id} references missing terminal ${destination.terminalId}",
                destination.terminalId in terminalIds,
            )
        }
    }

    @Test
    fun `island destinations are flagged for boat routing`() {
        val file = SeedParser.parse(seed)
        assertEquals(8, file.destinations.count { it.boat })
    }

    @Test
    fun `fare rule matches the city fare order`() {
        val rule = SeedParser.parse(seed).fareRule!!
        assertEquals(15.0, rule.baseFare, 0.001)
        assertEquals(3.0, rule.baseKm, 0.001)
        assertEquals(2.0, rule.extraPerKm, 0.001)
        assertTrue(rule.source.contains("2026-117"))
    }

    @Test
    fun `the official fare matrix and its origins are bundled`() {
        val file = SeedParser.parse(seed)
        assertEquals(40, file.fareMatrix.size)
        assertEquals(5, file.fareOrigins.size)
        // Five origin tables: City Proper, Crossing Vito, Paraiso, Old Sagay, Bato.
        assertEquals(
            setOf("CITY_PROPER", "CROSSING_VITO", "PARAISO", "OLD_SAGAY", "BATO"),
            file.fareMatrix.map { it.origin }.toSet(),
        )
    }

    @Test
    fun `the Old Sagay to Sagay City corridor fare is bundled`() {
        val file = SeedParser.parse(seed)
        val row = file.fareMatrix.firstOrNull {
            it.origin == "OLD_SAGAY" && it.destination == "SAGAY CITY"
        }
        assertTrue("expected the OLD_SAGAY -> SAGAY CITY matrix row", row != null)
        assertEquals(20.0, row!!.regular, 0.001)
        assertEquals(16.0, row.discounted, 0.001)
    }

    @Test
    fun `the corridor access rule is bundled and names known routes`() {
        val file = SeedParser.parse(seed)
        assertEquals(1, file.routeAccess.size)
        val rule = file.routeAccess.first()
        assertEquals("OLD_SAGAY", rule.originZone)
        assertEquals("R-POPLACION", rule.destinationRouteId)
        assertEquals("R-OLD-SAGAY", rule.viaRouteId)
        assertEquals("Sagay City Bus Terminal (Poblacion)", rule.changeAt)
        assertEquals("SAGAY CITY", rule.fareZone)

        val routeIds = file.routes.map { it.id }.toSet()
        assertTrue(
            "access rule names an unknown route ${rule.viaRouteId}",
            rule.viaRouteId in routeIds,
        )
        assertTrue(
            "access rule names an unknown route ${rule.destinationRouteId}",
            rule.destinationRouteId in routeIds,
        )
    }

    @Test
    fun `the corridor fare zone is a bundled matrix destination`() {
        val file = SeedParser.parse(seed)
        val destinations = file.fareMatrix.map { it.destination }.toSet()
        file.routeAccess.forEach { rule ->
            assertTrue(
                "access rule prices against unknown matrix route ${rule.fareZone}",
                rule.fareZone in destinations,
            )
        }
    }

    @Test
    fun `matrix fares stay between 15 and 40 pesos with the 20 percent discount`() {
        val file = SeedParser.parse(seed)
        file.fareMatrix.forEach { row ->
            assertTrue(
                "${row.origin} -> ${row.destination} regular fare out of range",
                row.regular in 15.0..40.0,
            )
            assertTrue(
                "${row.origin} -> ${row.destination} discounted fare out of range",
                row.discounted in 12.0..32.0,
            )
        }
    }

    @Test
    fun `destination zones resolve to matrix routes`() {
        val file = SeedParser.parse(seed)
        val destinations = file.fareMatrix.map { it.destination }.toSet()
        file.destinations.filter { it.zone != null }.forEach { destination ->
            assertTrue(
                "destination ${destination.id} references unknown route ${destination.zone}",
                destinations.any { it == destination.zone },
            )
        }
    }

    @Test
    fun `terminal colours are valid hex codes`() {
        SeedParser.parse(seed).terminals.forEach { terminal ->
            assertTrue(
                "terminal ${terminal.id} colour is not #RRGGBB: ${terminal.colorHex}",
                Regex("^#[0-9A-Fa-f]{6}$").matches(terminal.colorHex),
            )
        }
    }
}
