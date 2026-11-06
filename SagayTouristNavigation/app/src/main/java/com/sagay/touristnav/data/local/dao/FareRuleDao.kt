package com.sagay.touristnav.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sagay.touristnav.data.local.FareRuleEntity

@Dao
interface FareRuleDao {

    @Insert
    suspend fun insert(rule: FareRuleEntity)

    @Query("SELECT * FROM fare_rule ORDER BY id LIMIT 1")
    suspend fun getFirst(): FareRuleEntity?

    @Query("SELECT COUNT(*) FROM fare_rule")
    suspend fun count(): Int
}
