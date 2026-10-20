package edu.ucdavis.saferide.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Where the rider is right now. Returns null when the position isn't available (no permission, GPS off, no fix). */
interface LocationProvider {
    suspend fun currentLocation(): LatLng?
}

/** Always the fixed spot on the Quad. For unit tests; the app itself uses [DeviceLocationProvider]. */
object SimulatedLocationProvider : LocationProvider {
    override suspend fun currentLocation(): LatLng = CampusLocations.simulatedCurrentLocation
}

/** The phone's GPS (or network location), read with the platform [LocationManager]. */
class DeviceLocationProvider(private val context: Context) : LocationProvider {

    @SuppressLint("MissingPermission") // Checked by hasPermission().
    override suspend fun currentLocation(): LatLng? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .firstOrNull { manager.isProviderEnabled(it) } ?: return null

        val fresh = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            withTimeoutOrNull(FIX_TIMEOUT_MILLIS) { requestFix(manager, provider) }
        } else {
            null
        }
        val location = fresh ?: manager.getLastKnownLocation(provider)
        return location?.let { LatLng(it.latitude, it.longitude) }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun requestFix(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            manager.getCurrentLocation(provider, signal, context.mainExecutor) { cont.resume(it) }
        }

    private fun hasPermission(): Boolean = PERMISSIONS.any {
        context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        private const val FIX_TIMEOUT_MILLIS = 5_000L
    }
}
