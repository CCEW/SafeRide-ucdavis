package edu.ucdavis.saferide.ui.ride

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucdavis.saferide.R
import edu.ucdavis.saferide.data.CampusStatus
import edu.ucdavis.saferide.data.RideRequest
import edu.ucdavis.saferide.data.RideStatus
import edu.ucdavis.saferide.domain.Geo
import edu.ucdavis.saferide.domain.Proximity
import edu.ucdavis.saferide.domain.StatusText
import edu.ucdavis.saferide.ui.TripDraft
import edu.ucdavis.saferide.ui.components.IconCircle
import edu.ucdavis.saferide.ui.components.PrimaryButton
import edu.ucdavis.saferide.ui.components.SystemBarIcons
import edu.ucdavis.saferide.ui.map.CampusMap
import edu.ucdavis.saferide.ui.map.MapCamera
import edu.ucdavis.saferide.ui.map.MapHighlight
import edu.ucdavis.saferide.ui.map.MapPin
import edu.ucdavis.saferide.ui.map.PinKind
import edu.ucdavis.saferide.ui.theme.AggieBlue
import edu.ucdavis.saferide.ui.theme.AggieGold
import edu.ucdavis.saferide.ui.theme.ArrivedGreen
import kotlin.math.roundToInt

/** Live ride: queue position, Van #4 on the map, the 100 ft arrival alert, then the ride to the drop-off. */
@Composable
fun TrackingScreen(
    ride: RideRequest?,
    trip: TripDraft,
    status: CampusStatus?,
    onBoard: () -> Unit,
    onCancel: () -> Unit,
) {
    SystemBarIcons(darkIcons = !isSystemInDarkTheme())
    ride ?: return
    val context = LocalContext.current
    var confirmCancel by remember { mutableStateOf(false) }
    val onBoardOrDone = ride.status == RideStatus.ON_BOARD || ride.status == RideStatus.COMPLETED

    // Back asks before cancelling; once on board there's nothing to cancel.
    BackHandler { if (!onBoardOrDone) confirmCancel = true }

    // Buzz once per ride when the van enters the arrival radius (not again after rotation).
    var buzzed by rememberSaveable(ride.id) { mutableStateOf(false) }
    LaunchedEffect(ride.status) {
        if (ride.status == RideStatus.ARRIVED && !buzzed) {
            Haptics.arrival(context)
            buzzed = true
        }
    }

    val pickup = ride.pickup.position
    val start = trip.start?.position
    val walks = start != null && !onBoardOrDone && Geo.distanceMeters(start, pickup) > 10
    val pins = buildList {
        add(MapPin("dropoff", ride.dropoff.position, PinKind.DROPOFF))
        if (walks) add(MapPin("start", start!!, PinKind.START))
        if (!onBoardOrDone) add(MapPin("pickup", pickup, PinKind.PICKUP, label = "Pickup Spot"))
    }
    // Reframe only when the stage changes, not on every van tick.
    val camera = remember(ride.status) {
        when (ride.status) {
            RideStatus.QUEUED -> MapCamera.Focus(pickup, 17.0)
            RideStatus.ASSIGNED -> MapCamera.Fit(ride.route + pickup, maxZoom = 17.0)
            RideStatus.ARRIVED -> MapCamera.Focus(pickup, 18.0)
            RideStatus.ON_BOARD, RideStatus.COMPLETED -> MapCamera.Fit(ride.route, maxZoom = 16.0)
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CampusMap(
            camera = camera,
            vehicles = ride.assignedVehicle?.let(::listOf) ?: status?.vehicles.orEmpty(),
            pins = pins,
            walkingPath = if (walks) listOf(start!!, pickup) else emptyList(),
            routePath = if (ride.status == RideStatus.ASSIGNED || onBoardOrDone) ride.route else emptyList(),
            // "Map: Highlights pickup point" with the exact radius that triggers the alert.
            highlight = if (onBoardOrDone) null else MapHighlight(pickup, Proximity.ARRIVAL_RADIUS_METERS),
            showServiceArea = false,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.52f),
        )

        AnimatedVisibility(
            visible = ride.status == RideStatus.ARRIVED,
            enter = slideInVertically { -it } + fadeIn(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(12.dp),
        ) {
            ArrivalBanner()
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            shadowElevation = 12.dp,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.52f),
        ) {
            RideSheet(ride, status, onBoard = onBoard, onCancel = { confirmCancel = true })
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("Cancel this ride?") },
            text = { Text("You'll lose your place in line.") },
            confirmButton = { TextButton(onClick = { confirmCancel = false; onCancel() }) { Text("Cancel Ride") } },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep Ride") } },
        )
    }
}

/** The prominent "Driver Arrived" alert. Van and stop details stay in the sheet below. */
@Composable
private fun ArrivalBanner() {
    Surface(color = ArrivedGreen, shape = RoundedCornerShape(18.dp), shadowElevation = 10.dp, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(18.dp)) {
            IconCircle(R.drawable.ic_check, background = Color.White, tint = ArrivedGreen, size = 36.dp)
            Spacer(Modifier.width(12.dp))
            Text("Driver Arrived", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RideSheet(ride: RideRequest, status: CampusStatus?, onBoard: () -> Unit, onCancel: () -> Unit) {
    val van = ride.assignedVehicle
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        Box(
            Modifier
                .padding(top = 10.dp)
                .size(width = 40.dp, height = 5.dp)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(50))
                .align(Alignment.CenterHorizontally),
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(ride)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(headline(ride), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(subline(ride, status), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // Only during the ride itself; while waiting, the headline already says the place in line or the van's status.
            if (ride.status == RideStatus.ON_BOARD) {
                Spacer(Modifier.height(14.dp))
                Text(
                    StatusText.queueCard(ride),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Stepper(ride.status)
            Spacer(Modifier.height(16.dp))
            DetailRow(R.drawable.ic_pickup, "Pickup", ride.pickup.stopName)
            DetailRow(R.drawable.ic_dropoff, "Drop-off", ride.dropoff.name)
            if (van != null) DetailRow(R.drawable.ic_van, "Your van", "${van.displayName} • ${van.licensePlate}")
            Spacer(Modifier.height(10.dp))
            Row {
                Icon(painterResource(R.drawable.ic_info), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Your place in line only moves forward. We only say your driver has arrived once the van is within 100 ft of your stop.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            when (ride.status) {
                RideStatus.ARRIVED -> PrimaryButton("I'm in the Van", onClick = onBoard)
                RideStatus.QUEUED, RideStatus.ASSIGNED -> OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Cancel Ride", fontSize = 17.sp) }
                RideStatus.ON_BOARD, RideStatus.COMPLETED -> Unit
            }
        }
    }
}

private fun headline(ride: RideRequest): String = when (ride.status) {
    RideStatus.QUEUED -> "You're #${ride.queuePosition} in line"
    RideStatus.ASSIGNED -> "${ride.assignedVehicle?.displayName ?: "Your van"} is on the way"
    RideStatus.ARRIVED -> "Your van is here"
    RideStatus.ON_BOARD -> "Riding to ${ride.dropoff.name}"
    RideStatus.COMPLETED -> "You've arrived"
}

private fun subline(ride: RideRequest, status: CampusStatus?): String = when (ride.status) {
    RideStatus.QUEUED -> StatusText.queueEstimate(ride.queuePosition, status?.estimatedWaitMinutes ?: 12)
    RideStatus.ASSIGNED -> {
        val meters = ride.metersToPickup?.roundToInt() ?: 0
        "Driver ~${ride.etaMinutes ?: 1} min • $meters m from your stop"
    }
    RideStatus.ARRIVED -> "Meet ${ride.assignedVehicle?.displayName ?: "your van"} at the curb"
    RideStatus.ON_BOARD -> "About ${ride.etaMinutes ?: 1} min to go"
    RideStatus.COMPLETED -> "Trip complete"
}

@Composable
private fun StatusBadge(ride: RideRequest) {
    val (background, content) = when (ride.status) {
        RideStatus.QUEUED -> AggieGold to AggieBlue
        RideStatus.ARRIVED, RideStatus.COMPLETED -> ArrivedGreen to Color.White
        else -> AggieBlue to Color.White
    }
    Box(Modifier.size(60.dp).background(background, CircleShape), contentAlignment = Alignment.Center) {
        when (ride.status) {
            RideStatus.QUEUED -> Text("#${ride.queuePosition}", color = content, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            RideStatus.ARRIVED, RideStatus.COMPLETED ->
                Icon(painterResource(R.drawable.ic_check), null, tint = content, modifier = Modifier.size(32.dp))
            else -> Icon(painterResource(R.drawable.ic_van), null, tint = content, modifier = Modifier.size(30.dp))
        }
    }
}

/** In line → Van assigned → Arrived. */
@Composable
private fun Stepper(status: RideStatus) {
    val reached = when (status) {
        RideStatus.QUEUED -> 0
        RideStatus.ASSIGNED -> 1
        else -> 2
    }
    val labels = listOf("In line", "Van assigned", "Arrived")
    val done = MaterialTheme.colorScheme.primary
    val todo = MaterialTheme.colorScheme.outlineVariant
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            labels.indices.forEach { i ->
                Box(Modifier.size(16.dp).background(if (i <= reached) done else todo, CircleShape))
                if (i < labels.lastIndex) {
                    Box(Modifier.weight(1f).height(4.dp).background(if (i < reached) done else todo))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, label ->
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (i == reached) FontWeight.Bold else FontWeight.Normal,
                    color = if (i <= reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(96.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailRow(icon: Int, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        IconCircle(icon, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** "Trip Complete" with End Trip, which resets the app back to the home map. */
@Composable
fun TripCompleteScreen(ride: RideRequest?, onEndTrip: () -> Unit) {
    SystemBarIcons(darkIcons = !isSystemInDarkTheme())
    BackHandler(onBack = onEndTrip)
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(80.dp))
        Box(Modifier.size(110.dp).background(ArrivedGreen, CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_check), null, tint = Color.White, modifier = Modifier.size(64.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text("Trip Complete", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(10.dp))
        Text(
            ride?.let { "You've arrived at ${it.dropoff.name}. Thanks for riding SafeRide!" } ?: "Thanks for riding SafeRide!",
            fontSize = 18.sp,
            lineHeight = 26.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (ride != null) {
            Spacer(Modifier.height(28.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    DetailRow(R.drawable.ic_pickup, "Picked up at", ride.pickup.stopName)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailRow(R.drawable.ic_dropoff, "Dropped off at", ride.dropoff.name)
                    ride.assignedVehicle?.let {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DetailRow(R.drawable.ic_van, "Van", "${it.displayName} • ${it.licensePlate}")
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        PrimaryButton("End Trip", onClick = onEndTrip, modifier = Modifier.padding(bottom = 20.dp))
    }
}
