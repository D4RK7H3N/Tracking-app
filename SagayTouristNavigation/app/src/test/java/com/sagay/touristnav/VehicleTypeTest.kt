package com.sagay.touristnav

import com.sagay.touristnav.data.local.SeedParser
import com.sagay.touristnav.data.local.TerminalEntity
import com.sagay.touristnav.data.local.toDomain
import com.sagay.touristnav.domain.models.Terminal
import com.sagay.touristnav.domain.models.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Covers the route colours and the vehicles boarding at each terminal: the
 * part of the app that tells a tourist which vehicle to ask for.
 */
class VehicleTypeTest {

    private val seed: String = File("src/main/assets/seed.json").readText()

    @Test
    fun `vehicle ids round trip`() {
        VehicleType.entries.forEach { vehicle ->
            assertEquals(vehicle, VehicleType.fromId(vehicle.id))
        }
    }

    @Test
    fun `an unknown vehicle id resolves to nothing`() {
        assertNull(VehicleType.fromId("jeepney"))
        assertNull(VehicleType.fromId(""))
    }

    @Test
    fun `every terminal in the seed lists at least one known vehicle`() {
        SeedParser.parse(seed).terminals.forEach { terminal ->
            assertTrue(
                "terminal ${terminal.id} has no vehicles",
                terminal.vehicles.isNotEmpty(),
            )
            terminal.vehicles.forEach { id ->
                assertTrue(
                    "terminal ${terminal.id} has unknown vehicle id $id",
                    VehicleType.fromId(id) != null,
                )
            }
        }
    }

    @Test
    fun `the terminals board the vehicles of their routes`() {
        val terminals = SeedParser.parse(seed).terminals.associateBy { it.id }
        assertEquals(listOf("tricycle"), terminals.getValue("T1").vehicles)
        assertEquals(
            listOf("reporm2", "tricycle", "boat"),
            terminals.getValue("T2").vehicles,
        )
        assertEquals(
            listOf("tricycle", "bus", "boat"),
            terminals.getValue("T3").vehicles,
        )
        assertEquals("Red", terminals.getValue("T1").colorName)
        assertEquals("Pink", terminals.getValue("T2").colorName)
        assertEquals("Yellow", terminals.getValue("T3").colorName)
    }

    @Test
    fun `the old sagay route is named on the old sagay port terminal`() {
        val terminals = SeedParser.parse(seed).terminals.associateBy { it.id }
        val area = terminals.getValue("T3").serviceArea
        assertTrue(area.contains("Old Sagay to Sagay City"))
        assertTrue(area.contains("Sagay City to Old Sagay"))
    }

    @Test
    fun `a terminal describes its vehicles for the tourist`() {
        assertEquals("Tricycle", terminal("T1", "Red", listOf(VehicleType.TRICYCLE)).serviceLabel)
        assertEquals(
            "Reporm 2 + Tricycle",
            terminal("T2", "Pink", listOf(VehicleType.REPORM_2, VehicleType.TRICYCLE)).serviceLabel,
        )
        assertEquals(
            "Tricycle + Bus + Boat",
            terminal("T3", "Yellow", listOf(VehicleType.TRICYCLE, VehicleType.BUS, VehicleType.BOAT)).serviceLabel,
        )
    }

    @Test
    fun `a terminal without vehicles still reads sensibly`() {
        assertEquals("Red", terminal("T1", "Red", emptyList()).serviceLabel)
    }

    @Test
    fun `stored vehicle ids map back to the domain`() {
        val entity = TerminalEntity(
            id = "T2",
            name = "Vito Terminal",
            lat = 10.903,
            lng = 123.514,
            colorHex = "#F9A825",
            colorName = "Yellow",
            serviceArea = "Vito",
            vehicles = "reporm2,boat",
        )
        assertEquals(
            listOf(VehicleType.REPORM_2, VehicleType.BOAT),
            entity.toDomain().vehicles,
        )
    }

    @Test
    fun `an unknown stored vehicle id is dropped instead of failing the terminal`() {
        val entity = TerminalEntity(
            id = "T1",
            name = "Sagay Public Market Terminal (Poblacion)",
            lat = 10.8968,
            lng = 123.4168,
            colorHex = "#D32F2F",
            colorName = "Red",
            serviceArea = "Poblacion",
            vehicles = "tricycle,jeepney",
        )
        assertEquals(listOf(VehicleType.TRICYCLE), entity.toDomain().vehicles)
    }

    @Test
    fun `a terminal seeded before the vehicle column keeps working`() {
        val entity = TerminalEntity(
            id = "T1",
            name = "Sagay Public Market Terminal (Poblacion)",
            lat = 10.8968,
            lng = 123.4168,
            colorHex = "#D32F2F",
            colorName = "Red",
            serviceArea = "Poblacion",
        )
        assertEquals(emptyList<VehicleType>(), entity.toDomain().vehicles)
    }

    private fun terminal(
        id: String,
        colorName: String,
        vehicles: List<VehicleType>,
    ) = Terminal(
        id = id,
        name = "Terminal $id",
        lat = 10.9,
        lng = 123.4,
        colorHex = "#D32F2F",
        colorName = colorName,
        serviceArea = "",
        vehicles = vehicles,
    )
}
