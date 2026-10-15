package edu.ucdavis.saferide.domain

import edu.ucdavis.saferide.data.LatLng
import edu.ucdavis.saferide.data.Vehicle

/** How far the closest van is from a pickup point, shown before booking. */
data class VanEstimate(val vehicle: Vehicle, val minutes: Int)

object NearestVan {
    fun find(vehicles: List<Vehicle>, pickup: LatLng): VanEstimate? =
        vehicles
            .map { it to Route.streetPath(it.position, pickup).lengthMeters }
            .minByOrNull { (_, meters) -> meters }
            ?.let { (van, meters) -> VanEstimate(van, Route.etaMinutes(meters)) }
}
