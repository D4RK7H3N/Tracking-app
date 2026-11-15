package com.sagay.touristnav.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sagay.touristnav.data.local.RouteEntity
import com.sagay.touristnav.data.local.RouteStopEntity

@Dao
interface RouteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<RouteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<RouteStopEntity>)

    /** Clears the routes so the bundled seed is the only source of truth. */
    @Query("DELETE FROM route_stop")
    suspend fun deleteAllStops()

    @Query("DELETE FROM route")
    suspend fun deleteAllRoutes()

    @Query("SELECT * FROM route")
    suspend fun getRoutes(): List<RouteEntity>

    @Query("SELECT * FROM route_stop ORDER BY routeId, stopOrder")
    suspend fun getStops(): List<RouteStopEntity>
}
