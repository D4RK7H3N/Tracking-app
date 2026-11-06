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
        assertEquals(17, file.destinations.size)
        assertTrue(file.fareRule != null)
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
    fun `five island destinations are flagged for boat routing`() {
        val file = SeedParser.parse(seed)
        assertEquals(5, file.destinations.count { it.boat })
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
    fun `terminal colours are valid hex codes`() {
        SeedParser.parse(seed).terminals.forEach { terminal ->
            assertTrue(
                "terminal ${terminal.id} colour is not #RRGGBB: ${terminal.colorHex}",
                Regex("^#[0-9A-Fa-f]{6}$").matches(terminal.colorHex),
            )
        }
    }
}
