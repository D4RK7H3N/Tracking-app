package com.sagay.touristnav

import com.sagay.touristnav.data.local.SeedParser
import com.sagay.touristnav.domain.models.Route
import com.sagay.touristnav.domain.models.RouteStop
import com.sagay.touristnav.domain.models.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RouteTest {

    private val seed: String = File("src/main/assets/seed.json").readText()

    private val file get() = SeedParser.parse(seed)

    @Test
    fun `the bundled routes keep their stops in travel order`() {
        val oldSagay = file.routes.first { it.id == "R-OLD-SAGAY" }
        assertEquals(
            listOf(
                "Sagay City Bus Terminal (Poblacion)",
                "Old Sagay Elementary School",
                "Margaha Beach",
                "Museo sang Bata sa Negros",
                "Old Sagay Port",
            ),
            oldSagay.stops.map { it.name },
        )
    }

    @Test
    fun `every route belongs to a known terminal`() {
        val terminalIds = file.terminals.map { it.id }.toSet()
        file.routes.forEach { route ->
            assertTrue(
                "route ${route.id} references unknown terminal ${route.terminalId}",
                route.terminalId in terminalIds,
            )
        }
    }

    @Test
    fun `every destination points at a known route`() {
        val routeIds = file.routes.map { it.id }.toSet()
        file.destinations.forEach { destination ->
            assertTrue(
                "destination ${destination.id} references unknown route ${destination.routeId}",
                destination.routeId in routeIds,
            )
        }
    }

    @Test
    fun `the old sagay route is yellow and the vito and bulanon routes are pink`() {
        val byId = file.routes.associateBy { it.id }
        assertEquals("Yellow", byId.getValue("R-OLD-SAGAY").colorName)
        // Vito and Bulanon are the same colour.
        assertEquals(
            byId.getValue("R-VITO").colorHex,
            byId.getValue("R-BULANON").colorHex,
        )
        assertEquals("Pink", byId.getValue("R-VITO").colorName)
        assertEquals("Pink", byId.getValue("R-BULANON").colorName)
        assertEquals("Pink", byId.getValue("R-BATO").colorName)
    }

    @Test
    fun `white is only used for the water routes`() {
        val white = file.routes.filter { it.colorName == "White" }
        assertTrue(white.isNotEmpty())
        white.forEach { route ->
            assertEquals(
                "route ${route.id} is white but is not a boat route",
                VehicleType.BOAT.id,
                route.vehicle,
            )
        }
        // No land route may be white.
        file.routes.filter { it.vehicle != VehicleType.BOAT.id }.forEach { route ->
            assertTrue(
                "land route ${route.id} must not be white",
                route.colorName != "White",
            )
        }
    }

    @Test
    fun `water routes are direct rides and every other route is direct or lists its stops`() {
        file.routes.forEach { route ->
            if (route.vehicle == VehicleType.BOAT.id) {
                assertTrue(
                    "boat route ${route.id} is a direct ride and should have no stops",
                    route.stops.isEmpty(),
                )
            } else {
                assertTrue(
                    "route ${route.id} is either a direct ride or lists more than one stop",
                    route.stops.isEmpty() || route.stops.size > 1,
                )
            }
        }
    }

    @Test
    fun `the old sagay service is the yellow tricycle`() {
        val oldSagay = file.routes.first { it.id == "R-OLD-SAGAY" }
        assertEquals("Yellow", oldSagay.colorName)
        assertEquals(VehicleType.TRICYCLE.id, oldSagay.vehicle)
        assertEquals("Tricycle", VehicleType.TRICYCLE.displayName)
    }

    @Test
    fun `the yellow corridor keeps its owner supplied stops`() {
        val oldSagay = file.routes.first { it.id == "R-OLD-SAGAY" }
        assertEquals(VehicleType.TRICYCLE.id, oldSagay.vehicle)
        assertTrue(
            "the yellow corridor should still list its stops",
            oldSagay.stops.size > 1,
        )
    }

    @Test
    fun `the reporm 2 is its own vehicle, not a tricycle or a bus`() {
        val reporm = file.routes.filter { it.vehicle == VehicleType.REPORM_2.id }
        assertTrue(reporm.isNotEmpty())
        reporm.forEach { route ->
            assertEquals(
                "route ${route.id} should not be announced as a tricycle",
                "Reporm 2",
                VehicleType.REPORM_2.displayName,
            )
        }
    }

    @Test
    fun `a terminal takes a colour that one of its routes uses`() {
        val routesByTerminal = file.routes.groupBy { it.terminalId }
        file.terminals.forEach { terminal ->
            val colours = routesByTerminal[terminal.id].orEmpty().map { it.colorHex }
            assertTrue(
                "terminal ${terminal.id} has colour ${terminal.colorHex} " +
                    "but none of its routes use it",
                colours.contains(terminal.colorHex),
            )
        }
    }

    @Test
    fun `a terminal boards every vehicle its routes use`() {
        val routesByTerminal = file.routes.groupBy { it.terminalId }
        file.terminals.forEach { terminal ->
            val vehicles = routesByTerminal[terminal.id].orEmpty().map { it.vehicle }
            vehicles.forEach { vehicle ->
                assertTrue(
                    "terminal ${terminal.id} does not list the $vehicle of one of its routes",
                    terminal.vehicles.contains(vehicle),
                )
            }
        }
    }

    @Test
    fun `stops keep their travel order when mapped to the domain`() {
        val stops = listOf(
            RouteStop(order = 0, name = "First", lat = 10.89, lng = 123.41),
            RouteStop(order = 1, name = "Second", lat = 10.94, lng = 123.42),
        )
        val route = Route(
            id = "R-TEST",
            name = "Test route",
            terminalId = "T3",
            colorHex = "#F9A825",
            colorName = "Yellow",
            vehicle = VehicleType.BUS,
            stops = stops,
        )
        assertEquals(listOf("First", "Second"), route.stops.map { it.name })
        assertEquals("Yellow Bus", route.serviceLabel)
    }
}
