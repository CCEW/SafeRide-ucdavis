package edu.ucdavis.saferide.data

import edu.ucdavis.saferide.domain.Geo
import edu.ucdavis.saferide.domain.Proximity
import edu.ucdavis.saferide.domain.StatusText
import edu.ucdavis.saferide.domain.TripRules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MockSafeRideServiceTest {

    private val service = MockSafeRideService()

    @Test
    fun campusStatus_hasThreeVansAndAWaitEstimate() = runTest {
        val status = service.fetchCampusStatus()
        assertEquals(3, status.activeVanCount)
        assertEquals("3 Vans Active • Estimated Campus Wait: ~12 mins", StatusText.campusBanner(status))
    }

    @Test
    fun requestRide_startsQueuedAtPositionThree() = runTest {
        val ride = service.requestRide(CampusLocations.shieldsLibrary, CampusLocations.westVillage)
        assertEquals(RideStatus.QUEUED, ride.status)
        assertEquals(3, ride.queuePosition)
        assertNull(ride.assignedVehicle)
        assertEquals(CampusLocations.RIDER_NAME, ride.rider)
        assertEquals("Request Received • Position in Line: #3", StatusText.queueCard(ride))
    }

    @Test
    fun updates_followTheStateMachineAndEndWithArrival() = runTest {
        val ride = service.requestRide(CampusLocations.shieldsLibrary, CampusLocations.westVillage)
        val start = currentTime
        val timestamps = mutableListOf<Long>()
        val updates = service.startSimulatedQueueUpdates(ride)
            .onEach { timestamps += currentTime - start }
            .toList()

        // Step A: position 2 after one queue step.
        assertEquals(2, updates[0].queuePosition)
        assertEquals(RideStatus.QUEUED, updates[0].status)
        assertEquals(5_000L, timestamps[0])

        // Step B: position 1 and Van #4 assigned.
        assertEquals(1, updates[1].queuePosition)
        assertEquals(RideStatus.ASSIGNED, updates[1].status)
        assertEquals("Van #4", updates[1].assignedVehicle?.displayName)
        assertEquals(10_000L, timestamps[1])

        // Step C: exactly one ARRIVED, and it's the last update.
        val last = updates.last()
        assertEquals(RideStatus.ARRIVED, last.status)
        assertEquals(1, updates.count { it.status == RideStatus.ARRIVED })
        assertEquals("Van #4 is at the Shields Library Bus Turnaround", StatusText.queueCard(last))
    }

    @Test
    fun arrival_neverFiresBeforeTheVanIsInside100Feet() = runTest {
        for (pickup in CampusLocations.campus + CampusLocations.parkingLots) {
            val ride = service.requestRide(pickup, CampusLocations.memorialUnion)
            val updates = service.startSimulatedQueueUpdates(ride).toList()

            val distances = updates.filter { it.assignedVehicle != null }
                .map { Geo.distanceMeters(it.assignedVehicle!!.position, pickup.position) }

            // Every update before arrival is outside the radius; the arrival is inside it.
            distances.dropLast(1).forEach {
                assertTrue("${pickup.name}: early arrival at $it m", it > Proximity.ARRIVAL_RADIUS_METERS)
            }
            assertTrue(distances.last() <= Proximity.ARRIVAL_RADIUS_METERS)
        }
    }

    @Test
    fun van_movesCloserAndEtaNeverIncreases() = runTest {
        val ride = service.requestRide(CampusLocations.silo, CampusLocations.arc)
        val assigned = service.startSimulatedQueueUpdates(ride)
            .toList()
            .filter { it.status == RideStatus.ASSIGNED }

        val remaining = assigned.map { it.metersToPickup!! }
        assertEquals(remaining.sortedDescending(), remaining)
        val etas = assigned.map { it.etaMinutes!! }
        assertEquals(etas.sortedDescending(), etas)
        assertTrue(StatusText.queueCard(assigned.first()).startsWith("Van #4 Assigned • Position in Line: #1 • Driver ~"))
    }

    @Test
    fun requestRide_carriesPassengerDetails() = runTest {
        val ride = service.requestRide(CampusLocations.silo, CampusLocations.westVillage, 2, wheelchair = true, note = "Red jacket")
        assertEquals(2, ride.passengers)
        assertTrue(ride.wheelchair)
        assertEquals("Red jacket", ride.note)
    }

    @Test
    fun requestRide_declinesOffCampusPickup() = runTest {
        val error = assertThrows(RideUnavailableException::class.java) {
            kotlinx.coroutines.runBlocking { service.requestRide(CampusLocations.westVillage, CampusLocations.memorialUnion) }
        }
        assertEquals(TripRules.OFF_CAMPUS_PICKUP_MESSAGE, error.message)
    }

    @Test
    fun requestRide_declinesDropoffOutsideServiceArea() = runTest {
        val sacramento = CampusLocation("sac", "Sacramento", "Downtown", LatLng(38.5816, -121.4944))
        val error = assertThrows(RideUnavailableException::class.java) {
            kotlinx.coroutines.runBlocking { service.requestRide(CampusLocations.shieldsLibrary, sacramento) }
        }
        assertEquals(TripRules.OUTSIDE_SERVICE_AREA_MESSAGE, error.message)
    }

    @Test
    fun assignedUpdatesCarryTheRouteToThePickup() = runTest {
        val ride = service.requestRide(CampusLocations.shieldsLibrary, CampusLocations.westVillage)
        val assigned = service.startSimulatedQueueUpdates(ride).toList().first { it.status == RideStatus.ASSIGNED }
        assertEquals(CampusLocations.shieldsLibrary.position, assigned.route.last())
        assertEquals(assigned.assignedVehicle!!.position, assigned.route.first())
    }

    @Test
    fun trip_ridesFromPickupToDropoffThenCompletes() = runTest {
        val ride = service.requestRide(CampusLocations.shieldsLibrary, CampusLocations.westVillage)
        val arrived = service.startSimulatedQueueUpdates(ride).toList().last()
        val updates = service.startSimulatedTrip(arrived).toList()

        assertEquals(RideStatus.ON_BOARD, updates.first().status)
        assertEquals(RideStatus.COMPLETED, updates.last().status)
        assertEquals(1, updates.count { it.status == RideStatus.COMPLETED })
        assertEquals(CampusLocations.westVillage.position, updates.last().assignedVehicle!!.position)
        assertEquals("Van #4", updates.last().assignedVehicle!!.displayName)

        val etas = updates.dropLast(1).map { it.etaMinutes!! }
        assertEquals(etas.sortedDescending(), etas)
        assertTrue(StatusText.queueCard(updates.first()).startsWith("On your way to West Village"))
    }

    @Test
    fun kemperAndMemorialUnionToTheU_driveTheStoredRoadPaths() = runTest {
        val trips = listOf(
            CampusLocations.kemperHall to (RoadRoutes.vanToKemper to RoadRoutes.kemperToTheU),
            CampusLocations.memorialUnion to (RoadRoutes.vanToMemorialUnion to RoadRoutes.memorialUnionToTheU),
        )
        for ((pickup, paths) in trips) {
            val (toPickup, toDropoff) = paths
            val ride = service.requestRide(pickup, CampusLocations.theUApartments)
            val updates = service.startSimulatedQueueUpdates(ride).toList()

            // The van drives the road path to the pickup, not the L-shaped fallback.
            assertEquals(toPickup, updates.first { it.status == RideStatus.ASSIGNED }.route)
            assertEquals(RideStatus.ARRIVED, updates.last().status)

            // Then the road path to The U, ending at its front gate.
            val trip = service.startSimulatedTrip(updates.last()).toList()
            assertEquals(toDropoff, trip.first().route)
            assertEquals(CampusLocations.theUApartments.position, trip.last().assignedVehicle!!.position)
            assertEquals(RideStatus.COMPLETED, trip.last().status)
        }
    }

    @Test
    fun queueEstimate_onlyDropsAsTheRiderMovesUp() {
        assertEquals("Estimated pickup ~12 mins", StatusText.queueEstimate(3, 12))
        assertEquals("Estimated pickup ~8 mins", StatusText.queueEstimate(2, 12))
        assertEquals("Estimated pickup ~4 mins", StatusText.queueEstimate(1, 12))
        assertEquals("Estimated pickup ~1 min", StatusText.queueEstimate(1, 1))
    }

    @Test
    fun completeRide_marksTripComplete() = runTest {
        val ride = service.requestRide(CampusLocations.coho, CampusLocations.solAtParkside)
        assertEquals(RideStatus.COMPLETED, service.completeRide(ride).status)
    }
}
