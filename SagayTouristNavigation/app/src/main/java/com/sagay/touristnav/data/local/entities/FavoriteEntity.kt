package com.sagay.touristnav.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite")
data class FavoriteEntity(
    @PrimaryKey val destinationId: String,
    val savedAt: Long,
)
