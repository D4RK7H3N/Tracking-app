package com.sagay.touristnav.domain

import com.sagay.touristnav.data.local.entities.TripRecordEntity
import com.sagay.touristnav.domain.models.FareScope
import com.sagay.touristnav.domain.models.TripEstimate

object TripSummary {

    fun unitAmount(scope: FareScope): Double = when (scope) {
        is FareScope.MotorizedTricycle -> scope.amount
        is FareScope.Island -> scope.tricycleToPort.amount
        FareScope.Bus, FareScope.Unverified -> 0.0
    }

    fun total(amount: Double, passengers: Int): Double =
        amount * passengers.coerceAtLeast(1)

    fun text(trip: TripEstimate, passengers: Int): String {
        val amount = unitAmount(trip.fare)
        val count = passengers.coerceAtLeast(1)
        return buildList {
            add("Sagay Tourist Navigator")
            add("Destination: ${trip.destination.name}")
            add("Distance: ${String.format("%.1f", trip.roadKm)} km")
            add("Service: ${trip.route?.serviceLabel ?: "not published"}")
            if (trip.changeAt != null) add("Change at: ${trip.changeAt}")
            add("Fare per passenger: \u20B1${String.format("%.2f", amount)}")
            if (count > 1) {
                add("Passengers: $count")
                add("Group total: \u20B1${String.format("%.2f", total(amount, count))}")
            }
            add("Estimate only. Actual fares may vary.")
        }.joinToString("\n")
    }

    fun csv(records: List<TripRecordEntity>): String = buildString {
        append("date,destination,origin,vehicle,fare,discounted,passengers,total\n")
        records.forEach { record ->
            append(listOf(
                record.savedAt.toString(),
                record.destinationName,
                record.originZone,
                record.vehicleLabel,
                String.format("%.2f", record.fare),
                record.discounted.toString(),
                record.passengers.toString(),
                String.format("%.2f", record.total),
            ).joinToString(",") { escape(it) }).append('\n')
        }
    }

    private fun escape(value: String): String = if (
        value.contains(',') || value.contains('"') || value.contains('\n')
    ) {
        "\"${value.replace("\"", "\"\"")}\""
    } else {
        value
    }
}
