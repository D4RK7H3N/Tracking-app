package com.sagay.touristnav.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class LocationFix(
    val lat: Double,
    val lng: Double,
)

@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * One high-accuracy fix, or null on any failure (permission, GPS off,
     * timeout). Callers show a retry prompt on null.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationFix? =
        suspendCancellableCoroutine { continuation ->
            try {
                val cancellationTokenSource =
                    com.google.android.gms.tasks.CancellationTokenSource()
                continuation.invokeOnCancellation { cancellationTokenSource.cancel() }
                fusedLocationClient
                    .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) {
                            continuation.resume(
                                location?.let { LocationFix(it.latitude, it.longitude) },
                            )
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            } catch (@Suppress("TooGenericExceptionCaught") e: SecurityException) {
                if (continuation.isActive) continuation.resume(null)
            }
        }

    /**
     * A stream of high-accuracy fixes for live journey guidance. Permission is
     * checked before subscribing; the callback and its executor are removed
     * when the collector cancels.
     */
    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<LocationFix> = callbackFlow {
        val granted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        if (!granted) {
            close()
            return@callbackFlow
        }
        val executor = Executors.newSingleThreadExecutor()
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(LocationFix(location.latitude, location.longitude))
                }
            }
        }
        try {
            fusedLocationClient.requestLocationUpdates(request, executor, callback)
        } catch (e: SecurityException) {
            close(e)
        }
        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
            executor.shutdown()
        }
    }
}
