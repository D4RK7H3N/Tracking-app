package com.sagay.touristnav.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sagay.touristnav.data.local.BusRouteEntity
import com.sagay.touristnav.data.local.BusRouteStopEntity

@Dao
interface BusRouteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutes(routes: List<BusRouteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStops(stops: List<BusRouteStopEntity>)

    @Query("SELECT * FROM bus_route")
    suspend fun getRoutes(): List<BusRouteEntity>

    @Query("SELECT * FROM bus_route_stop ORDER BY routeId, stopOrder")
    suspend fun getStops(): List<BusRouteStopEntity>
}
