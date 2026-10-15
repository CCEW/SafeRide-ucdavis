package edu.ucdavis.saferide.domain

import edu.ucdavis.saferide.data.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoAndRouteTest {

    private val origin = LatLng(38.5400, -121.7500)

    /** About 111.2 m per 0.001° of latitude. */
    private fun north(meters: Double) = LatLng(origin.latitude + meters / 111_195.0, origin.longitude)

    @Test
    fun distance_matchesKnownLatitudeSpacing() {
        assertEquals(1000.0, Geo.distanceMeters(origin, north(1000.0)), 1.0)
        assertEquals(0.0, Geo.distanceMeters(origin, origin), 0.0)
    }

    @Test
    fun proximity_firesOnlyInside100Feet() {
        assertTrue(Proximity.hasArrived(north(30.0), origin))
        assertTrue(Proximity.hasArrived(north(30.4), origin))
        assertFalse(Proximity.hasArrived(north(30.6), origin))
        assertFalse(Proximity.hasArrived(north(100.0), origin))
    }

    @Test
    fun route_positionsByDistanceTraveled() {
        val route = Route(listOf(origin, north(200.0), north(500.0)))
        assertEquals(500.0, route.lengthMeters, 1.0)
        assertEquals(origin, route.positionAt(0.0))
        assertEquals(250.0, Geo.distanceMeters(origin, route.positionAt(250.0)), 1.0)
        assertEquals(north(500.0), route.positionAt(9_999.0))
    }

    @Test
    fun remainingFrom_dropsTheCornersAlreadyPassed() {
        val route = Route(listOf(origin, north(200.0), north(500.0)))
        assertEquals(route.points, route.remainingFrom(0.0))
        val ahead = route.remainingFrom(300.0)
        assertEquals(2, ahead.size)
        assertEquals(300.0, Geo.distanceMeters(origin, ahead.first()), 1.0)
        assertEquals(north(500.0), ahead.last())
        assertEquals(1, route.remainingFrom(500.0).size)
    }

    @Test
    fun toPickup_endsAtPickupAndBacksOffWhenVanIsTooClose() {
        val far = Route.toPickup(LatLng(38.5350, -121.7550), origin)
        assertEquals(origin, far.points.last())

        val close = Route.toPickup(north(20.0), origin)
        assertEquals(origin, close.points.last())
        assertTrue(close.lengthMeters >= Route.MIN_APPROACH_METERS)
    }

    @Test
    fun eta_roundsUpAndIsAtLeastOneMinute() {
        assertEquals(1, Route.etaMinutes(10.0))
        assertEquals(2, Route.etaMinutes(600.0)) // 600 m / 5.5 m/s ≈ 109 s
        assertEquals(4, Route.etaMinutes(1_000.0))
    }
}
