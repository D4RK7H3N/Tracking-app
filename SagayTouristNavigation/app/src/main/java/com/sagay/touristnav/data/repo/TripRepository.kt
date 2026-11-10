package com.sagay.touristnav.data.repo

import com.sagay.touristnav.data.local.dao.DestinationDao
import com.sagay.touristnav.data.local.dao.FareRuleDao
import com.sagay.touristnav.data.local.dao.TerminalDao
import com.sagay.touristnav.data.local.dao.TripStateDao
import com.sagay.touristnav.data.local.toDomain
import com.sagay.touristnav.data.local.TripStateEntity
import com.sagay.touristnav.data.remote.DirectionsApi
import com.sagay.touristnav.data.remote.RouteException
import com.sagay.touristnav.domain.Haversine
import com.sagay.touristnav.domain.TripEstimator
import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.FareRule
import com.sagay.touristnav.domain.models.RoutePoint
import com.sagay.touristnav.domain.models.RouteSummary
import com.sagay.touristnav.domain.models.Terminal
import com.sagay.touristnav.domain.models.TripEstimate
import com.sagay.touristnav.platform.ConnectivityObserver
import com.sagay.touristnav.platform.LocationFix
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TripRepository @Inject constructor(
    private val api: DirectionsApi,
    private val terminalDao: TerminalDao,
    private val destinationDao: DestinationDao,
    private val fareRuleDao: FareRuleDao,
    private val tripStateDao: TripStateDao,
    private val estimator: TripEstimator,
    connectivityObserver: ConnectivityObserver,
) {

    val isOnline: Flow<Boolean> = connectivityObserver.isOnline

    suspend fun destinations(): List<Destination> =
        destinationDao.getAll().map { it.toDomain() }

    suspend fun destinationById(id: String): Destination? =
        destinationDao.getById(id)?.toDomain()

    suspend fun terminalById(id: String): Terminal? =
        terminalDao.getById(id)?.toDomain()

    suspend fun terminals(): List<Terminal> =
        terminalDao.getAll().map { it.toDomain() }

    suspend fun fareRule(): FareRule =
        fareRuleDao.getFirst()?.toDomain()
            ?: FareRule(
                id = 0,
                baseFare = 15.0,
                baseKm = 3.0,
                extraPerKm = 2.0,
                source = "fallback",
                maxFare = 20.0,
            )

    /**
     * Road route to a land endpoint. Island/reef destinations route to the
     * serving port terminal (the caller passes those coordinates), never to
     * the offshore point, because the driving profile cannot cross water.
     */
    suspend fun fetchRoute(
        origin: LocationFix,
        targetLat: Double,
        targetLng: Double,
    ): RouteSummary {
        val coordinates = "${origin.lng},${origin.lat};$targetLng,$targetLat"
        val response = api.drivingRoute(coordinates)
        if (response.code != "Ok") {
            throw RouteException("Directions API returned code ${response.code}")
        }
        val route = response.routes.firstOrNull()
            ?: throw RouteException("Directions API returned no route")
        return RouteSummary(
            roadKm = route.distance / 1000.0,
            durationMinutes = route.duration / 60.0,
            points = route.geometry.coordinates.mapNotNull { c ->
                if (c.size >= 2) RoutePoint(lat = c[1], lng = c[0]) else null
            },
        )
    }

    suspend fun estimateTrip(
        origin: LocationFix,
        destination: Destination,
        terminal: Terminal,
    ): TripEstimate {
        val roadLat = if (destination.requiresBoat) terminal.lat else destination.lat
        val roadLng = if (destination.requiresBoat) terminal.lng else destination.lng
        val route = fetchRoute(origin, roadLat, roadLng)
        val rule = fareRule()
        return estimator.estimate(
            originLat = origin.lat,
            originLng = origin.lng,
            destination = destination,
            terminal = terminal,
            fareRule = rule,
            route = route,
        )
    }

    suspend fun savePendingTrip(
        origin: LocationFix,
        destinationId: String,
        tripId: Long? = null,
    ): Long = tripStateDao.insert(
        TripStateEntity(
            id = tripId ?: 0,
            originLat = origin.lat,
            originLng = origin.lng,
            destinationId = destinationId,
            status = TripStateEntity.STATUS_PENDING_ROUTE,
            savedAt = System.currentTimeMillis(),
        ),
    )

    suspend fun pendingTrip(): TripStateEntity? = tripStateDao.getLatestPending()

    suspend fun markTripDone(id: Long) =
        tripStateDao.updateStatus(id, TripStateEntity.STATUS_DONE)
}
