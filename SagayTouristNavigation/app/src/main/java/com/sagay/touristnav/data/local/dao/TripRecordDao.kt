package com.sagay.touristnav.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sagay.touristnav.data.local.entities.TripRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripRecordDao {
    @Query("SELECT * FROM trip_record ORDER BY savedAt DESC")
    fun all(): Flow<List<TripRecordEntity>>

    @Insert
    suspend fun insert(record: TripRecordEntity): Long

    @Query("DELETE FROM trip_record WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM trip_record")
    suspend fun clear()
}
