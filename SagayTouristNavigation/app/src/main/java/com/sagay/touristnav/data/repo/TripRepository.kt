package com.sagay.touristnav.data.repo

import com.sagay.touristnav.data.local.dao.RouteDao
import com.sagay.touristnav.data.local.dao.RouteAccessDao
import com.sagay.touristnav.data.local.dao.DestinationDao
import com.sagay.touristnav.data.local.dao.FareMatrixDao
import com.sagay.touristnav.data.local.dao.FareOriginDao
import com.sagay.touristnav.data.local.dao.FareRuleDao
import com.sagay.touristnav.data.local.dao.TerminalDao
import com.sagay.touristnav.data.local.dao.TripStateDao
import com.sagay.touristnav.data.local.dao.TripRecordDao
import com.sagay.touristnav.data.local.dao.FavoriteDao
import com.sagay.touristnav.data.local.entities.TripRecordEntity
import com.sagay.touristnav.data.local.entities.FavoriteEntity
import com.sagay.touristnav.data.local.toDomain
import com.sagay.touristnav.data.local.TripStateEntity
import com.sagay.touristnav.data.remote.DirectionsApi
import com.sagay.touristnav.data.remote.RouteException
import com.sagay.touristnav.domain.Haversine
import com.sagay.touristnav.domain.TripEstimator
import com.sagay.touristnav.domain.models.Route
import com.sagay.touristnav.domain.models.RouteAccess
import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.FareMatrixEntry
import com.sagay.touristnav.domain.models.FareOrigin
import com.sagay.touristnav.domain.models.FareRule
import com.sagay.touristnav.domain.models.FareScope
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
    private val fareMatrixDao: FareMatrixDao,
    private val fareOriginDao: FareOriginDao,
    private val routeDao: RouteDao,
    private val routeAccessDao: RouteAccessDao,
    private val tripRecordDao: TripRecordDao,
    private val favoriteDao: FavoriteDao,
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
                maxFare = 40.0,
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
        route: RouteSummary? = null,
        discounted: Boolean = false,
    ): TripEstimate {
        val resolvedRoute = route ?: fetchRoute(
            origin = origin,
            targetLat = if (destination.requiresBoat) terminal.lat else destination.lat,
            targetLng = if (destination.requiresBoat) terminal.lng else destination.lng,
        )
        val rule = fareRule()
        return estimator.estimate(
            originLat = origin.lat,
            originLng = origin.lng,
            destination = destination,
            terminal = terminal,
            fareRule = rule,
            roadRoute = resolvedRoute,
            matrixEntries = fareMatrix(),
            fareOrigins = fareOrigins(),
            routes = routes(),
            routeAccess = routeAccess(),
            discounted = discounted,
        )
    }

    suspend fun fareMatrix(): List<FareMatrixEntry> =
        fareMatrixDao.getAll().map { it.toDomain() }

    suspend fun fareOrigins(): List<FareOrigin> =
        fareOriginDao.getAll().map { it.toDomain() }

    /** The owner-supplied rules for reaching a service from another area. */
    suspend fun routeAccess(): List<RouteAccess> =
        routeAccessDao.getAll().map { it.toDomain() }

    /** Transport routes with their stops, in travel order. */
    suspend fun routes(): List<Route> {
        val stops = routeDao.getStops()
        return routeDao.getRoutes().map { it.toDomain(stops) }
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

    val tripHistory: Flow<List<TripRecordEntity>> = tripRecordDao.all()
    val favoriteIds: Flow<List<String>> = favoriteDao.allIds()

    suspend fun saveTrip(trip: TripEstimate, passengers: Int) {
        val amount = when (val scope = trip.fare) {
            is FareScope.MotorizedTricycle -> scope.amount
            is FareScope.Island -> scope.tricycleToPort.amount
            else -> 0.0
        }
        tripRecordDao.insert(
            TripRecordEntity(
                destinationId = trip.destination.id,
                destinationName = trip.destination.name,
                originZone = trip.originZoneName.orEmpty(),
                vehicleLabel = trip.route?.serviceLabel.orEmpty(),
                fare = amount,
                discounted = trip.fareDiscounted,
                passengers = passengers.coerceAtLeast(1),
                total = amount * passengers.coerceAtLeast(1),
                savedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun deleteTrip(id: Long) = tripRecordDao.delete(id)

    suspend fun clearHistory() = tripRecordDao.clear()

    suspend fun toggleFavorite(destinationId: String, favorite: Boolean) {
        if (favorite) {
            favoriteDao.add(FavoriteEntity(destinationId, System.currentTimeMillis()))
        } else {
            favoriteDao.remove(destinationId)
        }
    }
}
