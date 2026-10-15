package edu.ucdavis.saferide.ui.trip

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.ucdavis.saferide.R
import edu.ucdavis.saferide.data.CampusStatus
import edu.ucdavis.saferide.domain.Geo
import edu.ucdavis.saferide.domain.StatusText
import edu.ucdavis.saferide.domain.VanEstimate
import edu.ucdavis.saferide.ui.TripDraft
import edu.ucdavis.saferide.ui.components.IconCircle
import edu.ucdavis.saferide.ui.components.PrimaryButton
import edu.ucdavis.saferide.ui.components.RoundMapButton
import edu.ucdavis.saferide.ui.components.SystemBarIcons
import edu.ucdavis.saferide.ui.map.CampusMap
import edu.ucdavis.saferide.ui.map.MapCamera
import edu.ucdavis.saferide.ui.map.MapPin
import edu.ucdavis.saferide.ui.map.PinKind
import edu.ucdavis.saferide.ui.theme.AggieBlue
import edu.ucdavis.saferide.ui.theme.AggieBlueLight
import edu.ucdavis.saferide.ui.theme.AggieGold

private const val NOTE_MAX_LENGTH = 140

/** "Review your trip below": map of the pickup spot plus the trip summary and Continue. */
@Composable
fun ReviewTripScreen(
    trip: TripDraft,
    status: CampusStatus?,
    nearestVan: VanEstimate?,
    requesting: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onEditPassengers: () -> Unit,
    onSaveNote: (String) -> Unit,
    onContinue: () -> Unit,
) {
    SystemBarIcons(darkIcons = !isSystemInDarkTheme())
    val start = trip.start ?: return
    val pickup = trip.pickupSpot ?: return
    val dropoff = trip.destination ?: return
    var editingNote by remember { mutableStateOf(false) }

    // Only show the walk when the rider isn't already standing at the stop.
    val walks = Geo.distanceMeters(start.position, pickup.position) > 10
    val pins = buildList {
        add(MapPin("dropoff", dropoff.position, PinKind.DROPOFF))
        if (walks) add(MapPin("start", start.position, PinKind.START))
        add(MapPin("pickup", pickup.position, PinKind.PICKUP, label = "Pickup Spot"))
    }
    val camera = remember(start, pickup) {
        MapCamera.Fit(if (walks) listOf(start.position, pickup.position) else listOf(pickup.position), maxZoom = 17.5)
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.42f)) {
            CampusMap(
                camera = camera,
                vehicles = status?.vehicles.orEmpty(),
                pins = pins,
                walkingPath = if (walks) listOf(start.position, pickup.position) else emptyList(),
                showServiceArea = false,
                modifier = Modifier.fillMaxSize(),
            )
            RoundMapButton(R.drawable.ic_back, "Back", onBack, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp))
            RoundMapButton(R.drawable.ic_close, "Cancel trip", onClose, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp))
        }

        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            shadowElevation = 8.dp,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.6f),
        ) {
            Column {
                Box(
                    Modifier
                        .padding(top = 10.dp)
                        .size(width = 40.dp, height = 5.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .align(Alignment.CenterHorizontally),
                )
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        "Review your trip below",
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF242526), RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    SheetCard {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            // The walk only appears when the rider isn't already at the stop.
                            if (walks) {
                                TimelineRow(
                                    icon = { IconCircle(R.drawable.ic_walk, size = 36.dp) },
                                    above = null,
                                    below = Connector.DOTTED,
                                    label = "Start Location",
                                    value = start.label,
                                    divider = true,
                                )
                            }
                            TimelineRow(
                                icon = { PickupDot() },
                                above = if (walks) Connector.DOTTED else null,
                                below = Connector.SOLID,
                                label = "Pickup Spot",
                                value = pickup.stopName,
                                divider = true,
                            )
                            TimelineRow(
                                icon = { IconCircle(R.drawable.ic_pickup, background = timelineBlue(), tint = Color.White, size = 36.dp) },
                                above = Connector.SOLID,
                                below = null,
                                label = "Dropoff Spot",
                                value = dropoff.name,
                                divider = false,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    NearestVanCard(nearestVan, status)
                    Spacer(Modifier.height(12.dp))
                    EditableCard(
                        icon = R.drawable.ic_walk,
                        text = "${trip.passengers ?: 1} Passenger${if (trip.passengers == 1) "" else "s"}",
                        subtitle = if (trip.wheelchair) "Wheelchair accessible van" else null,
                        onEdit = onEditPassengers,
                    )
                    Spacer(Modifier.height(12.dp))
                    EditableCard(
                        icon = R.drawable.ic_notes,
                        text = trip.note.ifBlank { "Add note" },
                        subtitle = null,
                        onEdit = { editingNote = true },
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding()
                        .padding(16.dp),
                ) {
                    PrimaryButton("Continue", onClick = onContinue, loading = requesting)
                }
            }
        }
    }

    if (editingNote) {
        NoteDialog(initial = trip.note, onDismiss = { editingNote = false }, onSave = { onSaveNote(it); editingNote = false })
    }
}

@Composable
private fun SheetCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) { content() }
}

private enum class Connector { DOTTED, SOLID }

/** One stop in the trip timeline, with the dotted (walk) or solid (ride) line joining it to its neighbors. */
@Composable
private fun TimelineRow(
    icon: @Composable () -> Unit,
    above: Connector?,
    below: Connector?,
    label: String,
    value: String,
    divider: Boolean,
) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(40.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            ConnectorLine(above, Modifier.weight(1f))
            icon()
            ConnectorLine(below, Modifier.weight(1f))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Spacer(Modifier.height(14.dp))
            Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(14.dp))
            if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun ConnectorLine(style: Connector?, modifier: Modifier) {
    val blue = MaterialTheme.colorScheme.primary
    Canvas(modifier.width(6.dp)) {
        val x = size.width / 2
        when (style) {
            Connector.DOTTED -> {
                val step = 7.dp.toPx()
                var y = step / 2
                while (y < size.height) {
                    drawCircle(AggieGold, radius = 2.5.dp.toPx(), center = Offset(x, y))
                    y += step
                }
            }

            Connector.SOLID -> drawLine(blue, Offset(x, 0f), Offset(x, size.height), strokeWidth = 4.dp.toPx())
            null -> Unit
        }
    }
}

/** Navy ring with a white center, matching the map's pickup marker. */
@Composable
private fun PickupDot() {
    val blue = timelineBlue()
    Box(Modifier.size(36.dp).background(blue, CircleShape), contentAlignment = Alignment.Center) {
        Box(Modifier.size(18.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(6.dp).background(blue, CircleShape))
        }
    }
}

/** Aggie Blue, lightened in dark mode so the timeline stays visible on dark cards. */
@Composable
private fun timelineBlue(): Color = if (isSystemInDarkTheme()) AggieBlueLight else AggieBlue

/** New feature: how far the nearest van is before you commit to booking. */
@Composable
private fun NearestVanCard(estimate: VanEstimate?, status: CampusStatus?) {
    SheetCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            IconCircle(R.drawable.ic_van, background = AggieGold, tint = AggieBlue, size = 48.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    estimate?.let { StatusText.nearestVan(it, "your pickup spot") } ?: "Finding the nearest van…",
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (status != null) {
                    Text(StatusText.campusBanner(status), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun EditableCard(icon: Int, text: String, subtitle: String?, onEdit: () -> Unit) {
    SheetCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            IconCircle(icon, size = 48.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 2)
                if (subtitle != null) Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(onClick = onEdit, shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_edit), "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun NoteDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Note for your driver") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(NOTE_MAX_LENGTH) },
                placeholder = { Text("e.g. I'm wearing a red jacket") },
                supportingText = { Text("${text.length}/$NOTE_MAX_LENGTH") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
