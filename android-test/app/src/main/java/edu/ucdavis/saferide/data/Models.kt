package edu.ucdavis.saferide.data

/** A map coordinate. Kept independent of any map SDK so the data layer has no UI dependencies. */
data class LatLng(val latitude: Double, val longitude: Double)

enum class PlaceCategory(val title: String) {
    CAMPUS("Campus Locations"),
    PARKING("Parking Lots"),
    HOUSING("Off Campus Housing"),
}

/**
 * A place a rider can be picked up or dropped off.
 * [stopName] is the exact curbside spot the van pulls up to, shown in the arrival alert.
 */
data class CampusLocation(
    val id: String,
    val name: String,
    val stopName: String,
    val position: LatLng,
    val category: PlaceCategory = PlaceCategory.CAMPUS,
)

/** Where the rider actually is. They may walk from here to the [CampusLocation] pickup spot. */
data class StartLocation(val label: String, val position: LatLng)

data class Vehicle(
    val number: Int,
    val licensePlate: String,
    val position: LatLng,
) {
    val displayName: String get() = "Van #$number"
}

data class CampusStatus(
    val vehicles: List<Vehicle>,
    val estimatedWaitMinutes: Int,
) {
    val activeVanCount: Int get() = vehicles.size
}

enum class RideStatus {
    /** Waiting in line; no van assigned yet. */
    QUEUED,

    /** A van is assigned and driving to the pickup point. */
    ASSIGNED,

    /** The van is inside the strict arrival radius of the pickup point. */
    ARRIVED,

    /** The rider is in the van, heading to the drop-off. */
    ON_BOARD,

    /** The rider ended the trip. */
    COMPLETED,
}

data class RideRequest(
    val id: String,
    val rider: String,
    val pickup: CampusLocation,
    val dropoff: CampusLocation,
    val status: RideStatus,
    val queuePosition: Int,
    val passengers: Int = 1,
    val wheelchair: Boolean = false,
    val note: String = "",
    val assignedVehicle: Vehicle? = null,
    /** Remaining driving distance to the pickup point, once a van is assigned. */
    val metersToPickup: Double? = null,
    /** Driver ETA in whole minutes: to the pickup while assigned, to the drop-off while on board. */
    val etaMinutes: Int? = null,
    /** The path the van is currently driving, for drawing on the map. */
    val route: List<LatLng> = emptyList(),
)

/** The dispatcher turned the request down, e.g. because the pickup is off campus. */
class RideUnavailableException(message: String) : Exception(message)
