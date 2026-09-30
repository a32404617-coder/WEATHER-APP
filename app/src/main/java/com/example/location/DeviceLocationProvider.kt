package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.example.model.LatLngPoint
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class DeviceLocationProvider(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): LatLngPoint? {
        val fusedLoc = suspendCancellableCoroutine<Location?> { cont ->
            try {
                fusedClient.lastLocation
                    .addOnSuccessListener { loc ->
                        if (cont.isActive) cont.resume(loc)
                    }
                    .addOnFailureListener {
                        if (cont.isActive) cont.resume(null)
                    }
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }

        if (fusedLoc != null) {
            return LatLngPoint(fusedLoc.latitude, fusedLoc.longitude)
        }

        return fallbackFromLocationManager()
    }

    @SuppressLint("MissingPermission")
    private fun fallbackFromLocationManager(): LatLngPoint? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val providers = locationManager?.getProviders(true) ?: return null
            var bestLocation: Location? = null
            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            }
            if (bestLocation != null) {
                LatLngPoint(bestLocation.latitude, bestLocation.longitude)
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
