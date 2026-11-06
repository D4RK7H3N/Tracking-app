package com.sagay.touristnav.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "terminal")
data class TerminalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    @ColumnInfo(name = "colorHex") val colorHex: String,
    @ColumnInfo(name = "colorName") val colorName: String,
    @ColumnInfo(name = "serviceArea") val serviceArea: String,
)
