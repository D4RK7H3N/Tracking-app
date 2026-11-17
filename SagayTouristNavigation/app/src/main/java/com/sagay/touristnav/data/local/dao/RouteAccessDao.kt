package com.sagay.touristnav.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sagay.touristnav.data.local.RouteAccessEntity

@Dao
interface RouteAccessDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(access: List<RouteAccessEntity>)

    /** Clears the access rules so the bundled seed is the only source of truth. */
    @Query("DELETE FROM route_access")
    suspend fun deleteAll()

    @Query("SELECT * FROM route_access")
    suspend fun getAll(): List<RouteAccessEntity>
}
