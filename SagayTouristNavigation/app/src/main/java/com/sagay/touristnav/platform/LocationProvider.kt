package com.sagay.touristnav.platform

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
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
}
