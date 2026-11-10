package com.sagay.touristnav.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sagay.touristnav.data.local.TerminalEntity

@Dao
interface TerminalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(terminals: List<TerminalEntity>)

    @Query("SELECT * FROM terminal ORDER BY name")
    suspend fun getAll(): List<TerminalEntity>

    @Query("SELECT * FROM terminal WHERE id = :id")
    suspend fun getById(id: String): TerminalEntity?

    @Query("SELECT COUNT(*) FROM terminal")
    suspend fun count(): Int
}
