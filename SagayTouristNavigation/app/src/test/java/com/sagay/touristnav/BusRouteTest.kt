package com.sagay.touristnav

import com.sagay.touristnav.data.local.SeedParser
import com.sagay.touristnav.domain.models.BusRoute
import com.sagay.touristnav.domain.models.RouteStop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BusRouteTest {

    private val seed: String = File("src/main/assets/seed.json").readText()

    @Test
    fun `the bundled bus route keeps its stops in travel order`() {
        val routes = SeedParser.parse(seed).busRoutes
        assertEquals(1, routes.size)
        val route = routes.first()
        assertEquals("BR1", route.id)
        assertEquals("T3", route.terminalId)
        assertTrue(route.stops.size >= 2)
        assertEquals(
            listOf(
                "Sagay City Bus Terminal (Poblacion)",
                "Old Sagay Elementary School",
                "Margaha Beach",
                "Museo sang Bata sa Negros",
                "Old Sagay Port",
            ),
            route.stops.map { it.name },
        )
    }

    @Test
    fun `every bus route belongs to a known terminal`() {
        val file = SeedParser.parse(seed)
        val terminalIds = file.terminals.map { it.id }.toSet()
        file.busRoutes.forEach { route ->
            assertTrue(
                "bus route ${route.id} references unknown terminal ${route.terminalId}",
                route.terminalId in terminalIds,
            )
        }
    }

    @Test
    fun `stops keep their travel order when mapped to the domain`() {
        val stops = listOf(
            RouteStop(order = 0, name = "First", lat = 10.89, lng = 123.41),
            RouteStop(order = 1, name = "Second", lat = 10.94, lng = 123.42),
        )
        val route = BusRoute(
            id = "BR1",
            name = "Test route",
            terminalId = "T3",
            colorHex = "#FFFFFF",
            stops = stops,
        )
        assertEquals(listOf("First", "Second"), route.stops.map { it.name })
    }
}
