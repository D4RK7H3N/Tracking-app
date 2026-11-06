package com.sagay.touristnav.data.local

import android.content.Context
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the database from assets/seed.json on first launch only. Editing the
 * seed requires clearing app data (or uninstalling) to take effect.
 */
@Singleton
class SeedLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
) {

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (database.destinationDao().count() > 0) return@withContext
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
                    ),
                )
            }
        }
    }
}
