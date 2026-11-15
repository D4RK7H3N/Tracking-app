package com.sagay.touristnav.domain

import com.sagay.touristnav.domain.models.Route
import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.FareMatrixEntry
import com.sagay.touristnav.domain.models.FareOrigin
import com.sagay.touristnav.domain.models.FareRule
import com.sagay.touristnav.domain.models.FareScope
import com.sagay.touristnav.domain.models.RouteSummary
import com.sagay.touristnav.domain.models.Terminal
import com.sagay.touristnav.domain.models.TripEstimate
import com.sagay.touristnav.domain.models.VehicleType
import javax.inject.Inject

class TripEstimator @Inject constructor() {

    fun estimate(
        originLat: Double,
        originLng: Double,
        destination: Destination,
        terminal: Terminal,
        fareRule: FareRule,
        roadRoute: RouteSummary,
        matrixEntries: List<FareMatrixEntry> = emptyList(),
        fareOrigins: List<FareOrigin> = emptyList(),
        routes: List<Route> = emptyList(),
        discounted: Boolean = false,
    ): TripEstimate {
        // For island/reef destinations the road leg ends at the port terminal,
        // so the sanity check must compare against that endpoint, not the
        // offshore point (which would falsely trigger the warning).
        val roadEndpointLat = if (destination.requiresBoat) terminal.lat else destination.lat
        val roadEndpointLng = if (destination.requiresBoat) terminal.lng else destination.lng
        val straightLineKm = Haversine.km(originLat, originLng, roadEndpointLat, roadEndpointLng)

        val origin = FareZoneDetector.detect(originLat, originLng, fareOrigins)
        val matrixEntry = origin?.let {
            FareMatrixLookup.find(matrixEntries, it.zone, destination.fareZone.orEmpty())
        }

        // The route decides which vehicle carries the passenger, and the
        // vehicle decides which fare rules apply: the published matrix covers
        // motorized tricycles only.
        val route = routes.firstOrNull { it.id == destination.routeId }
        val fare = fareScope(
            vehicle = route?.vehicle,
            requiresBoat = destination.requiresBoat,
            origin = origin,
            matrixEntry = matrixEntry,
            fareRule = fareRule,
            roadKm = roadRoute.roadKm,
            destinationZone = destination.fareZone,
            discounted = discounted,
        )

        return TripEstimate(
            destination = destination,
            terminal = terminal,
            roadKm = roadRoute.roadKm,
            durationMinutes = roadRoute.durationMinutes,
            fare = fare,
            routePoints = roadRoute.points,
            straightLineKm = straightLineKm,
            distanceWarning = Haversine.isRoadDistanceSuspicious(roadRoute.roadKm, straightLineKm),
            boatNote = if (destination.requiresBoat) {
                "Boat ride required from ${terminal.name}"
            } else {
                null
            },
            fareDiscounted = discounted,
            originZoneName = origin?.name,
            // The destination names the route it is served by; the route
            // carries the colour, the vehicle and the stops.
            route = route,
        )
    }

    /**
     * The fare of the service being described.
     *
     * The published matrix prices motorized tricycles, so it is only applied
     * to tricycle and Reporm 2 rides. A bus ride keeps its road distance and
     * time but carries no fare, and an island trip prices only the tricycle
     * leg to the port: the boat fare is not published and is never folded
     * into the land amount.
     */
    private fun fareScope(
        vehicle: VehicleType?,
        requiresBoat: Boolean,
        origin: FareOrigin?,
        matrixEntry: FareMatrixEntry?,
        fareRule: FareRule,
        roadKm: Double,
        destinationZone: String?,
        discounted: Boolean,
    ): FareScope {
        // An island trip is a tricycle ride to the port plus a boat leg, so
        // the land leg is priced like any other tricycle ride.
        val landVehicle = if (requiresBoat) VehicleType.TRICYCLE else vehicle
        if (landVehicle == null) {
            return FareScope.Unverified
        }
        if (landVehicle == VehicleType.BUS) {
            return FareScope.Bus
        }
        if (landVehicle == VehicleType.BOAT) {
            // A boat route that is not an island destination has no priced
            // land leg to show.
            return FareScope.Unverified
        }

        val tricycle = tricycleFare(
            origin = origin,
            matrixEntry = matrixEntry,
            fareRule = fareRule,
            roadKm = roadKm,
            destinationZone = destinationZone,
            discounted = discounted,
        )
        return if (requiresBoat) FareScope.Island(tricycle) else tricycle
    }

    /**
     * The tricycle fare: the official matrix when the origin and destination
     * are in the same table, otherwise the distance estimate.
     *
     * The 20% discount is only published for the matrix fares, so the
     * distance estimate keeps its single value.
     */
    private fun tricycleFare(
        origin: FareOrigin?,
        matrixEntry: FareMatrixEntry?,
        fareRule: FareRule,
        roadKm: Double,
        destinationZone: String?,
        discounted: Boolean,
    ): FareScope.MotorizedTricycle {
        if (matrixEntry != null) {
            return FareScope.MotorizedTricycle(
                amount = if (discounted) matrixEntry.discountedFare else matrixEntry.regularFare,
                isOfficial = true,
                basis = "Official fare matrix: ${origin?.name} \u2192 ${destinationZone}",
            )
        }
        val estimate = FareCalculator.compute(fareRule, roadKm)
        return FareScope.MotorizedTricycle(
            amount = estimate,
            isOfficial = false,
            basis = if (origin == null) {
                "Estimated from road distance (outside the matrix areas): " +
                    "\u20B1${fareRule.baseFare.toInt()} first ${fareRule.baseKm.toInt()} km, " +
                    "+\u20B1${fareRule.extraPerKm.toInt()} per started km"
            } else {
                "Estimated from road distance (not in the ${origin.name} matrix): " +
                    "\u20B1${fareRule.baseFare.toInt()} first ${fareRule.baseKm.toInt()} km, " +
                    "+\u20B1${fareRule.extraPerKm.toInt()} per started km"
            },
        )
    }
}
