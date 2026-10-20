package edu.ucdavis.saferide.domain

import edu.ucdavis.saferide.data.CampusLocation
import edu.ucdavis.saferide.data.CampusLocations
import edu.ucdavis.saferide.data.LatLng
import edu.ucdavis.saferide.data.PlaceCategory

/** SafeRide's booking rules. */
object TripRules {

    const val MAX_PASSENGERS = 6

    /** How close to the rider's position still counts as "Current Location". */
    private const val CURRENT_LOCATION_TOLERANCE_METERS = 15.0

    const val OFF_CAMPUS_PICKUP_MESSAGE =
        "Sorry! We do not offer transportation between these locations. Rides must start on-campus."
    const val OUTSIDE_SERVICE_AREA_MESSAGE =
        "Sorry! We do not offer transportation between these locations. Drop-offs must be on campus or in the City of Davis."

    fun isOnCampus(position: LatLng): Boolean = Geo.contains(CampusLocations.campusBoundary, position)

    fun isInServiceArea(position: LatLng): Boolean = Geo.contains(CampusLocations.serviceArea, position)

    fun isValidPassengerCount(count: Int?): Boolean = count != null && count in 1..MAX_PASSENGERS

    /** Null when the trip is allowed, otherwise the message for the "Ride Not Available" screen. */
    fun unavailableReason(pickup: LatLng, dropoff: LatLng): String? = when {
        !isOnCampus(pickup) -> OFF_CAMPUS_PICKUP_MESSAGE
        !isInServiceArea(dropoff) -> OUTSIDE_SERVICE_AREA_MESSAGE
        else -> null
    }

    /**
     * Where the van will meet a rider standing at [position]: the closest campus stop.
     * Off campus there is no stop, so the pin itself is returned and the request will be declined.
     */
    fun pickupSpotFor(position: LatLng): CampusLocation {
        if (!isOnCampus(position)) {
            return CampusLocation("pin", "Dropped pin", "your dropped pin", position, PlaceCategory.HOUSING)
        }
        return (CampusLocations.campus + CampusLocations.parkingLots)
            .minBy { Geo.distanceMeters(it.position, position) }
    }

    /** Label for a start point chosen on the map, given the rider's GPS position ([current], null without a fix). */
    fun startLabelFor(position: LatLng, current: LatLng?): String {
        if (current != null && Geo.distanceMeters(position, current) <= CURRENT_LOCATION_TOLERANCE_METERS) {
            return "Current Location"
        }
        val nearest = CampusLocations.all.minBy { Geo.distanceMeters(it.position, position) }
        return "Near ${nearest.name}"
    }
}
