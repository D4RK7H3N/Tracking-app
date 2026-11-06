package com.sagay.touristnav.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
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
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun terminalDao(): TerminalDao
    abstract fun destinationDao(): DestinationDao
    abstract fun fareRuleDao(): FareRuleDao
    abstract fun tripStateDao(): TripStateDao
}
