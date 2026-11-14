package com.sagay.touristnav.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sagay.touristnav.data.local.dao.BusRouteDao
import com.sagay.touristnav.data.local.dao.DestinationDao
import com.sagay.touristnav.data.local.dao.FareMatrixDao
import com.sagay.touristnav.data.local.dao.FareOriginDao
import com.sagay.touristnav.data.local.dao.FareRuleDao
import com.sagay.touristnav.data.local.dao.TerminalDao
import com.sagay.touristnav.data.local.dao.TripStateDao

@Database(
    entities = [
        TerminalEntity::class,
        DestinationEntity::class,
        FareRuleEntity::class,
        TripStateEntity::class,
        FareMatrixEntity::class,
        FareOriginEntity::class,
        BusRouteEntity::class,
        BusRouteStopEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun terminalDao(): TerminalDao
    abstract fun destinationDao(): DestinationDao
    abstract fun fareRuleDao(): FareRuleDao
    abstract fun tripStateDao(): TripStateDao
    abstract fun fareMatrixDao(): FareMatrixDao
    abstract fun fareOriginDao(): FareOriginDao
    abstract fun busRouteDao(): BusRouteDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE fare_rule ADD COLUMN maxFare REAL")
            }
        }

        /** Adds the official fare matrix and the destination's matrix route. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `fare_matrix` (" +
                        "`originZone` TEXT NOT NULL, " +
                        "`destinationZone` TEXT NOT NULL, " +
                        "`regularFare` REAL NOT NULL, " +
                        "`discountedFare` REAL NOT NULL, " +
                        "PRIMARY KEY(`originZone`, `destinationZone`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `fare_origin` (" +
                        "`zone` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`lat` REAL NOT NULL, " +
                        "`lng` REAL NOT NULL, " +
                        "`radiusKm` REAL NOT NULL, " +
                        "PRIMARY KEY(`zone`))",
                )
                db.execSQL("ALTER TABLE `destination` ADD COLUMN `fareZone` TEXT")
            }
        }

        /**
         * Adds the vehicles boarding at each terminal. The column holds
         * comma-separated [com.sagay.touristnav.domain.models.VehicleType.id]
         * values; an empty string means "no vehicle recorded", which is the
         * state of terminals seeded before this version.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `terminal` ADD COLUMN `vehicles` TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        /** Adds bus / jeepney routes and their ordered stops. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bus_route` (" +
                        "`id` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`terminalId` TEXT NOT NULL, " +
                        "`colorHex` TEXT NOT NULL, " +
                        "PRIMARY KEY(`id`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bus_route_stop` (" +
                        "`routeId` TEXT NOT NULL, " +
                        "`stopOrder` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`lat` REAL NOT NULL, " +
                        "`lng` REAL NOT NULL, " +
                        "PRIMARY KEY(`routeId`, `stopOrder`))",
                )
            }
        }
    }
}
