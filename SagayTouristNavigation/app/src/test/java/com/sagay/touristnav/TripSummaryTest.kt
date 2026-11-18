package com.sagay.touristnav

import com.sagay.touristnav.data.local.entities.TripRecordEntity
import com.sagay.touristnav.domain.TripSummary
import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.FareScope
import com.sagay.touristnav.domain.models.Terminal
import com.sagay.touristnav.domain.models.TripEstimate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripSummaryTest {

    @Test
    fun `group total multiplies the unit fare`() {
        assertEquals(60.0, TripSummary.total(20.0, 3), 0.001)
        assertEquals(20.0, TripSummary.total(20.0, 0), 0.001)
    }

    @Test
    fun `share text includes the group total for more than one passenger`() {
        val scope = FareScope.MotorizedTricycle(20.0, true, "matrix")
        val text = TripSummary.text(
            trip = TripEstimate(
                destination = Destination(
                    id = "D6",
                    name = "The Mall",
                    category = "Shopping",
                    lat = 10.89,
                    lng = 123.41,
                    terminalId = "T1",
                ),
                terminal = Terminal(
                    id = "T1",
                    name = "Sagay Public Market Terminal",
                    lat = 10.89,
                    lng = 123.41,
                    colorHex = "#D32F2F",
                    colorName = "Red",
                    serviceArea = "Poblacion",
                ),
                roadKm = 7.3,
                durationMinutes = 18.0,
                fare = scope,
                routePoints = emptyList(),
                straightLineKm = 7.0,
                distanceWarning = false,
            ),
            passengers = 3,
        )
        assertTrue(text.contains("Group total: \u20B160.00"))
        assertTrue(text.contains("Passengers: 3"))
    }

    @Test
    fun `csv export quotes commas and lists each record`() {
        val csv = TripSummary.csv(
            listOf(
                TripRecordEntity(
                    destinationId = "D6",
                    destinationName = "The Mall, Sagay",
                    originZone = "OLD_SAGAY",
                    vehicleLabel = "Yellow Tricycle",
                    fare = 20.0,
                    discounted = false,
                    passengers = 2,
                    total = 40.0,
                    savedAt = 1000L,
                ),
            ),
        )
        assertTrue(csv.contains("\"The Mall, Sagay\""))
        assertTrue(csv.contains("40.00"))
        assertEquals(2, csv.trimEnd().lines().size)
    }
}
