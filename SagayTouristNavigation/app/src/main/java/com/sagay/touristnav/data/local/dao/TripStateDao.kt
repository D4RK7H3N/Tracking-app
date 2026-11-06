package com.sagay.touristnav.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sagay.touristnav.data.local.TripStateEntity

@Dao
interface TripStateDao {

    @Insert
    suspend fun insert(state: TripStateEntity): Long

    @Query(
        "SELECT * FROM trip_state WHERE status = 'PENDING_ROUTE' " +
            "ORDER BY savedAt DESC LIMIT 1",
    )
    suspend fun getLatestPending(): TripStateEntity?

    @Query("UPDATE trip_state SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
}
