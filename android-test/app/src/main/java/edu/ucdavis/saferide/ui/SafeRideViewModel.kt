package edu.ucdavis.saferide.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.ucdavis.saferide.data.CampusLocation
import edu.ucdavis.saferide.data.CampusLocations
import edu.ucdavis.saferide.data.CampusStatus
import edu.ucdavis.saferide.data.LatLng
import edu.ucdavis.saferide.data.MockSafeRideService
import edu.ucdavis.saferide.data.PlaceCategory
import edu.ucdavis.saferide.data.RideRequest
import edu.ucdavis.saferide.data.RideStatus
import edu.ucdavis.saferide.data.RideUnavailableException
import edu.ucdavis.saferide.data.SafeRideService
import edu.ucdavis.saferide.data.StartLocation
import edu.ucdavis.saferide.domain.NearestVan
import edu.ucdavis.saferide.domain.TripRules
import edu.ucdavis.saferide.domain.VanEstimate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Every screen in the app, in the order the booking flow visits them. */
sealed interface Screen {
    data object Welcome : Screen
    data object SignIn : Screen
    /** Home and Settings tabs. */
    data object Main : Screen
    data object FullMap : Screen
    data object WhereTo : Screen
    data object WhereAreYou : Screen
    data class Explore(val category: PlaceCategory, val forPickup: Boolean) : Screen
    data object SetOnMap : Screen
    data object Passengers : Screen
    data object Review : Screen
    data class Unavailable(val message: String) : Screen
    /** "Calculating your place in line" while the request goes through. */
    data object Calculating : Screen
    /** Live queue position, van on the map, arrival alert, and the ride to the drop-off. */
    data object Tracking : Screen
    data object TripComplete : Screen
}

enum class MainTab { HOME, SETTINGS }

/** What the rider has chosen so far. */
data class TripDraft(
    val destination: CampusLocation? = null,
    val start: StartLocation? = null,
    val pickupSpot: CampusLocation? = null,
    val passengers: Int? = null,
    val wheelchair: Boolean = false,
    val note: String = "",
)

data class SafeRideUiState(
    val backStack: List<Screen> = listOf(Screen.Welcome),
    val tab: MainTab = MainTab.HOME,
    /** Null while the first status fetch is in flight. */
    val campusStatus: CampusStatus? = null,
    val trip: TripDraft = TripDraft(),
    val recentPlaces: List<CampusLocation> = CampusLocations.seededRecents,
    val signingIn: Boolean = false,
    val requesting: Boolean = false,
    val activeRide: RideRequest? = null,
) {
    val screen: Screen get() = backStack.last()

    /** The closest van to the chosen pickup spot, shown before booking. */
    val nearestVan: VanEstimate?
        get() {
            val pickup = trip.pickupSpot ?: return null
            return campusStatus?.let { NearestVan.find(it.vehicles, pickup.position) }
        }
}

class SafeRideViewModel(
    private val service: SafeRideService = MockSafeRideService.shared,
) : ViewModel() {

    private val _state = MutableStateFlow(SafeRideUiState())
    val state: StateFlow<SafeRideUiState> = _state.asStateFlow()

    /** Collects live updates for the active ride; cancelled when the ride ends. */
    private var rideJob: Job? = null

    init {
        refreshCampusStatus()
    }

    fun refreshCampusStatus() {
        viewModelScope.launch {
            val status = service.fetchCampusStatus()
            _state.update { it.copy(campusStatus = status) }
        }
    }

    // region Navigation

    /** Returns false when there's nothing to go back to, so the system can close the app. */
    fun back(): Boolean {
        val stack = _state.value.backStack
        if (stack.size <= 1) return false
        _state.update { it.copy(backStack = stack.dropLast(1)) }
        return true
    }

    fun selectTab(tab: MainTab) = _state.update { it.copy(tab = tab) }

    fun openFullMap() = push(Screen.FullMap)

    private fun push(screen: Screen) = _state.update { it.copy(backStack = it.backStack + screen) }

    /** Goes to [screen], popping back to it if it's already on the stack (e.g. returning to Review after an edit). */
    private fun advanceTo(screen: Screen) = _state.update {
        val existing = it.backStack.indexOf(screen)
        val stack = if (existing >= 0) it.backStack.take(existing + 1) else it.backStack + screen
        it.copy(backStack = stack)
    }

    /** Drops any Explore list so Back from the next screen skips it. */
    private fun withoutExplore(stack: List<Screen>) = stack.filterNot { it is Screen.Explore }

    // endregion

    // region Welcome and sign-in (simulated; the rider is always Alex)

    fun getStarted() = push(Screen.SignIn)

    fun signIn() {
        if (_state.value.signingIn) return
        // Set before launching so the spinner shows at once and double taps are ignored.
        _state.update { it.copy(signingIn = true) }
        viewModelScope.launch {
            delay(SIGN_IN_DELAY_MILLIS)
            _state.update { it.copy(signingIn = false, backStack = listOf(Screen.Main), tab = MainTab.HOME) }
        }
    }

    fun signOut() {
        rideJob?.cancel()
        _state.update {
            it.copy(backStack = listOf(Screen.Welcome), trip = TripDraft(), activeRide = null, tab = MainTab.HOME)
        }
    }

    // endregion

    // region Booking flow

    /** "Where to?" on the home screen starts a fresh trip. */
    fun openWhereTo() = _state.update {
        it.copy(trip = TripDraft(), backStack = listOf(Screen.Main, Screen.WhereTo))
    }

    fun openExplore(category: PlaceCategory, forPickup: Boolean) = push(Screen.Explore(category, forPickup))

    fun chooseDestination(place: CampusLocation) = _state.update {
        it.copy(
            trip = it.trip.copy(destination = place),
            recentPlaces = addRecent(it.recentPlaces, place),
            backStack = withoutExplore(it.backStack) + Screen.WhereAreYou,
        )
    }

    /** Picking a listed place: the rider is there, and the van meets them at that place's stop. */
    fun choosePickupPlace(place: CampusLocation) = _state.update {
        it.copy(
            trip = it.trip.copy(start = StartLocation(place.name, place.position), pickupSpot = place),
            recentPlaces = addRecent(it.recentPlaces, place),
            backStack = withoutExplore(it.backStack) + Screen.Passengers,
        )
    }

    fun openSetOnMap() = push(Screen.SetOnMap)

    /** Picking a spot on the map: the rider walks from there to the nearest campus stop. */
    fun confirmMapPin(position: LatLng) = _state.update {
        it.copy(
            trip = it.trip.copy(
                start = StartLocation(TripRules.startLabelFor(position), position),
                pickupSpot = TripRules.pickupSpotFor(position),
            ),
            backStack = it.backStack + Screen.Passengers,
        )
    }

    fun editPassengers() = push(Screen.Passengers)

    fun setPassengers(count: Int, wheelchair: Boolean) {
        if (!TripRules.isValidPassengerCount(count)) return
        _state.update { it.copy(trip = it.trip.copy(passengers = count, wheelchair = wheelchair)) }
        advanceTo(Screen.Review)
    }

    fun setNote(note: String) = _state.update { it.copy(trip = it.trip.copy(note = note.trim())) }

    /** The X on the review screen: abandon the trip and go home. */
    fun cancelTrip() = _state.update {
        it.copy(trip = TripDraft(), backStack = listOf(Screen.Main), tab = MainTab.HOME)
    }

    /** Continue on the review screen: calculate the queue position, then start live tracking. */
    fun requestRide() {
        val trip = _state.value.trip
        val pickup = trip.pickupSpot ?: return
        val dropoff = trip.destination ?: return
        val passengers = trip.passengers ?: return
        if (_state.value.requesting) return

        _state.update { it.copy(requesting = true, activeRide = null, backStack = it.backStack + Screen.Calculating) }
        viewModelScope.launch {
            try {
                val ride = service.requestRide(pickup, dropoff, passengers, trip.wheelchair, trip.note)
                // Let the rider read their place in line before the map takes over.
                _state.update { it.copy(activeRide = ride) }
                delay(CALCULATING_RESULT_MILLIS)
                // A live ride replaces the booking screens; Back no longer returns to the review.
                _state.update { it.copy(backStack = listOf(Screen.Main, Screen.Tracking)) }
                follow(service.startSimulatedQueueUpdates(ride))
            } catch (e: RideUnavailableException) {
                val message = e.message ?: TripRules.OFF_CAMPUS_PICKUP_MESSAGE
                _state.update {
                    it.copy(backStack = it.backStack.filterNot { s -> s == Screen.Calculating } + Screen.Unavailable(message))
                }
            } finally {
                _state.update { it.copy(requesting = false) }
            }
        }
    }

    /** "I'm in the Van" after the arrival alert: ride to the drop-off, then show Trip Complete. */
    fun boardVan() {
        val ride = _state.value.activeRide ?: return
        if (ride.status != RideStatus.ARRIVED) return
        rideJob?.cancel()
        rideJob = viewModelScope.launch {
            service.startSimulatedTrip(ride).collect { update ->
                _state.update { it.copy(activeRide = update) }
                if (update.status == RideStatus.COMPLETED) {
                    _state.update { it.copy(backStack = listOf(Screen.Main, Screen.TripComplete)) }
                }
            }
        }
    }

    /** End Trip (after completion) or Cancel Ride (before boarding): stop the simulation and go home. */
    fun endRide() {
        rideJob?.cancel()
        rideJob = null
        _state.update {
            it.copy(activeRide = null, trip = TripDraft(), backStack = listOf(Screen.Main), tab = MainTab.HOME)
        }
    }

    private fun follow(updates: Flow<RideRequest>) {
        rideJob?.cancel()
        rideJob = viewModelScope.launch {
            updates.collect { update -> _state.update { it.copy(activeRide = update) } }
        }
    }

    // endregion

    private fun addRecent(recents: List<CampusLocation>, place: CampusLocation): List<CampusLocation> =
        (listOf(place) + recents.filterNot { it.id == place.id }).take(MAX_RECENTS)

    private companion object {
        const val MAX_RECENTS = 3
        const val SIGN_IN_DELAY_MILLIS = 900L
        const val CALCULATING_RESULT_MILLIS = 2_000L
    }
}
