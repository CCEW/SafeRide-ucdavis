package edu.ucdavis.saferide.domain

import edu.ucdavis.saferide.data.LatLng

/**
 * The "Driver Arrived" rule. The real app fires the alert before the driver stops;
 * here it fires only once the van is inside a strict 100-foot radius of the pickup point.
 */
object Proximity {
    /** 100 feet. */
    const val ARRIVAL_RADIUS_METERS = 30.48

    fun hasArrived(vehicle: LatLng, pickup: LatLng): Boolean =
        Geo.distanceMeters(vehicle, pickup) <= ARRIVAL_RADIUS_METERS
}
