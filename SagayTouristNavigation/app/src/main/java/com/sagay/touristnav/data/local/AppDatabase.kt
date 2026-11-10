package com.sagay.touristnav.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sagay.touristnav.data.local.dao.DestinationDao
import com.sagay.touristnav.data.local.dao.FareRuleDao
import com.sagay.touristnav.data.local.dao.TerminalDao
import com.sagay.touristnav.data.local.dao.TripStateDao

@Database(
    entities = [
        TerminalEntity::class,
        DestinationEntity::class,
        FareRuleEntity::class,
        TripStateEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun terminalDao(): TerminalDao
    abstract fun destinationDao(): DestinationDao
    abstract fun fareRuleDao(): FareRuleDao
    abstract fun tripStateDao(): TripStateDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE fare_rule ADD COLUMN maxFare REAL")
            }
        }
    }
}
