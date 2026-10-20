package edu.ucdavis.saferide.ui

import edu.ucdavis.saferide.data.CampusLocations
import edu.ucdavis.saferide.data.LatLng
import edu.ucdavis.saferide.data.LocationProvider
import edu.ucdavis.saferide.data.MockSafeRideService
import edu.ucdavis.saferide.data.PlaceCategory
import edu.ucdavis.saferide.data.RideStatus
import edu.ucdavis.saferide.data.Vehicle
import edu.ucdavis.saferide.domain.NearestVan
import edu.ucdavis.saferide.domain.StatusText
import edu.ucdavis.saferide.domain.TripRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SafeRideViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private val SafeRideViewModel.screen get() = state.value.screen

    /** Signs in and walks to the review screen with Shields Library → West Village. */
    private fun TestScope.reviewReady(): SafeRideViewModel {
        val vm = SafeRideViewModel(MockSafeRideService())
        vm.getStarted()
        vm.signIn()
        advanceUntilIdle()
        vm.openWhereTo()
        vm.chooseDestination(CampusLocations.westVillage)
        vm.choosePickupPlace(CampusLocations.shieldsLibrary)
        vm.setPassengers(2, wheelchair = false)
        return vm
    }

    @Test
    fun startsOnWelcome_thenSignInLandsOnHome() = runTest(dispatcher) {
        val vm = SafeRideViewModel(MockSafeRideService())
        assertEquals(Screen.Welcome, vm.screen)
        vm.getStarted()
        assertEquals(Screen.SignIn, vm.screen)
        vm.signIn()
        assertTrue(vm.state.value.signingIn)
        advanceUntilIdle()
        assertEquals(listOf(Screen.Main), vm.state.value.backStack)
        assertEquals(3, vm.state.value.campusStatus?.activeVanCount)
    }

    @Test
    fun bookingFlowFollowsTheReferenceScreens() = runTest(dispatcher) {
        val vm = SafeRideViewModel(MockSafeRideService())
        vm.signIn()
        advanceUntilIdle()

        vm.openWhereTo()
        assertEquals(Screen.WhereTo, vm.screen)
        vm.chooseDestination(CampusLocations.westVillage)
        assertEquals(Screen.WhereAreYou, vm.screen)
        vm.choosePickupPlace(CampusLocations.shieldsLibrary)
        assertEquals(Screen.Passengers, vm.screen)
        vm.setPassengers(2, wheelchair = true)
        assertEquals(Screen.Review, vm.screen)

        val trip = vm.state.value.trip
        assertEquals(CampusLocations.westVillage, trip.destination)
        assertEquals(CampusLocations.shieldsLibrary, trip.pickupSpot)
        assertEquals("Shields Library", trip.start?.label)
        assertEquals(2, trip.passengers)
        assertTrue(trip.wheelchair)
    }

    @Test
    fun exploreListIsSkippedWhenGoingBack() = runTest(dispatcher) {
        val vm = SafeRideViewModel(MockSafeRideService())
        vm.openWhereTo()
        vm.openExplore(PlaceCategory.HOUSING, forPickup = false)
        assertEquals(Screen.Explore(PlaceCategory.HOUSING, false), vm.screen)
        vm.chooseDestination(CampusLocations.solAtParkside)
        assertEquals(Screen.WhereAreYou, vm.screen)
        vm.back()
        assertEquals(Screen.WhereTo, vm.screen)
    }

    @Test
    fun invalidPassengerCountStaysOnPassengers() = runTest(dispatcher) {
        val vm = SafeRideViewModel(MockSafeRideService())
        vm.openWhereTo()
        vm.chooseDestination(CampusLocations.arc)
        vm.choosePickupPlace(CampusLocations.silo)
        vm.setPassengers(TripRules.MAX_PASSENGERS + 1, wheelchair = false)
        assertEquals(Screen.Passengers, vm.screen)
    }

    @Test
    fun editingPassengersReturnsToReview() = runTest(dispatcher) {
        val vm = reviewReady()
        vm.editPassengers()
        assertEquals(Screen.Passengers, vm.screen)
        vm.setPassengers(4, wheelchair = false)
        assertEquals(Screen.Review, vm.screen)
        assertEquals(4, vm.state.value.trip.passengers)
        // Review appears once in the stack, not twice.
        assertEquals(1, vm.state.value.backStack.count { it == Screen.Review })
    }

    @Test
    fun mapPinNearCurrentLocationWalksToNearestStop() = runTest(dispatcher) {
        val vm = SafeRideViewModel(MockSafeRideService())
        advanceUntilIdle() // GPS fix
        vm.openWhereTo()
        vm.chooseDestination(CampusLocations.westVillage)
        vm.openSetOnMap()
        vm.confirmMapPin(CampusLocations.simulatedCurrentLocation)
        val trip = vm.state.value.trip
        assertEquals("Current Location", trip.start?.label)
        assertEquals(CampusLocations.hartHall, trip.pickupSpot)
        assertEquals(Screen.Passengers, vm.screen)
    }

    @Test
    fun currentLocationUsesTheGpsFix_andPicksTheNearestStop() = runTest(dispatcher) {
        // Standing outside Shields Library.
        val gps = LatLng(38.53965, -121.74970)
        val vm = SafeRideViewModel(MockSafeRideService(), locationProvider = FixedLocation(gps))
        advanceUntilIdle()
        assertEquals(gps, vm.state.value.myLocation)

        vm.openWhereTo()
        advanceUntilIdle()
        vm.chooseDestination(CampusLocations.westVillage)
        vm.useCurrentLocation()
        val trip = vm.state.value.trip
        assertEquals("Current Location", trip.start?.label)
        assertEquals(gps, trip.start?.position)
        assertEquals(CampusLocations.shieldsLibrary, trip.pickupSpot)
        assertEquals(Screen.Passengers, vm.screen)
    }

    @Test
    fun noGpsFix_hasNoCurrentLocation_andCantBookFromIt() = runTest(dispatcher) {
        val vm = SafeRideViewModel(MockSafeRideService(), locationProvider = FixedLocation(null))
        advanceUntilIdle()
        assertNull(vm.state.value.myLocation)
        vm.openWhereTo()
        vm.chooseDestination(CampusLocations.westVillage)
        vm.useCurrentLocation()
        advanceUntilIdle()
        assertNull(vm.state.value.trip.pickupSpot)
        assertEquals(Screen.WhereAreYou, vm.screen)
    }

    private class FixedLocation(private val position: LatLng?) : LocationProvider {
        override suspend fun currentLocation(): LatLng? = position
    }

    @Test
    fun continueShowsCalculatingThenLiveTracking() = runTest(dispatcher) {
        val vm = reviewReady()
        vm.requestRide()
        assertTrue(vm.state.value.requesting)
        assertEquals(Screen.Calculating, vm.screen)
        assertNull(vm.state.value.activeRide)

        // Request accepted: the calculating screen shows the place in line.
        advanceTimeBy(700)
        assertEquals(Screen.Calculating, vm.screen)
        assertEquals(3, vm.state.value.activeRide?.queuePosition)

        // Then the live tracking screen replaces the booking flow.
        advanceTimeBy(2_000)
        assertEquals(listOf(Screen.Main, Screen.Tracking), vm.state.value.backStack)
        assertFalse(vm.state.value.requesting)
        assertEquals(RideStatus.QUEUED, vm.state.value.activeRide?.status)

        advanceTimeBy(5_000)
        assertEquals(2, vm.state.value.activeRide?.queuePosition)
        advanceTimeBy(5_000)
        assertEquals(RideStatus.ASSIGNED, vm.state.value.activeRide?.status)
        assertTrue(vm.state.value.activeRide!!.route.isNotEmpty())

        advanceUntilIdle()
        assertEquals(RideStatus.ARRIVED, vm.state.value.activeRide?.status)
        assertEquals(Screen.Tracking, vm.screen)
    }

    @Test
    fun boardingRidesToTheDropoffThenEndTripResetsHome() = runTest(dispatcher) {
        val vm = reviewReady()
        vm.requestRide()
        advanceUntilIdle()
        assertEquals(RideStatus.ARRIVED, vm.state.value.activeRide?.status)

        vm.boardVan()
        runCurrent()
        assertEquals(RideStatus.ON_BOARD, vm.state.value.activeRide?.status)
        advanceUntilIdle()
        assertEquals(listOf(Screen.Main, Screen.TripComplete), vm.state.value.backStack)
        assertEquals(RideStatus.COMPLETED, vm.state.value.activeRide?.status)

        vm.endRide()
        assertEquals(listOf(Screen.Main), vm.state.value.backStack)
        assertEquals(MainTab.HOME, vm.state.value.tab)
        assertNull(vm.state.value.activeRide)
        assertNull(vm.state.value.trip.destination)
    }

    @Test
    fun boardingIsIgnoredBeforeTheVanArrives() = runTest(dispatcher) {
        val vm = reviewReady()
        vm.requestRide()
        advanceTimeBy(3_000)
        assertEquals(RideStatus.QUEUED, vm.state.value.activeRide?.status)
        vm.boardVan()
        runCurrent()
        assertEquals(RideStatus.QUEUED, vm.state.value.activeRide?.status)
        vm.endRide()
    }

    @Test
    fun cancellingStopsLiveUpdates() = runTest(dispatcher) {
        val vm = reviewReady()
        vm.requestRide()
        advanceTimeBy(3_000)
        assertEquals(Screen.Tracking, vm.screen)
        vm.endRide()
        advanceUntilIdle()
        assertEquals(listOf(Screen.Main), vm.state.value.backStack)
        // No stray updates bring the ride back after cancelling.
        assertNull(vm.state.value.activeRide)
    }

    @Test
    fun offCampusPickupShowsRideNotAvailable_andGoBackReturnsToReview() = runTest(dispatcher) {
        val vm = SafeRideViewModel(MockSafeRideService())
        vm.openWhereTo()
        vm.chooseDestination(CampusLocations.memorialUnion)
        vm.choosePickupPlace(CampusLocations.westVillage)
        vm.setPassengers(1, wheelchair = false)
        vm.requestRide()
        advanceUntilIdle()
        assertEquals(Screen.Unavailable(TripRules.OFF_CAMPUS_PICKUP_MESSAGE), vm.screen)
        assertNull(vm.state.value.activeRide)
        // The calculating screen is dropped, so Go Back lands on the review.
        assertFalse(vm.state.value.backStack.contains(Screen.Calculating))
        vm.back()
        assertEquals(Screen.Review, vm.screen)
    }

    @Test
    fun closeOnReviewCancelsTheTrip() = runTest(dispatcher) {
        val vm = reviewReady()
        vm.cancelTrip()
        assertEquals(listOf(Screen.Main), vm.state.value.backStack)
        assertNull(vm.state.value.trip.destination)
    }

    @Test
    fun recentPlacesPutNewestFirstWithoutDuplicates() = runTest(dispatcher) {
        val vm = SafeRideViewModel(MockSafeRideService())
        assertEquals(CampusLocations.seededRecents, vm.state.value.recentPlaces)
        vm.openWhereTo()
        vm.chooseDestination(CampusLocations.theUApartments)
        vm.choosePickupPlace(CampusLocations.silo)
        assertEquals(
            listOf(CampusLocations.silo, CampusLocations.theUApartments, CampusLocations.hartHall),
            vm.state.value.recentPlaces,
        )
    }

    @Test
    fun nearestVanAppearsOnceThePickupIsKnown() = runTest(dispatcher) {
        val vm = SafeRideViewModel(MockSafeRideService())
        advanceUntilIdle()
        assertNull(vm.state.value.nearestVan)
        vm.openWhereTo()
        vm.chooseDestination(CampusLocations.arc)
        vm.choosePickupPlace(CampusLocations.memorialUnion)
        // Van #1 is parked next to the Memorial Union.
        assertEquals("Van #1", vm.state.value.nearestVan?.vehicle?.displayName)
    }

    @Test
    fun nearestVan_picksTheClosestAndFormatsText() {
        val pickup = LatLng(38.5400, -121.7500)
        val near = Vehicle(7, "NEAR", LatLng(38.5410, -121.7500))
        val far = Vehicle(8, "FAR", LatLng(38.5500, -121.7700))
        val estimate = NearestVan.find(listOf(far, near), pickup)!!
        assertEquals(7, estimate.vehicle.number)
        assertEquals("Nearest van: Van #7 • ~1 min from Silo", StatusText.nearestVan(estimate, "Silo"))
        assertNull(NearestVan.find(emptyList(), pickup))
    }

    @Test
    fun signOutReturnsToWelcome() = runTest(dispatcher) {
        val vm = reviewReady()
        vm.signOut()
        assertEquals(listOf(Screen.Welcome), vm.state.value.backStack)
    }
}
