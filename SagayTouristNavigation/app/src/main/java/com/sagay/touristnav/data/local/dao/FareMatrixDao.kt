package com.sagay.touristnav.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sagay.touristnav.data.local.FareMatrixEntity

@Dao
interface FareMatrixDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<FareMatrixEntity>)

    @Query("SELECT * FROM fare_matrix")
    suspend fun getAll(): List<FareMatrixEntity>

    @Query("SELECT * FROM fare_matrix WHERE originZone = :origin")
    suspend fun getForOrigin(origin: String): List<FareMatrixEntity>
}
