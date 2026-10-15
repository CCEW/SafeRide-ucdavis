package edu.ucdavis.saferide.data

import kotlinx.coroutines.flow.Flow

/**
 * Everything the app needs from a dispatch backend. There is no public UC Davis SafeRide API,
 * so the app uses [MockSafeRideService]; a real backend would implement this interface.
 */
interface SafeRideService {

    /** Active vans and the current estimated campus wait. */
    suspend fun fetchCampusStatus(): CampusStatus

    /**
     * Puts the rider in line. The returned request is [RideStatus.QUEUED].
     * Throws [RideUnavailableException] when SafeRide doesn't serve the trip (e.g. an off-campus pickup).
     */
    suspend fun requestRide(
        pickup: CampusLocation,
        dropoff: CampusLocation,
        passengers: Int = 1,
        wheelchair: Boolean = false,
        note: String = "",
    ): RideRequest

    /**
     * Live updates for [request] until the van arrives: queue position drops, a van is assigned
     * and drives to the pickup point, and the last emission is [RideStatus.ARRIVED].
     * Cancelling collection cancels the ride simulation.
     */
    fun startSimulatedQueueUpdates(request: RideRequest): Flow<RideRequest>

    /**
     * The ride to the drop-off once the rider is on board: the van drives from the pickup to the
     * drop-off, and the last emission is [RideStatus.COMPLETED].
     */
    fun startSimulatedTrip(request: RideRequest): Flow<RideRequest>

    /** Ends the trip. */
    suspend fun completeRide(request: RideRequest): RideRequest
}
