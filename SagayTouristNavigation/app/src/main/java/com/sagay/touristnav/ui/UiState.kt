package com.sagay.touristnav.ui

import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.Terminal
import com.sagay.touristnav.domain.models.TripEstimate
import com.sagay.touristnav.platform.LocationFix

/**
 * Data shared by every post-permission state: user position, destination
 * list and the terminal lookup (for colors).
 */
data class MapData(
    val position: LocationFix,
    val destinations: List<Destination>,
    val terminals: Map<String, Terminal>,
)

/**
 * Runtime state machine:
 * NeedsPermission -> Locating -> MapReady -> DestinationSelected ->
 * FetchingRoute -> ResultShown.
 *
 * Branches: GpsError (retryable), WaitingForInternet (trip saved as
 * PENDING_ROUTE; auto-resumes when connectivity returns), RouteError
 * (retryable).
 */
sealed interface UiState {
    data object NeedsPermission : UiState
    data object Locating : UiState
    data class GpsError(val message: String) : UiState
    data class MapReady(val data: MapData) : UiState
    data class DestinationSelected(
        val data: MapData,
        val selected: Destination,
        val terminal: Terminal?,
    ) : UiState

    data class FetchingRoute(
        val data: MapData,
        val selected: Destination,
        val terminal: Terminal?,
    ) : UiState

    data class WaitingForInternet(
        val data: MapData,
        val selected: Destination,
        val terminal: Terminal?,
    ) : UiState

    data class RouteError(
        val data: MapData,
        val selected: Destination,
        val terminal: Terminal?,
        val message: String,
    ) : UiState

    data class ResultShown(
        val data: MapData,
        val trip: TripEstimate,
    ) : UiState
}

/** Parses "#RRGGBB" into an ARGB Int; falls back to grey on bad data. */
fun parseHexColor(hex: String?): Int = try {
    (0xFF000000L or (hex?.removePrefix("#")?.toLong(16) ?: 0x9E9E9EL)).toInt()
} catch (_: NumberFormatException) {
    0xFF9E9E9E.toInt()
}
