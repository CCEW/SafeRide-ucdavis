package edu.ucdavis.saferide.domain

import edu.ucdavis.saferide.data.LatLng
import edu.ucdavis.saferide.data.RoadRoutes
import kotlin.math.ceil
import kotlin.math.max

/** A driving path made of straight segments. Positions are looked up by distance traveled. */
class Route(val points: List<LatLng>) {
    init {
        require(points.size >= 2) { "A route needs at least two points" }
    }

    private val segmentLengths = points.zipWithNext { a, b -> Geo.distanceMeters(a, b) }

    val lengthMeters: Double = segmentLengths.sum()

    fun positionAt(metersTraveled: Double): LatLng {
        var remaining = metersTraveled.coerceIn(0.0, lengthMeters)
        segmentLengths.forEachIndexed { i, length ->
            if (remaining <= length) {
                val fraction = if (length == 0.0) 1.0 else remaining / length
                return Geo.interpolate(points[i], points[i + 1], fraction)
            }
            remaining -= length
        }
        return points.last()
    }

    /** The part of the route still ahead after [metersTraveled]: the current position, then the remaining corners. */
    fun remainingFrom(metersTraveled: Double): List<LatLng> {
        val ahead = mutableListOf(positionAt(metersTraveled))
        var covered = 0.0
        segmentLengths.forEachIndexed { i, length ->
            covered += length
            if (covered > metersTraveled) ahead += points[i + 1]
        }
        return ahead
    }

    companion object {
        /** Below this distance the van starts from further out so the approach is visible on the map. */
        const val MIN_APPROACH_METERS = 300.0

        /** Rough campus driving speed (about 20 km/h) used for the driver ETA. */
        const val VAN_SPEED_METERS_PER_SECOND = 5.5

        /**
         * An L-shaped, street-like path: first along [from]'s latitude, then along [to]'s longitude.
         */
        fun streetPath(from: LatLng, to: LatLng): Route =
            Route(listOf(from, LatLng(from.latitude, to.longitude), to))

        /** The stored road path between two points when there is one, otherwise [streetPath]. */
        fun drive(from: LatLng, to: LatLng): Route =
            RoadRoutes.between(from, to)?.let(::Route) ?: streetPath(from, to)

        /** The path the assigned van drives to the pickup point. */
        fun toPickup(vehicle: LatLng, pickup: LatLng): Route {
            RoadRoutes.between(vehicle, pickup)?.let { return Route(it) }
            val start = if (Geo.distanceMeters(vehicle, pickup) >= MIN_APPROACH_METERS) {
                vehicle
            } else {
                // Too close to animate; back off about 400 m south-west of the pickup.
                LatLng(pickup.latitude - 0.0025, pickup.longitude - 0.0025)
            }
            return streetPath(start, pickup)
        }

        /** Whole minutes to drive [meters], never less than 1 while the van is still moving. */
        fun etaMinutes(meters: Double): Int =
            max(1, ceil(meters / VAN_SPEED_METERS_PER_SECOND / 60.0).toInt())
    }
}
