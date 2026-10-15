package edu.ucdavis.saferide.domain

import kotlin.math.ceil
import kotlin.math.max

/**
 * Pickup estimate while the rider waits in line. It scales the campus wait by queue position, so it
 * only drops as the rider moves up, instead of jumping around like the current app's countdown.
 */
object QueueEstimate {
    /** Riders join the line at this position. */
    const val STARTING_POSITION = 3

    fun pickupMinutes(queuePosition: Int, campusWaitMinutes: Int): Int =
        max(1, ceil(campusWaitMinutes * queuePosition.toDouble() / STARTING_POSITION).toInt())
}
