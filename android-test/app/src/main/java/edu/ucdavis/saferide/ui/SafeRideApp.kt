package edu.ucdavis.saferide.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import edu.ucdavis.saferide.ui.home.FullMapScreen
import edu.ucdavis.saferide.ui.home.MainScreen
import edu.ucdavis.saferide.ui.places.ExploreScreen
import edu.ucdavis.saferide.ui.places.PlaceSearchMode
import edu.ucdavis.saferide.ui.places.PlaceSearchScreen
import edu.ucdavis.saferide.ui.places.SetOnMapScreen
import edu.ucdavis.saferide.ui.ride.CalculatingScreen
import edu.ucdavis.saferide.ui.ride.TrackingScreen
import edu.ucdavis.saferide.ui.ride.TripCompleteScreen
import edu.ucdavis.saferide.ui.trip.PassengersScreen
import edu.ucdavis.saferide.ui.trip.ReviewTripScreen
import edu.ucdavis.saferide.ui.trip.RideUnavailableScreen
import edu.ucdavis.saferide.ui.welcome.SignInScreen
import edu.ucdavis.saferide.ui.welcome.WelcomeScreen

/** Shows the screen on top of the back stack and slides between screens like a native push/pop. */
@Composable
fun SafeRideApp(viewModel: SafeRideViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(enabled = state.backStack.size > 1) { viewModel.back() }
    BackHandler(enabled = state.screen == Screen.Main && state.tab == MainTab.SETTINGS) {
        viewModel.selectTab(MainTab.HOME)
    }

    AnimatedContent(
        targetState = state.backStack,
        contentKey = { it.last() },
        transitionSpec = {
            // Live-ride screens replace the stack but should still feel like moving forward.
            val forward = targetState.size >= initialState.size ||
                targetState.last() == Screen.Tracking || targetState.last() == Screen.TripComplete
            slideInHorizontally { if (forward) it else -it / 3 } togetherWith
                slideOutHorizontally { if (forward) -it / 3 else it }
        },
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        label = "screens",
    ) { stack ->
        when (val screen = stack.last()) {
            Screen.Welcome -> WelcomeScreen(onGetStarted = viewModel::getStarted)

            Screen.SignIn -> SignInScreen(
                signingIn = state.signingIn,
                onSignIn = viewModel::signIn,
                onClose = { viewModel.back() },
            )

            Screen.Main -> MainScreen(
                state = state,
                onSelectTab = viewModel::selectTab,
                onWhereTo = viewModel::openWhereTo,
                onExpandMap = viewModel::openFullMap,
                onSignOut = viewModel::signOut,
            )

            Screen.FullMap -> FullMapScreen(state.campusStatus, onClose = { viewModel.back() })

            Screen.WhereTo -> PlaceSearchScreen(
                mode = PlaceSearchMode.DESTINATION,
                recentPlaces = state.recentPlaces,
                onBack = { viewModel.back() },
                onPlace = viewModel::chooseDestination,
                onExplore = { viewModel.openExplore(it, forPickup = false) },
                onSetOnMap = {},
            )

            Screen.WhereAreYou -> PlaceSearchScreen(
                mode = PlaceSearchMode.PICKUP,
                recentPlaces = state.recentPlaces,
                onBack = { viewModel.back() },
                onPlace = viewModel::choosePickupPlace,
                onExplore = { viewModel.openExplore(it, forPickup = true) },
                onSetOnMap = viewModel::openSetOnMap,
            )

            is Screen.Explore -> ExploreScreen(
                category = screen.category,
                onBack = { viewModel.back() },
                onPlace = if (screen.forPickup) viewModel::choosePickupPlace else viewModel::chooseDestination,
            )

            Screen.SetOnMap -> SetOnMapScreen(
                status = state.campusStatus,
                onBack = { viewModel.back() },
                onConfirm = viewModel::confirmMapPin,
            )

            Screen.Passengers -> PassengersScreen(
                initialCount = state.trip.passengers,
                initialWheelchair = state.trip.wheelchair,
                onBack = { viewModel.back() },
                onNext = viewModel::setPassengers,
            )

            Screen.Review -> ReviewTripScreen(
                trip = state.trip,
                status = state.campusStatus,
                nearestVan = state.nearestVan,
                requesting = state.requesting,
                onBack = { viewModel.back() },
                onClose = viewModel::cancelTrip,
                onEditPassengers = viewModel::editPassengers,
                onSaveNote = viewModel::setNote,
                onContinue = viewModel::requestRide,
            )

            is Screen.Unavailable -> RideUnavailableScreen(screen.message, onGoBack = { viewModel.back() })

            Screen.Calculating -> CalculatingScreen(state.activeRide, state.campusStatus)

            Screen.Tracking -> TrackingScreen(
                ride = state.activeRide,
                trip = state.trip,
                status = state.campusStatus,
                onBoard = viewModel::boardVan,
                onCancel = viewModel::endRide,
            )

            Screen.TripComplete -> TripCompleteScreen(state.activeRide, onEndTrip = viewModel::endRide)
        }
    }
}
