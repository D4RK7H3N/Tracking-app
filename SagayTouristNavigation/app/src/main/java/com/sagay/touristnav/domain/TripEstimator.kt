package com.sagay.touristnav.domain

import com.sagay.touristnav.domain.models.Destination
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
    ): TripEstimate {
        // For island/reef destinations the road leg ends at the port terminal,
        // so the sanity check must compare against that endpoint, not the
        // offshore point (which would falsely trigger the warning).
        val roadEndpointLat = if (destination.requiresBoat) terminal.lat else destination.lat
        val roadEndpointLng = if (destination.requiresBoat) terminal.lng else destination.lng
        val straightLineKm = Haversine.km(originLat, originLng, roadEndpointLat, roadEndpointLng)
        return TripEstimate(
            destination = destination,
            terminal = terminal,
            roadKm = route.roadKm,
            durationMinutes = route.durationMinutes,
            fare = FareCalculator.compute(fareRule, route.roadKm),
            routePoints = route.points,
            straightLineKm = straightLineKm,
            distanceWarning = Haversine.isRoadDistanceSuspicious(route.roadKm, straightLineKm),
            fareSource = fareRule.source,
            boatNote = if (destination.requiresBoat) {
                "Boat ride required from ${terminal.name}"
            } else {
                null
            },
        )
    }
}
