package com.sagay.touristnav.domain

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
        route: RouteSummary,
        matrixEntries: List<FareMatrixEntry> = emptyList(),
        fareOrigins: List<FareOrigin> = emptyList(),
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

        val fare: Double
        val fareBasis: String
        val fareIsOfficial: Boolean
        if (matrixEntry != null) {
            fare = if (discounted) matrixEntry.discountedFare else matrixEntry.regularFare
            fareBasis = "Official fare matrix: ${origin?.name} → ${destination.fareZone}"
            fareIsOfficial = true
        } else {
            fare = FareCalculator.compute(fareRule, route.roadKm)
            fareBasis = if (origin == null) {
                "Estimated from road distance (outside the matrix areas): " +
                    "₱${fareRule.baseFare.toInt()} first ${fareRule.baseKm.toInt()} km, " +
                    "+₱${fareRule.extraPerKm.toInt()} per started km"
            } else {
                "Estimated from road distance (not in the ${origin.name} matrix): " +
                    "₱${fareRule.baseFare.toInt()} first ${fareRule.baseKm.toInt()} km, " +
                    "+₱${fareRule.extraPerKm.toInt()} per started km"
            }
            fareIsOfficial = false
        }

        return TripEstimate(
            destination = destination,
            terminal = terminal,
            roadKm = route.roadKm,
            durationMinutes = route.durationMinutes,
            fare = fare,
            routePoints = route.points,
            straightLineKm = straightLineKm,
            distanceWarning = Haversine.isRoadDistanceSuspicious(route.roadKm, straightLineKm),
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
        )
    }
}
