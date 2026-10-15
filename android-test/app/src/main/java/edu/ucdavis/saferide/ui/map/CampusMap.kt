package edu.ucdavis.saferide.ui.map

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import edu.ucdavis.saferide.data.CampusLocations
import edu.ucdavis.saferide.data.LatLng
import edu.ucdavis.saferide.data.Vehicle
import edu.ucdavis.saferide.ui.theme.AggieBlue
import edu.ucdavis.saferide.ui.theme.AggieGold
import edu.ucdavis.saferide.ui.theme.GoldDark
import org.osmdroid.config.Configuration
import org.osmdroid.events.DelayedMapListener
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

/** Where the camera should point. */
sealed interface MapCamera {
    data class Focus(val center: LatLng, val zoom: Double) : MapCamera
    /** Frames all [points], never zooming in past [maxZoom]. */
    data class Fit(val points: List<LatLng>, val maxZoom: Double = 17.5) : MapCamera
}

enum class PinKind { CURRENT, START, PICKUP, DROPOFF }

data class MapPin(val id: String, val position: LatLng, val kind: PinKind, val label: String? = null)

data class MapHighlight(val center: LatLng, val radiusMeters: Double)

/**
 * The campus map: osmdroid's native MapView (no WebView, no API key) showing OpenStreetMap tiles,
 * color-filtered to the muted light-grey look of the current app's Google map.
 */
@Composable
fun CampusMap(
    camera: MapCamera,
    modifier: Modifier = Modifier,
    vehicles: List<Vehicle> = emptyList(),
    pins: List<MapPin> = emptyList(),
    walkingPath: List<LatLng> = emptyList(),
    /** The van's driving route, drawn as a solid Aggie Blue line. */
    routePath: List<LatLng> = emptyList(),
    /** A circle around a point, e.g. the 100 ft arrival radius around the pickup spot. */
    highlight: MapHighlight? = null,
    showServiceArea: Boolean = true,
    onCenterChanged: ((LatLng) -> Unit)? = null,
) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val holder = remember(dark) { MapHolder(context, dark) }
    val centerCallback by rememberUpdatedState(onCenterChanged)

    // osmdroid needs to pause tile loading with the screen and release resources when it leaves.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, holder) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> holder.map.onResume()
                Lifecycle.Event.ON_PAUSE -> holder.map.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        holder.onCenterChanged = { centerCallback?.invoke(it) }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            holder.map.onDetach()
        }
    }

    LaunchedEffect(holder, camera) { holder.applyCamera(camera) }

    Box(modifier.clipToBounds()) {
        AndroidView(
            factory = { holder.map },
            // osmdroid draws tiles and the service-area fill past its own edges unless clipped.
            modifier = Modifier.fillMaxSize(),
            update = { holder.render(vehicles, pins, walkingPath, routePath, highlight, showServiceArea) },
        )
        // Required attribution for OpenStreetMap tiles.
        Text(
            "© OpenStreetMap contributors",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

/** Owns the MapView and its overlays so markers are updated in place instead of recreated. */
private class MapHolder(private val context: Context, dark: Boolean) {

    val map: MapView
    var onCenterChanged: ((LatLng) -> Unit)? = null

    private val density = context.resources.displayMetrics.density
    private val serviceArea: Polygon
    private val walking: Polyline
    private val route: Polyline
    private val radius: Polygon
    private var radiusKey: MapHighlight? = null
    private val vanMarkers = mutableMapOf<Int, Marker>()
    private val pinMarkers = mutableMapOf<String, Pair<MapPin, Marker>>()

    init {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            // Tile servers require an identifying user agent.
            userAgentValue = context.packageName
        }
        map = MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            overlayManager.tilesOverlay.setColorFilter(if (dark) darkFilter() else lightFilter())
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 11.0
            maxZoomLevel = 19.0
            controller.setZoom(14.8)
            controller.setCenter(CampusLocations.campusCenter.toGeoPoint())
            addMapListener(
                DelayedMapListener(object : MapListener {
                    override fun onScroll(event: ScrollEvent?) = notifyCenter()
                    override fun onZoom(event: ZoomEvent?) = notifyCenter()
                }, 150),
            )
        }
        serviceArea = Polygon(map).apply {
            points = CampusLocations.serviceArea.map { it.toGeoPoint() }
            fillPaint.color = AggieGold.copy(alpha = 0.28f).toArgb()
            outlinePaint.color = GoldDark.toArgb()
            outlinePaint.strokeWidth = 2.5f * density
            infoWindow = null
        }
        walking = Polyline(map).apply {
            outlinePaint.color = AggieGold.toArgb()
            outlinePaint.strokeWidth = 5f * density
            outlinePaint.strokeCap = Paint.Cap.ROUND
            // Near-zero dashes with round caps render as dots, like the reference walking path.
            outlinePaint.pathEffect = DashPathEffect(floatArrayOf(0.1f, 11f * density), 0f)
            infoWindow = null
        }
        route = Polyline(map).apply {
            outlinePaint.color = AggieBlue.copy(alpha = 0.85f).toArgb()
            outlinePaint.strokeWidth = 6f * density
            outlinePaint.strokeCap = Paint.Cap.ROUND
            outlinePaint.strokeJoin = Paint.Join.ROUND
            infoWindow = null
        }
        radius = Polygon(map).apply {
            fillPaint.color = AggieGold.copy(alpha = 0.25f).toArgb()
            outlinePaint.color = GoldDark.toArgb()
            outlinePaint.strokeWidth = 2f * density
            infoWindow = null
        }
    }

    private fun notifyCenter(): Boolean {
        val center = map.mapCenter
        onCenterChanged?.invoke(LatLng(center.latitude, center.longitude))
        return false
    }

    fun render(
        vehicles: List<Vehicle>,
        pins: List<MapPin>,
        walkingPath: List<LatLng>,
        routePath: List<LatLng>,
        highlight: MapHighlight?,
        showServiceArea: Boolean,
    ) {
        map.overlays.clear()
        if (showServiceArea) map.overlays.add(serviceArea)

        if (highlight != null) {
            if (highlight != radiusKey) {
                radius.points = Polygon.pointsAsCircle(highlight.center.toGeoPoint(), highlight.radiusMeters)
                radiusKey = highlight
            }
            map.overlays.add(radius)
        }

        if (routePath.size >= 2) {
            route.setPoints(routePath.map { it.toGeoPoint() })
            map.overlays.add(route)
        }

        if (walkingPath.size >= 2) {
            walking.setPoints(walkingPath.map { it.toGeoPoint() })
            map.overlays.add(walking)
        }

        pinMarkers.keys.retainAll(pins.map { it.id }.toSet())
        // Draw order: drop-off, start, pickup, then the current-location dot on top.
        pins.sortedBy { it.kind.drawOrder() }.forEach { pin ->
            val existing = pinMarkers[pin.id]
            val marker = if (existing != null && existing.first.kind == pin.kind && existing.first.label == pin.label) {
                existing.second
            } else {
                marker(iconFor(pin)).also { pinMarkers[pin.id] = pin to it }
            }
            marker.position = pin.position.toGeoPoint()
            map.overlays.add(marker)
        }

        // Vans draw last so a van pulling into the pickup spot stays visible on top of it.
        val liveVans = vehicles.map { it.number }.toSet()
        vanMarkers.keys.retainAll(liveVans)
        vehicles.forEach { van ->
            val marker = vanMarkers.getOrPut(van.number) { marker(MarkerIcons.van(context, van.number)) }
            marker.position = van.position.toGeoPoint()
            map.overlays.add(marker)
        }
        map.invalidate()
    }

    fun applyCamera(camera: MapCamera) {
        if (map.width == 0 || map.height == 0) {
            map.addOnFirstLayoutListener { _, _, _, _, _ -> applyCamera(camera) }
            return
        }
        when (camera) {
            is MapCamera.Focus -> {
                map.controller.setZoom(camera.zoom)
                map.controller.setCenter(camera.center.toGeoPoint())
            }

            is MapCamera.Fit -> {
                val distinct = camera.points.distinct()
                if (distinct.size < 2) {
                    distinct.firstOrNull()?.let {
                        map.controller.setZoom(camera.maxZoom)
                        map.controller.setCenter(it.toGeoPoint())
                    }
                    return
                }
                map.zoomToBoundingBox(
                    BoundingBox.fromGeoPoints(distinct.map { it.toGeoPoint() }).increaseByScale(1.3f),
                    false,
                    (56 * density).toInt(),
                )
                if (map.zoomLevelDouble > camera.maxZoom) map.controller.setZoom(camera.maxZoom)
            }
        }
    }

    private fun iconFor(pin: MapPin): MarkerIcon = when (pin.kind) {
        PinKind.CURRENT -> MarkerIcons.currentLocation(context)
        PinKind.START -> MarkerIcons.start(context)
        PinKind.PICKUP -> MarkerIcons.pickup(context, pin.label)
        PinKind.DROPOFF -> MarkerIcons.dropoff(context)
    }

    private fun marker(icon: MarkerIcon) = Marker(map).apply {
        this.icon = icon.drawable
        setAnchor(Marker.ANCHOR_CENTER, icon.anchorV)
        // No info-window popups; screens show details instead.
        infoWindow = null
        setOnMarkerClickListener { _, _ -> true }
    }

    private fun PinKind.drawOrder() = when (this) {
        PinKind.DROPOFF -> 0
        PinKind.START -> 1
        PinKind.PICKUP -> 2
        PinKind.CURRENT -> 3
    }

    companion object {
        /** Mostly desaturated and lifted toward white, so gold, blue, and markers stand out. */
        fun lightFilter() = ColorMatrixColorFilter(
            ColorMatrix().apply {
                setSaturation(0.15f)
                postConcat(ColorMatrix(floatArrayOf(
                    0.78f, 0f, 0f, 0f, 58f,
                    0f, 0.78f, 0f, 0f, 58f,
                    0f, 0f, 0.78f, 0f, 60f,
                    0f, 0f, 0f, 1f, 0f,
                )))
            },
        )

        /** Desaturated and inverted for dark mode. */
        fun darkFilter() = ColorMatrixColorFilter(
            ColorMatrix().apply {
                setSaturation(0.1f)
                postConcat(ColorMatrix(floatArrayOf(
                    -0.72f, 0f, 0f, 0f, 200f,
                    0f, -0.72f, 0f, 0f, 205f,
                    0f, 0f, -0.72f, 0f, 215f,
                    0f, 0f, 0f, 1f, 0f,
                )))
            },
        )
    }
}

private fun LatLng.toGeoPoint() = GeoPoint(latitude, longitude)
