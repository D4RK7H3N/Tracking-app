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
                        )
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
