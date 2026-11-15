package com.sagay.touristnav.domain

import com.sagay.touristnav.domain.models.Route
import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.FareMatrixEntry
import com.sagay.touristnav.domain.models.FareOrigin
import com.sagay.touristnav.domain.models.FareRule
import com.sagay.touristnav.domain.models.RouteSummary
import com.sagay.touristnav.domain.models.Terminal
import com.sagay.touristnav.domain.models.TripEstimate
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
    ): TripEstimate {        // For island/reef destinations the road leg ends at the port terminal,
        // so the sanity check must compare against that endpoint, not the
        // offshore point (which would falsely trigger the warning).
        val roadEndpointLat = if (destination.requiresBoat) terminal.lat else destination.lat
        val roadEndpointLng = if (destination.requiresBoat) terminal.lng else destination.lng
        val straightLineKm = Haversine.km(originLat, originLng, roadEndpointLat, roadEndpointLng)

        val origin = FareZoneDetector.detect(originLat, originLng, fareOrigins)
        val matrixEntry = origin?.let {
            FareMatrixLookup.find(matrixEntries, it.zone, destination.fareZone.orEmpty())
        }

        val fare: Double
        val fareBasis: String
        val fareIsOfficial: Boolean
        if (matrixEntry != null) {
            fare = if (discounted) matrixEntry.discountedFare else matrixEntry.regularFare
            fareBasis = "Official fare matrix: ${origin?.name} \u2192 ${destination.fareZone}"
            fareIsOfficial = true
        } else {
            fare = FareCalculator.compute(fareRule, roadRoute.roadKm)
            fareBasis = if (origin == null) {
                "Estimated from road distance (outside the matrix areas): " +
                    "\u20B1${fareRule.baseFare.toInt()} first ${fareRule.baseKm.toInt()} km, " +
                    "+\u20B1${fareRule.extraPerKm.toInt()} per started km"
            } else {
                "Estimated from road distance (not in the ${origin.name} matrix): " +
                    "\u20B1${fareRule.baseFare.toInt()} first ${fareRule.baseKm.toInt()} km, " +
                    "+\u20B1${fareRule.extraPerKm.toInt()} per started km"
            }
            fareIsOfficial = false
        }

        return TripEstimate(
            destination = destination,
            terminal = terminal,
            roadKm = roadRoute.roadKm,
            durationMinutes = roadRoute.durationMinutes,
            fare = fare,
            routePoints = roadRoute.points,
            straightLineKm = straightLineKm,
            distanceWarning = Haversine.isRoadDistanceSuspicious(roadRoute.roadKm, straightLineKm),
            fareSource = fareRule.source,
            boatNote = if (destination.requiresBoat) {
                "Boat ride required from ${terminal.name}"
            } else {
                null
            },
            fareBasis = fareBasis,
            fareIsOfficial = fareIsOfficial,
            fareDiscounted = discounted,
            originZoneName = origin?.name,
            // The destination names the route it is served by; the route
            // carries the colour, the vehicle and the stops.
            route = routes.firstOrNull { it.id == destination.routeId },
        )
    }
}
