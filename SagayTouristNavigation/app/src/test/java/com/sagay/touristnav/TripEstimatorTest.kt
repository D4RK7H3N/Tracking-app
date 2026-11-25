package com.sagay.touristnav

import com.sagay.touristnav.domain.TripEstimator
import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.FareMatrixEntry
import com.sagay.touristnav.domain.models.FareOrigin
import com.sagay.touristnav.domain.models.FareRule
import com.sagay.touristnav.domain.models.FareScope
import com.sagay.touristnav.domain.models.Route
import com.sagay.touristnav.domain.models.RouteAccess
import com.sagay.touristnav.domain.models.RouteSummary
import com.sagay.touristnav.domain.models.Terminal
import com.sagay.touristnav.domain.models.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The fare must describe the vehicle that actually carries the passenger.
 *
 * The published matrix prices motorized tricycles, so it covers the Reporm 2
 * service as well, and an island trip prices only the tricycle leg to the
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
        FareOrigin("OLD_SAGAY", "Brgy. Old Sagay", 10.94691, 123.426, 2.5),
    )

    private val matrix = listOf(
        FareMatrixEntry("CITY_PROPER", "BRGY. VITO", 40.0, 32.0),
        FareMatrixEntry("CITY_PROPER", "BRGY. BULANON", 30.0, 24.0),
        FareMatrixEntry("OLD_SAGAY", "SAGAY CITY", 20.0, 16.0),
    )

    /** The owner-supplied rule for the Old Sagay to Sagay City corridor. */
    private val access = listOf(
        RouteAccess(
            originZone = "OLD_SAGAY",
            destinationRouteId = "R-POPLACION",
            viaRouteId = "R-OLD-SAGAY",
            changeAt = "Sagay City Bus Terminal (Poblacion)",
            fareZone = "SAGAY CITY",
        ),
    )

    private val terminal = Terminal(
        id = "T3",
        name = "Old Sagay Port Terminal",
        lat = 10.9469,
        lng = 123.426,
        colorHex = "#F9A825",
        colorName = "Yellow",
        serviceArea = "Old Sagay",
        vehicles = listOf(VehicleType.TRICYCLE, VehicleType.REPORM_2, VehicleType.BOAT),
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
        originLat: Double = 10.8890,
        originLng: Double = 123.4140,
        routes: List<Route> = listOfNotNull(route),
        routeAccess: List<RouteAccess> = emptyList(),
    ) = estimator.estimate(
        originLat = originLat,
        originLng = originLng,
        destination = destination,
        terminal = terminal,
        fareRule = rule,
        roadRoute = road,
        matrixEntries = matrix,
        fareOrigins = origins,
        routes = routes,
        routeAccess = routeAccess,
        discounted = discounted,
    )

    /** Standing in Old Sagay, where the fare origin zone is OLD_SAGAY. */
    private fun estimateFromOldSagay(
        destination: Destination,
        route: Route?,
        discounted: Boolean = false,
        routes: List<Route> = listOfNotNull(route),
        routeAccess: List<RouteAccess> = access,
    ) = estimate(
        destination = destination,
        route = route,
        discounted = discounted,
        originLat = 10.94691,
        originLng = 123.426,
        routes = routes,
        routeAccess = routeAccess,
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
    fun `a reporm 2 ride takes the official matrix fare`() {
        val trip = estimate(
            destination = destination("R-BULANON", fareZone = "BRGY. BULANON"),
            route = route("R-BULANON", VehicleType.REPORM_2),
        )
        val fare = trip.fare as FareScope.MotorizedTricycle
        assertEquals(30.0, fare.amount, 0.001)
        assertTrue(fare.isOfficial)
        assertTrue(fare.basis.contains("City Proper (Poblacion)"))
    }

    @Test
    fun `a reporm 2 ride keeps its road distance and time`() {
        val trip = estimate(
            destination = destination("R-BULANON", fareZone = "BRGY. BULANON"),
            route = route("R-BULANON", VehicleType.REPORM_2),
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
    fun `the 20 percent discount applies to a reporm 2 fare`() {
        val trip = estimate(
            destination = destination("R-BULANON", fareZone = "BRGY. BULANON"),
            route = route("R-BULANON", VehicleType.REPORM_2),
            discounted = true,
        )
        val fare = trip.fare as FareScope.MotorizedTricycle
        assertEquals(24.0, fare.amount, 0.001)
        assertTrue(trip.fareDiscounted)
        assertTrue(fare.canDiscount)
    }

    @Test
    fun `the discount is only offered for a published matrix fare`() {
        val matrixTrip = estimate(
            destination = destination("R-VITO", fareZone = "BRGY. VITO"),
            route = route("R-VITO", VehicleType.REPORM_2),
        )
        assertTrue(matrixTrip.fare.canDiscount)

        val estimateTrip = estimate(
            destination = destination("R-POPLACION"),
            route = route("R-POPLACION", VehicleType.TRICYCLE),
        )
        assertTrue(!estimateTrip.fare.canDiscount)

        val repormTrip = estimate(
            destination = destination("R-BULANON", fareZone = "BRGY. BULANON"),
            route = route("R-BULANON", VehicleType.REPORM_2),
        )
        assertTrue(repormTrip.fare.canDiscount)
    }

    @Test
    fun `an island trip whose land leg is an estimate offers no discount`() {
        val trip = estimate(
            destination = destination(
                routeId = "R-WATER-OLD-SAGAY",
                fareZone = null,
                requiresBoat = true,
            ),
            route = route("R-WATER-OLD-SAGAY", VehicleType.BOAT),
        )
        assertTrue(!trip.fare.canDiscount)
    }

    @Test
    fun `Old Sagay to Sagay City needs the yellow tricycle and then the red one`() {
        val yellow = route("R-OLD-SAGAY", VehicleType.TRICYCLE)
        val red = route("R-POPLACION", VehicleType.TRICYCLE)
        val trip = estimateFromOldSagay(
            destination = destination("R-POPLACION", fareZone = "RICKY"),
            route = red,
            routes = listOf(yellow, red),
        )
        assertEquals(listOf("R-OLD-SAGAY", "R-POPLACION"), trip.services.map { it.id })
        assertEquals("Sagay City Bus Terminal (Poblacion)", trip.changeAt)
        assertEquals("R-POPLACION", trip.route?.id)
    }

    @Test
    fun `the Old Sagay to Sagay City corridor is priced as one fare`() {
        val yellow = route("R-OLD-SAGAY", VehicleType.TRICYCLE)
        val red = route("R-POPLACION", VehicleType.TRICYCLE)
        val trip = estimateFromOldSagay(
            destination = destination("R-POPLACION", fareZone = "RICKY"),
            route = red,
            routes = listOf(yellow, red),
        )
        val fare = trip.fare as FareScope.MotorizedTricycle
        assertEquals(20.0, fare.amount, 0.001)
        assertTrue(fare.isOfficial)
        assertTrue(fare.basis.contains("SAGAY CITY"))
    }

    @Test
    fun `the corridor fare is discounted for students and senior citizens`() {
        val yellow = route("R-OLD-SAGAY", VehicleType.TRICYCLE)
        val red = route("R-POPLACION", VehicleType.TRICYCLE)
        val trip = estimateFromOldSagay(
            destination = destination("R-POPLACION", fareZone = "RICKY"),
            route = red,
            discounted = true,
            routes = listOf(yellow, red),
        )
        val fare = trip.fare as FareScope.MotorizedTricycle
        assertEquals(16.0, fare.amount, 0.001)
        assertTrue(trip.fareDiscounted)
    }

    @Test
    fun `the corridor rule does not apply outside Old Sagay`() {
        val yellow = route("R-OLD-SAGAY", VehicleType.TRICYCLE)
        val red = route("R-POPLACION", VehicleType.TRICYCLE)
        val trip = estimate(
            destination = destination("R-POPLACION", fareZone = "RICKY"),
            route = red,
            routes = listOf(yellow, red),
            routeAccess = access,
        )
        assertEquals(listOf("R-POPLACION"), trip.services.map { it.id })
        assertEquals(null, trip.changeAt)
    }

    @Test
    fun `a corridor rule for a destination served by the same route needs no change`() {
        val yellow = route("R-OLD-SAGAY", VehicleType.TRICYCLE)
        val trip = estimateFromOldSagay(
            destination = destination("R-OLD-SAGAY", fareZone = "BRGY. OLD SAGAY"),
            route = yellow,
            routes = listOf(yellow),
        )
        assertEquals(listOf("R-OLD-SAGAY"), trip.services.map { it.id })
        assertEquals(null, trip.changeAt)
    }

    @Test
    fun `an access rule naming an unknown route is ignored`() {
        val red = route("R-POPLACION", VehicleType.TRICYCLE)
        val stale = access.map { it.copy(viaRouteId = "R-NOT-IN-THE-SEED") }
        val trip = estimateFromOldSagay(
            destination = destination("R-POPLACION", fareZone = "RICKY"),
            route = red,
            routes = listOf(red),
            routeAccess = stale,
        )
        assertEquals(listOf("R-POPLACION"), trip.services.map { it.id })
        assertEquals(null, trip.changeAt)
    }
}
