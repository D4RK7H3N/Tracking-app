package com.sagay.touristnav.di

import android.content.Context
import androidx.room.Room
import com.sagay.touristnav.data.local.AppDatabase
import com.sagay.touristnav.data.local.dao.DestinationDao
import com.sagay.touristnav.data.local.dao.FareRuleDao
import com.sagay.touristnav.data.local.dao.TerminalDao
import com.sagay.touristnav.data.local.dao.TripStateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "sagay_tourist_nav.db")
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTerminalDao(db: AppDatabase): TerminalDao = db.terminalDao()

    @Provides
    fun provideDestinationDao(db: AppDatabase): DestinationDao = db.destinationDao()

    @Provides
    fun provideFareRuleDao(db: AppDatabase): FareRuleDao = db.fareRuleDao()

    @Provides
    fun provideTripStateDao(db: AppDatabase): TripStateDao = db.tripStateDao()
}
