package com.sagay.touristnav.data.local

import android.content.Context
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Refreshes bundled static data once per process without touching trip state. */
@Singleton
class SeedLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
) {
    private val seedMutex = Mutex()
    private var seeded = false

    suspend fun seed() = withContext(Dispatchers.IO) {
        seedMutex.withLock {
            if (seeded) return@withLock
            val raw = context.assets.open("seed.json").bufferedReader().use { it.readText() }
            val seed = SeedParser.parse(raw)
            database.withTransaction {
                database.terminalDao().insertAll(
                    seed.terminals.map {
                        TerminalEntity(
                            id = it.id,
                            name = it.name,
                            lat = it.lat,
                            lng = it.lng,
                            colorHex = it.colorHex,
                            colorName = it.colorName,
                            serviceArea = it.serviceArea,
                            vehicles = it.vehicles.joinToString(","),
                        )
                    },
                )
                database.destinationDao().insertAll(
                    seed.destinations.map {
                        DestinationEntity(
                            id = it.id,
                            name = it.name,
                            category = it.category,
                            lat = it.lat,
                            lng = it.lng,
                            terminalId = it.terminalId,
                            requiresBoat = it.boat,
                            fareZone = it.zone,
                            routeId = it.routeId,
                        )
                    },
                )
                database.fareOriginDao().insertAll(
                    seed.fareOrigins.map {
                        FareOriginEntity(
                            zone = it.zone,
                            name = it.name,
                            lat = it.lat,
                            lng = it.lng,
                            radiusKm = it.radiusKm,
                        )
                    },
                )
                database.fareMatrixDao().insertAll(
                    seed.fareMatrix.map {
                        FareMatrixEntity(
                            originZone = it.origin,
                            destinationZone = it.destination,
                            regularFare = it.regular,
                            discountedFare = it.discounted,
                        )
                    },
                )
                database.routeDao().deleteAllStops()
                database.routeDao().deleteAllRoutes()
                database.routeAccessDao().deleteAll()
                database.routeAccessDao().insertAll(
                    seed.routeAccess.map {
                        RouteAccessEntity(
                            originZone = it.originZone,
                            destinationRouteId = it.destinationRouteId,
                            viaRouteId = it.viaRouteId,
                            changeAt = it.changeAt,
                            fareZone = it.fareZone,
                        )
                    },
                )
                database.routeDao().insertRoutes(
                    seed.routes.map {
                        RouteEntity(
                            id = it.id,
                            name = it.name,
                            terminalId = it.terminalId,
                            colorHex = it.colorHex,
                            colorName = it.colorName,
                            vehicle = it.vehicle,
                        )
                    },
                )
                database.routeDao().insertStops(
                    seed.routes.flatMap { route ->
                        route.stops.mapIndexed { index, stop ->
                            RouteStopEntity(
                                routeId = route.id,
                                stopOrder = index,
                                name = stop.name,
                                lat = stop.lat,
                                lng = stop.lng,
                            )
                        }
                    },
                )
                seed.fareRule?.let {
                    database.fareRuleDao().insert(
                        FareRuleEntity(
                            id = it.id,
                            baseFare = it.baseFare,
                            baseKm = it.baseKm,
                            extraPerKm = it.extraPerKm,
                            source = it.source,
                            maxFare = it.maxFare,
                        ),
                    )
                }
            }
            seeded = true
        }
    }
}
