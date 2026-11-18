package com.sagay.touristnav

import com.sagay.touristnav.domain.JourneyGuidance
import com.sagay.touristnav.domain.models.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyGuidanceTest {

    private val points = listOf(
        RoutePoint(10.900, 123.410),
        RoutePoint(10.910, 123.420),
        RoutePoint(10.920, 123.430),
        RoutePoint(10.930, 123.440),
    )

    @Test
    fun `starting point has no progress and prompts the change`() {
        val snapshot = JourneyGuidance.snapshot(
            points = points,
            currentLat = 10.900,
            currentLng = 123.410,
            destinationName = "The Mall",
            changeAtName = "Sagay City Bus Terminal (Poblacion)",
            changeAtLat = 10.910,
            changeAtLng = 123.420,
        )
        assertEquals(0f, snapshot.progress, 0.01f)
        assertTrue(snapshot.remainingKm > 3.0)
        assertEquals("Change at Sagay City Bus Terminal (Poblacion)", snapshot.nextPrompt)
        assertFalse(snapshot.arrived)
    }

    @Test
    fun `past the change point prompts the destination`() {
        val snapshot = JourneyGuidance.snapshot(
            points = points,
            currentLat = 10.925,
            currentLng = 123.435,
            destinationName = "The Mall",
            changeAtName = "Sagay City Bus Terminal (Poblacion)",
            changeAtLat = 10.910,
            changeAtLng = 123.420,
        )
        assertTrue(snapshot.progress > 0.6f)
        assertEquals("Continue to The Mall", snapshot.nextPrompt)
    }

    @Test
    fun `arriving at the end reports arrival`() {
        val snapshot = JourneyGuidance.snapshot(
            points = points,
            currentLat = 10.930,
            currentLng = 123.440,
            destinationName = "The Mall",
        )
        assertTrue(snapshot.remainingKm < 0.15)
        assertTrue(snapshot.arrived)
        assertEquals("You have arrived at The Mall", snapshot.nextPrompt)
    }
}
