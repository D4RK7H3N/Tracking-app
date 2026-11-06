package com.sagay.touristnav.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sagay.touristnav.data.local.DestinationEntity

@Dao
interface DestinationDao {

    @Insert
    suspend fun insertAll(destinations: List<DestinationEntity>)

    @Query("SELECT * FROM destination ORDER BY name")
    suspend fun getAll(): List<DestinationEntity>

    @Query("SELECT * FROM destination WHERE id = :id")
    suspend fun getById(id: String): DestinationEntity?

    @Query("SELECT COUNT(*) FROM destination")
    suspend fun count(): Int
}
