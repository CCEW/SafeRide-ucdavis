package edu.ucdavis.saferide.ui.places

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucdavis.saferide.R
import edu.ucdavis.saferide.data.CampusLocations
import edu.ucdavis.saferide.data.CampusStatus
import edu.ucdavis.saferide.data.LatLng
import edu.ucdavis.saferide.domain.Geo
import edu.ucdavis.saferide.domain.TripRules
import edu.ucdavis.saferide.ui.components.IconCircle
import edu.ucdavis.saferide.ui.components.PrimaryButton
import edu.ucdavis.saferide.ui.components.RoundMapButton
import edu.ucdavis.saferide.ui.components.SystemBarIcons
import edu.ucdavis.saferide.ui.map.CampusMap
import edu.ucdavis.saferide.ui.map.MapCamera
import edu.ucdavis.saferide.ui.map.MapPin
import edu.ucdavis.saferide.ui.map.PinKind
import edu.ucdavis.saferide.ui.theme.AggieBlue
import kotlin.math.roundToInt

/** "Current Location / Set on Map": drag the map under a fixed pin to choose where you are. */
@Composable
fun SetOnMapScreen(status: CampusStatus?, myLocation: LatLng?, onBack: () -> Unit, onConfirm: (LatLng) -> Unit) {
    SystemBarIcons(darkIcons = !isSystemInDarkTheme())
    // Start on the rider's GPS position, or on campus without a fix.
    val start = myLocation ?: CampusLocations.campusCenter
    var center by remember { mutableStateOf(start) }
    val camera = remember { MapCamera.Focus(start, 17.0) }

    val onCampus = TripRules.isOnCampus(center)
    val spot = TripRules.pickupSpotFor(center)
    val walkMeters = Geo.distanceMeters(center, spot.position).roundToInt()

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            CampusMap(
                camera = camera,
                vehicles = status?.vehicles.orEmpty(),
                pins = listOfNotNull(myLocation?.let { MapPin("me", it, PinKind.CURRENT) }),
                onCenterChanged = { center = it },
                // At street zoom the gold fill would tint the whole map.
                showServiceArea = false,
                modifier = Modifier.fillMaxSize(),
            )
            // Fixed pin: its tip marks the map center.
            Icon(
                painterResource(R.drawable.ic_pickup),
                contentDescription = "Pickup pin",
                tint = AggieBlue,
                modifier = Modifier.align(Alignment.Center).size(48.dp).offset(y = (-22).dp),
            )
            RoundMapButton(
                R.drawable.ic_back,
                "Back",
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp),
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp)) {
                Text("Where are you?", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text("Drag the map to place the pin where you are.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconCircle(if (onCampus) R.drawable.ic_walk else R.drawable.ic_warning, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(TripRules.startLabelFor(center, myLocation), fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            if (onCampus) {
                                "Pickup spot: ${spot.stopName} • ${walkMeters} m walk"
                            } else {
                                "Outside campus. Rides must start on-campus."
                            },
                            fontSize = 14.sp,
                            color = if (onCampus) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Set Pickup Here", onClick = { onConfirm(center) })
            }
        }
    }
}
