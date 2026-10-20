package edu.ucdavis.saferide.domain

import edu.ucdavis.saferide.data.CampusLocations
import edu.ucdavis.saferide.data.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripRulesTest {

    private val square = listOf(LatLng(0.0, 0.0), LatLng(0.0, 1.0), LatLng(1.0, 1.0), LatLng(1.0, 0.0))

    @Test
    fun polygonContainment() {
        assertTrue(Geo.contains(square, LatLng(0.5, 0.5)))
        assertFalse(Geo.contains(square, LatLng(1.5, 0.5)))
        assertFalse(Geo.contains(square, LatLng(0.5, -0.1)))
    }

    @Test
    fun campusAndParkingAreOnCampus_housingIsNot() {
        (CampusLocations.campus + CampusLocations.parkingLots).forEach {
            assertTrue("${it.name} should be on campus", TripRules.isOnCampus(it.position))
        }
        CampusLocations.offCampusHousing.forEach {
            assertFalse("${it.name} should be off campus", TripRules.isOnCampus(it.position))
        }
        assertTrue(TripRules.isOnCampus(CampusLocations.simulatedCurrentLocation))
    }

    @Test
    fun everyPlaceIsInsideTheServiceArea() {
        CampusLocations.all.forEach {
            assertTrue("${it.name} should be in the service area", TripRules.isInServiceArea(it.position))
        }
    }

    @Test
    fun unavailableReasons() {
        assertNull(TripRules.unavailableReason(CampusLocations.silo.position, CampusLocations.westVillage.position))
        assertEquals(
            TripRules.OFF_CAMPUS_PICKUP_MESSAGE,
            TripRules.unavailableReason(CampusLocations.westVillage.position, CampusLocations.silo.position),
        )
        assertEquals(
            TripRules.OUTSIDE_SERVICE_AREA_MESSAGE,
            TripRules.unavailableReason(CampusLocations.silo.position, LatLng(38.5816, -121.4944)),
        )
    }

    @Test
    fun passengerLimits() {
        assertFalse(TripRules.isValidPassengerCount(null))
        assertFalse(TripRules.isValidPassengerCount(0))
        assertTrue(TripRules.isValidPassengerCount(1))
        assertTrue(TripRules.isValidPassengerCount(6))
        assertFalse(TripRules.isValidPassengerCount(7))
    }

    @Test
    fun pickupSpot_isNearestStopOnCampus_orThePinOffCampus() {
        val nearShields = LatLng(38.53965, -121.74970)
        assertEquals(CampusLocations.shieldsLibrary, TripRules.pickupSpotFor(nearShields))

        val offCampus = CampusLocations.solAtParkside.position
        assertEquals(offCampus, TripRules.pickupSpotFor(offCampus).position)
        assertEquals("pin", TripRules.pickupSpotFor(offCampus).id)
    }

    @Test
    fun startLabel_currentLocationOrNearestPlace() {
        val gps = CampusLocations.simulatedCurrentLocation
        assertEquals("Current Location", TripRules.startLabelFor(gps, current = gps))
        assertEquals("Near Shields Library", TripRules.startLabelFor(LatLng(38.53965, -121.74970), current = gps))
        // Without a GPS fix nothing counts as "Current Location".
        assertEquals("Near Hart Hall", TripRules.startLabelFor(gps, current = null))
    }
}
