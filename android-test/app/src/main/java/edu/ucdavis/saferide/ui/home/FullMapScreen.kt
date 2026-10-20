package edu.ucdavis.saferide.ui.home

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.ucdavis.saferide.R
import edu.ucdavis.saferide.data.CampusLocations
import edu.ucdavis.saferide.data.CampusStatus
import edu.ucdavis.saferide.data.LatLng
import edu.ucdavis.saferide.ui.components.RoundMapButton
import edu.ucdavis.saferide.ui.components.SystemBarIcons
import edu.ucdavis.saferide.ui.map.CampusMap
import edu.ucdavis.saferide.ui.map.MapCamera
import edu.ucdavis.saferide.ui.map.MapPin
import edu.ucdavis.saferide.ui.map.PinKind

/** The expanded service-area map. */
@Composable
fun FullMapScreen(status: CampusStatus?, myLocation: LatLng?, onClose: () -> Unit) {
    SystemBarIcons(darkIcons = !isSystemInDarkTheme())
    Box(Modifier.fillMaxSize()) {
        CampusMap(
            camera = MapCamera.Focus(CampusLocations.campusCenter, 13.8),
            vehicles = status?.vehicles.orEmpty(),
            pins = listOfNotNull(myLocation?.let { MapPin("me", it, PinKind.CURRENT) }),
            modifier = Modifier.fillMaxSize(),
        )
        RoundMapButton(
            R.drawable.ic_close,
            "Close map",
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp),
        )
    }
}
