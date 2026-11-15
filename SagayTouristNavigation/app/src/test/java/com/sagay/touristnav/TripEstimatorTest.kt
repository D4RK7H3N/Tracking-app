package com.sagay.touristnav

import com.sagay.touristnav.domain.TripEstimator
import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.FareMatrixEntry
import com.sagay.touristnav.domain.models.FareOrigin
import com.sagay.touristnav.domain.models.FareRule
import com.sagay.touristnav.domain.models.FareScope
import com.sagay.touristnav.domain.models.Route
import com.sagay.touristnav.domain.models.RouteSummary
import com.sagay.touristnav.domain.models.Terminal
import com.sagay.touristnav.domain.models.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The fare must describe the vehicle that actually carries the passenger.
 *
 * The published matrix prices motorized tricycles, so a bus ride is not
 * priced from it, and an island trip prices only the tricycle leg to the
 * port: the boat fare is not published and is never folded into the land
 * amount.
 */
class TripEstimatorTest {

    private val estimator = TripEstimator()

    private val rule = FareRule(
        id = 1,
        baseFare = 15.0,
        baseKm = 3.0,
        extraPerKm = 2.0,
        source = "Sagay City Resolution No. 2026-117",
        maxFare = 40.0,
    )

    private val origins = listOf(
        FareOrigin("CITY_PROPER", "City Proper (Poblacion)", 10.8890, 123.4140, 3.5),
    )

    private val matrix = listOf(
        FareMatrixEntry("CITY_PROPER", "BRGY. VITO", 40.0, 32.0),
        FareMatrixEntry("CITY_PROPER", "BRGY. BULANON", 30.0, 24.0),
    )

    private val terminal = Terminal(
        id = "T3",
        name = "Old Sagay Port Terminal",
        lat = 10.9469,
        lng = 123.426,
        colorHex = "#F9A825",
        colorName = "Yellow",
        serviceArea = "Old Sagay",
        vehicles = listOf(VehicleType.TRICYCLE, VehicleType.BUS, VehicleType.BOAT),
    )

    private val road = RouteSummary(roadKm = 6.6, durationMinutes = 18.0, points = emptyList())

    private fun destination(
        routeId: String?,
        fareZone: String? = null,
        requiresBoat: Boolean = false,
    ) = Destination(
        id = "D1",
        name = "Test destination",
        category = "Landmark",
        lat = 10.94,
        lng = 123.42,
        terminalId = "T3",
        requiresBoat = requiresBoat,
        fareZone = fareZone,
        routeId = routeId,
    )

    private fun route(id: String, vehicle: VehicleType) = Route(
        id = id,
        name = "Route $id",
        terminalId = "T3",
        colorHex = "#E91E63",
        colorName = "Pink",
        vehicle = vehicle,
    )

    private fun estimate(
        destination: Destination,
        route: Route?,
        discounted: Boolean = false,
    ) = estimator.estimate(
        originLat = 10.8890,
        originLng = 123.4140,
        destination = destination,
        terminal = terminal,
        fareRule = rule,
        roadRoute = road,
        matrixEntries = matrix,
        fareOrigins = origins,
        routes = listOfNotNull(route),
        discounted = discounted,
    )

    @Test
    fun `a tricycle ride takes the official matrix fare`() {
        val trip = estimate(
            destination = destination("R-VITO", fareZone = "BRGY. VITO"),
            route = route("R-VITO", VehicleType.REPORM_2),
        )
        val fare = trip.fare as FareScope.MotorizedTricycle
        assertEquals(40.0, fare.amount, 0.001)
        assertTrue(fare.isOfficial)
        assertTrue(fare.basis.contains("City Proper (Poblacion)"))
    }

    @Test
    fun `the 20 percent discount applies to a tricycle fare`() {
        val trip = estimate(
            destination = destination("R-VITO", fareZone = "BRGY. VITO"),
            route = route("R-VITO", VehicleType.REPORM_2),
            discounted = true,
        )
        val fare = trip.fare as FareScope.MotorizedTricycle
        assertEquals(32.0, fare.amount, 0.001)
        assertTrue(trip.fareDiscounted)
    }

    @Test
    fun `a tricycle ride outside the matrix falls back to the distance estimate`() {
        val trip = estimate(
            destination = destination("R-POPLACION"),
            route = route("R-POPLACION", VehicleType.TRICYCLE),
        )
        val fare = trip.fare as FareScope.MotorizedTricycle
        // 15 for the first 3 km, then 4 started kilometres at 2 each.
        assertEquals(23.0, fare.amount, 0.001)
        assertTrue(!fare.isOfficial)
        assertTrue(fare.basis.contains("Estimated from road distance"))
    }

    @Test
    fun `a bus ride has no fare`() {
        val trip = estimate(
            destination = destination("R-BULANON", fareZone = "BRGY. BULANON"),
            route = route("R-BULANON", VehicleType.BUS),
        )
        assertEquals(FareScope.Bus, trip.fare)
    }

    @Test
    fun `a bus ride keeps its road distance and time`() {
        val trip = estimate(
            destination = destination("R-BULANON", fareZone = "BRGY. BULANON"),
            route = route("R-BULANON", VehicleType.BUS),
        )
        assertEquals(6.6, trip.roadKm, 0.001)
        assertEquals(18.0, trip.durationMinutes, 0.001)
    }

    @Test
    fun `an island trip prices only the tricycle leg to the port`() {
        val trip = estimate(
            destination = destination(
                routeId = "R-WATER-OLD-SAGAY",
                fareZone = "BRGY. VITO",
                requiresBoat = true,
            ),
            route = route("R-WATER-OLD-SAGAY", VehicleType.BOAT),
        )
        val fare = trip.fare as FareScope.Island
        assertEquals(40.0, fare.tricycleToPort.amount, 0.001)
        assertTrue(fare.tricycleToPort.isOfficial)
    }

    @Test
    fun `an island trip notes the boat ride`() {
        val trip = estimate(
            destination = destination(
                routeId = "R-WATER-OLD-SAGAY",
                fareZone = "BRGY. VITO",
                requiresBoat = true,
            ),
            route = route("R-WATER-OLD-SAGAY", VehicleType.BOAT),
        )
        assertEquals("Boat ride required from Old Sagay Port Terminal", trip.boatNote)
    }

    @Test
    fun `a destination without a route has no fare`() {
        val trip = estimate(destination = destination(routeId = null), route = null)
        assertEquals(FareScope.Unverified, trip.fare)
        assertEquals(null, trip.route)
    }

    @Test
    fun `the discount never applies to a bus fare`() {
        val trip = estimate(
            destination = destination("R-BULANON", fareZone = "BRGY. BULANON"),
            route = route("R-BULANON", VehicleType.BUS),
            discounted = true,
        )
        assertEquals(FareScope.Bus, trip.fare)
    }
}
