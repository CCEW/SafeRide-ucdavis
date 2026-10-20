package edu.ucdavis.saferide.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucdavis.saferide.R
import edu.ucdavis.saferide.data.CampusLocations
import edu.ucdavis.saferide.data.CampusStatus
import edu.ucdavis.saferide.data.LatLng
import edu.ucdavis.saferide.domain.StatusText
import edu.ucdavis.saferide.ui.MainTab
import edu.ucdavis.saferide.ui.SafeRideUiState
import edu.ucdavis.saferide.ui.components.IconCircle
import edu.ucdavis.saferide.ui.components.ListRow
import edu.ucdavis.saferide.ui.components.PrimaryButton
import edu.ucdavis.saferide.ui.components.RoundMapButton
import edu.ucdavis.saferide.ui.components.SectionHeader
import edu.ucdavis.saferide.ui.components.SystemBarIcons
import edu.ucdavis.saferide.ui.map.CampusMap
import edu.ucdavis.saferide.ui.map.MapCamera
import edu.ucdavis.saferide.ui.map.MapPin
import edu.ucdavis.saferide.ui.map.PinKind
import edu.ucdavis.saferide.ui.theme.AggieBlue
import edu.ucdavis.saferide.ui.theme.ArrivedGreen

/** The Home and Settings tabs with the bottom navigation bar from the reference. */
@Composable
fun MainScreen(
    state: SafeRideUiState,
    onSelectTab: (MainTab) -> Unit,
    onRequestRide: () -> Unit,
    onExpandMap: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.weight(1f)) {
            when (state.tab) {
                MainTab.HOME -> HomeTab(state.campusStatus, state.myLocation, onRequestRide, onExpandMap)
                MainTab.SETTINGS -> SettingsTab(onSignOut)
            }
        }
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            val colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            NavigationBarItem(
                selected = state.tab == MainTab.HOME,
                onClick = { onSelectTab(MainTab.HOME) },
                icon = { Icon(painterResource(R.drawable.ic_home), null) },
                label = { Text("Home", fontSize = 15.sp, fontWeight = FontWeight.Medium) },
                colors = colors,
            )
            NavigationBarItem(
                selected = state.tab == MainTab.SETTINGS,
                onClick = { onSelectTab(MainTab.SETTINGS) },
                icon = { Icon(painterResource(R.drawable.ic_person), null) },
                label = { Text("Settings", fontSize = 15.sp, fontWeight = FontWeight.Medium) },
                colors = colors,
            )
        }
    }
}

@Composable
private fun HomeTab(status: CampusStatus?, myLocation: LatLng?, onRequestRide: () -> Unit, onExpandMap: () -> Unit) {
    SystemBarIcons(darkIcons = false)
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(AggieBlue)
                .statusBarsPadding()
                .height(64.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("UC Davis Safe Ride", color = Color.White, style = MaterialTheme.typography.titleLarge)
        }

        LiveStatusBanner(status)

        // Service Hours & Area card.
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            Text("Service Hours & Area", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(6.dp))
            Text(CampusLocations.SERVICE_HOURS, fontSize = 16.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text(CampusLocations.VIP_HOURS, fontSize = 16.sp, lineHeight = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Box(Modifier.fillMaxWidth().weight(1f)) {
            CampusMap(
                // Centered on campus so the live vans are readable; the gold service area surrounds it.
                camera = MapCamera.Focus(LatLng(38.5440, -121.7480), 13.4),
                vehicles = status?.vehicles.orEmpty(),
                pins = listOfNotNull(myLocation?.let { MapPin("me", it, PinKind.CURRENT) }),
                modifier = Modifier.fillMaxSize(),
            )
            RoundMapButton(
                R.drawable.ic_fullscreen,
                "Expand map",
                onClick = onExpandMap,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            )
        }

        // The one way into booking: Request SafeRide → "Where to?".
        PrimaryButton(
            "Request SafeRide",
            onClick = onRequestRide,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
        )
    }
}

/** New feature: live system status before booking. */
@Composable
private fun LiveStatusBanner(status: CampusStatus?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(Modifier.size(10.dp).background(if (status != null) ArrivedGreen else Color.Gray, CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(
            status?.let(StatusText::campusBanner) ?: "Checking SafeRide vans…",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

/** No reference screenshot exists for Settings, so it stays minimal (no profile editing in scope). */
@Composable
private fun SettingsTab(onSignOut: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        edu.ucdavis.saferide.ui.components.BlueHeader("Settings")
        SectionHeader("Account")
        ListRow(
            title = CampusLocations.RIDER_NAME,
            subtitle = "Signed in with UC Davis",
            onClick = {},
            leading = { IconCircle(R.drawable.ic_person) },
        )
        ListRow(
            title = "Sign Out",
            onClick = onSignOut,
            leading = { IconCircle(R.drawable.ic_close) },
            showDivider = false,
        )
        SectionHeader("About")
        ListRow(
            title = "UC Davis Safe Ride prototype",
            subtitle = "ECS 191 class project • Version 1.0",
            onClick = {},
            leading = { IconCircle(R.drawable.ic_info) },
            showDivider = false,
        )
    }
}
