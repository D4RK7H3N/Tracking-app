package com.sagay.touristnav.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_state")
data class TripStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originLat: Double,
    val originLng: Double,
    val destinationId: String,
    val status: String,
    val savedAt: Long,
) {
    companion object {
        const val STATUS_PENDING_ROUTE = "PENDING_ROUTE"
        const val STATUS_DONE = "DONE"
    }
}
