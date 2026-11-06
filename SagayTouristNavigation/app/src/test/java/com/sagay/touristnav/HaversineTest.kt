package com.sagay.touristnav

import com.sagay.touristnav.domain.Haversine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HaversineTest {

    @Test
    fun `0 point 01 degrees of latitude is about 1 point 11 km`() {
        val km = Haversine.km(10.896, 123.416, 10.906, 123.416)
        assertEquals(1.112, km, 0.01)
    }

    @Test
    fun `known distance between two Sagay points`() {
        // 0.045 degrees of longitude at ~10.9 N latitude is about 4.89 km.
        val km = Haversine.km(10.896, 123.416, 10.896, 123.461)
        assertEquals(4.887, km, 0.05)
    }

    @Test
    fun `same point is zero distance`() {
        assertEquals(0.0, Haversine.km(10.896, 123.416, 10.896, 123.416), 0.0001)
    }

    @Test
    fun `road distance shorter than straight line is suspicious`() {
        assertTrue(Haversine.isRoadDistanceSuspicious(roadKm = 1.0, straightLineKm = 2.0))
    }

    @Test
    fun `road distance more than 3x straight line is suspicious`() {
        assertTrue(Haversine.isRoadDistanceSuspicious(roadKm = 7.0, straightLineKm = 2.0))
    }

    @Test
    fun `normal road distance is not suspicious`() {
        assertFalse(Haversine.isRoadDistanceSuspicious(roadKm = 2.5, straightLineKm = 2.0))
    }
}
