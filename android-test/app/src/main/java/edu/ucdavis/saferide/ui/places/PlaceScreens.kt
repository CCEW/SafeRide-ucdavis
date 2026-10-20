package edu.ucdavis.saferide.ui.places

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucdavis.saferide.R
import edu.ucdavis.saferide.data.CampusLocation
import edu.ucdavis.saferide.data.CampusLocations
import edu.ucdavis.saferide.data.LatLng
import edu.ucdavis.saferide.data.PlaceCategory
import edu.ucdavis.saferide.domain.TripRules
import edu.ucdavis.saferide.ui.components.BlueHeader
import edu.ucdavis.saferide.ui.components.IconCircle
import edu.ucdavis.saferide.ui.components.ListRow
import edu.ucdavis.saferide.ui.components.SearchField
import edu.ucdavis.saferide.ui.components.SectionHeader
import edu.ucdavis.saferide.ui.theme.AggieBlue
import edu.ucdavis.saferide.ui.theme.GoldDark

enum class PlaceSearchMode(val title: String, val placeholder: String) {
    DESTINATION("Where to?", "Type or select a destination"),
    PICKUP("Where are you?", "Type or select a pickup location"),
}

/** What "Where are you?" is pre-filled with. Pressing Enter books from the rider's GPS position. */
private const val CURRENT_LOCATION = "Current Location"

/**
 * "Where to?" and "Where are you?": search, recent places, and explore lists.
 * "Where are you?" starts filled in with the rider's current location, selected so typing replaces it.
 */
@Composable
fun PlaceSearchScreen(
    mode: PlaceSearchMode,
    recentPlaces: List<CampusLocation>,
    onBack: () -> Unit,
    onPlace: (CampusLocation) -> Unit,
    onExplore: (PlaceCategory) -> Unit,
    onSetOnMap: () -> Unit,
    onUseCurrentLocation: () -> Unit,
    /** The rider's GPS position; null until there's a fix. */
    currentLocation: LatLng? = null,
) {
    val prefill = if (mode == PlaceSearchMode.PICKUP) CURRENT_LOCATION else ""
    var field by rememberSaveable(mode, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(prefill, selection = TextRange(0, prefill.length)))
    }
    val usingCurrent = mode == PlaceSearchMode.PICKUP && field.text == CURRENT_LOCATION
    val query = if (usingCurrent) "" else field.text.trim()
    val matches = if (query.isEmpty()) emptyList() else CampusLocations.all.filter { it.name.contains(query, ignoreCase = true) }

    // Open the keyboard on "Where are you?" so confirming the pre-filled location is a single Enter.
    val focus = remember { FocusRequester() }
    if (mode == PlaceSearchMode.PICKUP) {
        LaunchedEffect(Unit) { focus.requestFocus() }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BlueHeader(mode.title, onBack = onBack) {
            SearchField(
                value = field,
                onValueChange = { field = it },
                placeholder = mode.placeholder,
                onDone = {
                    when {
                        usingCurrent -> onUseCurrentLocation()
                        matches.isNotEmpty() -> onPlace(matches.first())
                    }
                },
                fieldModifier = Modifier.focusRequester(focus),
                leading = {
                    if (mode == PlaceSearchMode.DESTINATION) {
                        Icon(painterResource(R.drawable.ic_pickup), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        // Hollow circle, like the reference pickup field.
                        Box(Modifier.size(20.dp).border(4.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
                    }
                },
            )
        }

        LazyColumn(Modifier.fillMaxSize()) {
            if (query.isNotEmpty()) {
                if (matches.isEmpty()) {
                    item {
                        Text(
                            "No SafeRide locations match \"$query\".",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                items(matches, key = { it.id }) { place ->
                    ListRow(
                        title = place.name,
                        subtitle = place.category.title,
                        onClick = { onPlace(place) },
                        leading = { IconCircle(R.drawable.ic_pickup) },
                    )
                }
                return@LazyColumn
            }

            if (mode == PlaceSearchMode.PICKUP) {
                item {
                    Surface(shadowElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
                        Column {
                            ListRow(
                                title = CURRENT_LOCATION,
                                subtitle = currentLocationHint(currentLocation),
                                onClick = onUseCurrentLocation,
                                leading = { IconCircle(R.drawable.ic_navigation) },
                                showChevron = true,
                            )
                            ListRow(
                                title = "Set on Map",
                                subtitle = "Move the map to your exact spot",
                                onClick = onSetOnMap,
                                leading = { IconCircle(R.drawable.ic_pickup) },
                                showChevron = true,
                                showDivider = false,
                            )
                        }
                    }
                }
            }

            item { SectionHeader("Recent Places") }
            items(recentPlaces, key = { "recent_${it.id}" }) { place ->
                ListRow(
                    title = place.name,
                    onClick = { onPlace(place) },
                    leading = { IconCircle(R.drawable.ic_history) },
                    showDivider = place != recentPlaces.last(),
                )
            }

            item { SectionHeader("Explore") }
            items(PlaceCategory.entries.toList(), key = { it.name }) { category ->
                ListRow(
                    title = category.title,
                    onClick = { onExplore(category) },
                    leading = { CategoryBadge(category) },
                    showChevron = true,
                    showDivider = category != PlaceCategory.entries.last(),
                )
            }
            item { Box(Modifier.navigationBarsPadding()) }
        }
    }
}

/** One Explore category ("Parking Lots", "Off Campus Housing", ...). */
@Composable
fun ExploreScreen(
    category: PlaceCategory,
    onBack: () -> Unit,
    onPlace: (CampusLocation) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BlueHeader(category.title, onBack = onBack)
        LazyColumn(Modifier.fillMaxSize()) {
            val places = CampusLocations.inCategory(category)
            items(places, key = { it.id }) { place ->
                ListRow(
                    title = place.name,
                    subtitle = place.stopName,
                    onClick = { onPlace(place) },
                    leading = { CategoryBadge(category) },
                    showDivider = place != places.last(),
                )
            }
            item { Box(Modifier.navigationBarsPadding()) }
        }
    }
}

/** Gold circle badges from the reference ("P" for parking, a pin for housing). */
@Composable
private fun CategoryBadge(category: PlaceCategory) {
    when (category) {
        PlaceCategory.PARKING -> Box(
            Modifier.size(44.dp).background(GoldDark, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("P", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        }

        PlaceCategory.HOUSING -> IconCircle(R.drawable.ic_pickup, background = GoldDark, tint = Color.White)
        PlaceCategory.CAMPUS -> IconCircle(R.drawable.ic_school, background = AggieBlue, tint = Color.White)
    }
}

/** Where the van will meet a rider at [position], so they know what Enter will book. */
private fun currentLocationHint(position: LatLng?): String? = when {
    position == null -> null
    TripRules.isOnCampus(position) -> "Van meets you at ${TripRules.pickupSpotFor(position).stopName}"
    else -> "You're off campus. Rides must start on campus."
}
