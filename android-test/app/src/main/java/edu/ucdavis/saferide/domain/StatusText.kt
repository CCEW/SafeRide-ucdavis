package edu.ucdavis.saferide.domain

import edu.ucdavis.saferide.data.CampusStatus
import edu.ucdavis.saferide.data.RideRequest
import edu.ucdavis.saferide.data.RideStatus

/** The exact strings shown on the status banner and the live queue card. */
object StatusText {

    fun campusBanner(status: CampusStatus): String {
        val vans = if (status.activeVanCount == 1) "1 Van Active" else "${status.activeVanCount} Vans Active"
        return "$vans • Estimated Campus Wait: ~${status.estimatedWaitMinutes} mins"
    }

    fun nearestVan(estimate: VanEstimate, pickupName: String): String =
        "Nearest van: ${estimate.vehicle.displayName} • ~${minutes(estimate.minutes)} from $pickupName"

    /** Before a van is assigned: an estimate that only ever goes down as the rider moves up in line. */
    fun queueEstimate(queuePosition: Int, campusWaitMinutes: Int): String =
        "Estimated pickup ~${minutes(QueueEstimate.pickupMinutes(queuePosition, campusWaitMinutes))}"

    fun queueCard(ride: RideRequest): String = when (ride.status) {
        RideStatus.QUEUED ->
            "Request Received • Position in Line: #${ride.queuePosition}"

        RideStatus.ASSIGNED -> {
            val van = ride.assignedVehicle?.displayName ?: "Van"
            "$van Assigned • Position in Line: #${ride.queuePosition} • Driver ~${minutes(ride.etaMinutes ?: 1)} away"
        }

        RideStatus.ARRIVED ->
            "${ride.assignedVehicle?.displayName ?: "Your van"} is at the ${ride.pickup.stopName}"

        RideStatus.ON_BOARD ->
            "On your way to ${ride.dropoff.name} • ~${minutes(ride.etaMinutes ?: 1)} left"

        RideStatus.COMPLETED -> "Trip complete"
    }

    private fun minutes(n: Int) = if (n == 1) "1 min" else "$n mins"
}
