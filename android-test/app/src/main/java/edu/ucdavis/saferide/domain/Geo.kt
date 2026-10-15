package edu.ucdavis.saferide.domain

import edu.ucdavis.saferide.data.LatLng
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** Great-circle (haversine) distance in meters. */
    fun distanceMeters(a: LatLng, b: LatLng): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).let { it * it } +
            cos(lat1) * cos(lat2) * sin(dLng / 2).let { it * it }
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h))
    }

    /** Ray-casting point-in-polygon test; fine for small, city-sized polygons. */
    fun contains(polygon: List<LatLng>, point: LatLng): Boolean {
        var inside = false
        var j = polygon.lastIndex
        for (i in polygon.indices) {
            val a = polygon[i]
            val b = polygon[j]
            val crosses = (a.latitude > point.latitude) != (b.latitude > point.latitude) &&
                point.longitude < (b.longitude - a.longitude) * (point.latitude - a.latitude) /
                (b.latitude - a.latitude) + a.longitude
            if (crosses) inside = !inside
            j = i
        }
        return inside
    }

    /** Linear interpolation; accurate enough over the few hundred meters a van drives on campus. */
    fun interpolate(from: LatLng, to: LatLng, fraction: Double): LatLng = LatLng(
        latitude = from.latitude + (to.latitude - from.latitude) * fraction,
        longitude = from.longitude + (to.longitude - from.longitude) * fraction,
    )
}
