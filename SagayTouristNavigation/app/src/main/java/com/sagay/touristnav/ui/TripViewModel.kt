package com.sagay.touristnav.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagay.touristnav.data.repo.TripRepository
import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.Terminal
import com.sagay.touristnav.platform.LocationFix
import com.sagay.touristnav.platform.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class TripViewModel @Inject constructor(
    private val repository: TripRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.NeedsPermission)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var mapData = MapData(
        position = LocationFix(SAGAY_CENTER_LAT, SAGAY_CENTER_LNG),
        destinations = emptyList(),
        terminals = emptyMap(),
    )
    private var dataLoaded = false

    // Restored PENDING_ROUTE trip (from a previous offline session).
    private var pendingTripId: Long? = null
    private var pendingOrigin: LocationFix? = null
    private var pendingDestinationId: String? = null

    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            repository.pendingTrip()?.let { trip ->
                pendingTripId = trip.id
                pendingOrigin = LocationFix(trip.originLat, trip.originLng)
                pendingDestinationId = trip.destinationId
            }
        }
        viewModelScope.launch {
            repository.isOnline.collect { online ->
                if (!online) return@collect
                val waiting = _state.value as? UiState.WaitingForInternet
                when {
                    waiting != null ->
                        fetchRoute(mapData.position, waiting.selected, waiting.terminal)

                    pendingOrigin != null && pendingDestinationId != null &&
                        _state.value is UiState.MapReady ->
                        resumePendingTrip()
                }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            locate()
        } else {
            _state.value = UiState.NeedsPermission
        }
    }

    fun retryGps() = locate()

    /**
     * "Locate me" button: takes a fresh GPS fix and recenters, keeping
     * whatever trip state the user is currently in.
     */
    fun locateMe() {
        viewModelScope.launch {
            val fix = withTimeoutOrNull(10_000) { locationProvider.getCurrentLocation() }
                ?: return@launch
            mapData = mapData.copy(position = fix)
            reissueCurrentState()
        }
    }

    /** Re-emits the current state so the map picks up the new user position. */
    private fun reissueCurrentState() {
        _state.value = when (val s = _state.value) {
            is UiState.MapReady -> s.copy(data = mapData)
            is UiState.DestinationSelected -> s.copy(data = mapData)
            is UiState.FetchingRoute -> s.copy(data = mapData)
            is UiState.WaitingForInternet -> s.copy(data = mapData)
            is UiState.RouteError -> s.copy(data = mapData)
            is UiState.ResultShown -> s.copy(data = mapData)
            else -> s
        }
    }

    private fun locate() {
        _state.value = UiState.Locating
        viewModelScope.launch {
            val fix = locationProvider.getCurrentLocation()
            if (fix == null) {
                _state.value = UiState.GpsError(
                    "Could not get your location. Make sure GPS/location " +
                        "services are turned on, then retry.",
                )
                return@launch
            }
            if (!dataLoaded) {
                val destinations = repository.destinations()
                val terminals = repository.terminals().associateBy { it.id }
                mapData = MapData(fix, destinations, terminals)
                dataLoaded = true
            } else {
                mapData = mapData.copy(position = fix)
            }
            if (pendingOrigin != null && pendingDestinationId != null) {
                resumePendingTrip()
            } else {
                _state.value = UiState.MapReady(mapData)
            }
        }
    }

    fun onDestinationSelected(destination: Destination) {
        viewModelScope.launch {
            val terminal = mapData.terminals[destination.terminalId]
                ?: repository.terminalById(destination.terminalId)
            _state.value = UiState.DestinationSelected(mapData, destination, terminal)
            // Refresh GPS so the route starts from where the user actually
            // is, not from the stale fix taken at app start.
            val fresh = withTimeoutOrNull(5_000) {
                locationProvider.getCurrentLocation()
            }
            if (fresh != null) {
                mapData = mapData.copy(position = fresh)
                _state.value = UiState.DestinationSelected(mapData, destination, terminal)
            }
            fetchRoute(fresh ?: mapData.position, destination, terminal)
        }
    }

    fun retryRoute() {
        val current = _state.value as? UiState.RouteError ?: return
        fetchRoute(mapData.position, current.selected, current.terminal)
    }

    fun selectNewDestination() {
        fetchJob?.cancel()
        fetchJob = null
        _state.value = UiState.MapReady(mapData)
    }

    private fun resumePendingTrip() {
        val origin = pendingOrigin ?: return
        val destinationId = pendingDestinationId ?: return
        if (fetchJob?.isActive == true) return
        viewModelScope.launch {
            val destination = repository.destinationById(destinationId)
            if (destination == null) {
                clearPendingTrip()
                _state.value = UiState.MapReady(mapData)
                return@launch
            }
            val terminal = mapData.terminals[destination.terminalId]
                ?: repository.terminalById(destination.terminalId)
            _state.value = UiState.FetchingRoute(mapData, destination, terminal)
            fetchRoute(origin, destination, terminal)
        }
    }

    private fun fetchRoute(
        origin: LocationFix,
        destination: Destination,
        terminal: Terminal?,
    ) {
        if (fetchJob?.isActive == true) return
        fetchJob = viewModelScope.launch {
            try {
                val trip = repository.estimateTrip(
                    origin = origin,
                    destination = destination,
                    terminal = terminal ?: fallbackTerminal(destination),
                )
                clearPendingTrip()
                _state.value = UiState.ResultShown(mapData, trip)
            } catch (e: IOException) {
                if (pendingTripId == null) {
                    pendingTripId = runCatching {
                        repository.savePendingTrip(origin, destination.id)
                    }.getOrNull()
                }
                _state.value = UiState.WaitingForInternet(mapData, destination, terminal)
            } catch (e: Exception) {
                _state.value = UiState.RouteError(
                    data = mapData,
                    selected = destination,
                    terminal = terminal,
                    message = e.message ?: "Could not fetch the route.",
                )
            }
        }
    }

    private suspend fun clearPendingTrip() {
        pendingTripId?.let { id ->
            runCatching { repository.markTripDone(id) }
        }
        pendingTripId = null
        pendingOrigin = null
        pendingDestinationId = null
    }

    private fun fallbackTerminal(destination: Destination) = Terminal(
        id = destination.terminalId,
        name = "Terminal info unavailable",
        lat = destination.lat,
        lng = destination.lng,
        colorHex = "#9E9E9E",
        colorName = "Unknown",
        serviceArea = "",
    )

    private companion object {
        const val SAGAY_CENTER_LAT = 10.896
        const val SAGAY_CENTER_LNG = 123.416
    }
}
