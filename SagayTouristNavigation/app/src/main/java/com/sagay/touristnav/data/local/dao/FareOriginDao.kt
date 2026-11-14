package com.sagay.touristnav.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sagay.touristnav.data.local.FareOriginEntity

@Dao
interface FareOriginDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(origins: List<FareOriginEntity>)

    @Query("SELECT * FROM fare_origin")
    suspend fun getAll(): List<FareOriginEntity>
}
