package edu.ucdavis.saferide.data

import edu.ucdavis.saferide.domain.Geo
import edu.ucdavis.saferide.domain.Proximity
import edu.ucdavis.saferide.domain.Route
import edu.ucdavis.saferide.domain.TripRules
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import kotlin.math.min

/** Timing for the simulation. Tests run it on virtual time, so the defaults are tuned for demos. */
data class SimulationConfig(
    /** Fake network round trip for [MockSafeRideService.fetchCampusStatus] and requests. */
    val networkLatencyMillis: Long = 600,
    /** Time between queue position changes (the spec asks for 4–6 seconds). */
    val queueStepMillis: Long = 5_000,
    /** How often the van's position updates while driving. */
    val driveTickMillis: Long = 500,
    /** Meters per tick while far from the pickup point. */
    val driveStepMeters: Double = 40.0,
    /** Meters per tick inside [approachZoneMeters], so the van slows down as it pulls in. */
    val approachStepMeters: Double = 10.0,
    val approachZoneMeters: Double = 100.0,
    /** Roughly how many ticks the ride to the drop-off takes, whatever its length. */
    val tripTicks: Int = 24,
)

class MockSafeRideService(
    private val config: SimulationConfig = SimulationConfig(),
) : SafeRideService {

    private val vans = listOf(
        Vehicle(number = 1, licensePlate = "8UCD101", position = LatLng(38.54310, -121.74800)),
        Vehicle(number = 2, licensePlate = "8UCD202", position = LatLng(38.54200, -121.76050)),
        Vehicle(number = 4, licensePlate = "8UCD404", position = LatLng(38.53520, -121.74880)),
    )

    /** The van the dispatcher always assigns in this prototype. */
    private val assignedVan = vans.first { it.number == 4 }

    override suspend fun fetchCampusStatus(): CampusStatus {
        delay(config.networkLatencyMillis)
        return CampusStatus(vehicles = vans, estimatedWaitMinutes = 12)
    }

    override suspend fun requestRide(
        pickup: CampusLocation,
        dropoff: CampusLocation,
        passengers: Int,
        wheelchair: Boolean,
        note: String,
    ): RideRequest {
        delay(config.networkLatencyMillis)
        require(TripRules.isValidPassengerCount(passengers)) { "Invalid passenger count: $passengers" }
        TripRules.unavailableReason(pickup.position, dropoff.position)?.let { throw RideUnavailableException(it) }
        return RideRequest(
            id = UUID.randomUUID().toString(),
            rider = CampusLocations.RIDER_NAME,
            pickup = pickup,
            dropoff = dropoff,
            status = RideStatus.QUEUED,
            queuePosition = 3,
            passengers = passengers,
            wheelchair = wheelchair,
            note = note,
        )
    }

    override fun startSimulatedQueueUpdates(request: RideRequest): Flow<RideRequest> = flow {
        val pickup = request.pickup.position

        // Step A: move up in line.
        delay(config.queueStepMillis)
        var ride = request.copy(queuePosition = 2)
        emit(ride)

        // Step B: first in line, Van #4 assigned and starts driving.
        delay(config.queueStepMillis)
        val route = Route.toPickup(assignedVan.position, pickup)
        ride = ride.copy(
            status = RideStatus.ASSIGNED,
            queuePosition = 1,
            assignedVehicle = assignedVan.copy(position = route.positionAt(0.0)),
            metersToPickup = route.lengthMeters,
            etaMinutes = Route.etaMinutes(route.lengthMeters),
            route = route.points,
        )
        emit(ride)

        // Step C: drive along the route; arrive only inside the strict radius.
        var traveled = 0.0
        while (true) {
            delay(config.driveTickMillis)
            val remainingBefore = route.lengthMeters - traveled
            val step = if (remainingBefore > config.approachZoneMeters) {
                config.driveStepMeters
            } else {
                config.approachStepMeters
            }
            traveled = min(traveled + step, route.lengthMeters)
            val position = route.positionAt(traveled)
            val remaining = route.lengthMeters - traveled
            val van = ride.assignedVehicle!!.copy(position = position)

            if (Proximity.hasArrived(position, pickup)) {
                emit(
                    ride.copy(
                        status = RideStatus.ARRIVED,
                        assignedVehicle = van,
                        metersToPickup = Geo.distanceMeters(position, pickup),
                        etaMinutes = 0,
                        route = route.remainingFrom(traveled),
                    )
                )
                return@flow
            }

            ride = ride.copy(
                assignedVehicle = van,
                metersToPickup = remaining,
                etaMinutes = Route.etaMinutes(remaining),
                route = route.remainingFrom(traveled),
            )
            emit(ride)
        }
    }

    override fun startSimulatedTrip(request: RideRequest): Flow<RideRequest> = flow {
        val route = Route.drive(request.pickup.position, request.dropoff.position)
        val van = request.assignedVehicle ?: assignedVan
        val step = maxOf(config.driveStepMeters, route.lengthMeters / config.tripTicks)
        var ride = request.copy(
            status = RideStatus.ON_BOARD,
            assignedVehicle = van.copy(position = route.positionAt(0.0)),
            metersToPickup = null,
            etaMinutes = Route.etaMinutes(route.lengthMeters),
            route = route.points,
        )
        emit(ride)

        var traveled = 0.0
        while (traveled < route.lengthMeters) {
            delay(config.driveTickMillis)
            traveled = min(traveled + step, route.lengthMeters)
            ride = ride.copy(
                assignedVehicle = ride.assignedVehicle!!.copy(position = route.positionAt(traveled)),
                etaMinutes = Route.etaMinutes(route.lengthMeters - traveled),
                route = route.remainingFrom(traveled),
            )
            emit(ride)
        }
        emit(ride.copy(status = RideStatus.COMPLETED, etaMinutes = null))
    }

    override suspend fun completeRide(request: RideRequest): RideRequest {
        delay(config.networkLatencyMillis)
        return request.copy(status = RideStatus.COMPLETED, etaMinutes = null, metersToPickup = null)
    }

    companion object {
        /** App-wide instance. */
        val shared: MockSafeRideService by lazy { MockSafeRideService() }
    }
}
