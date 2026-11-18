package com.sagay.touristnav.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagay.touristnav.data.local.SeedLoader
import com.sagay.touristnav.data.local.entities.TripRecordEntity
import com.sagay.touristnav.data.repo.TripRepository
import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.RouteSummary
import com.sagay.touristnav.domain.models.Terminal
import com.sagay.touristnav.platform.LocationFix
import com.sagay.touristnav.platform.LocationProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    private val seedLoader: SeedLoader,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.NeedsPermission)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()

    private val _history = MutableStateFlow<List<TripRecordEntity>>(emptyList())
    val history: StateFlow<List<TripRecordEntity>> = _history.asStateFlow()

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

    /** Student / senior citizen / PWD discount (20% in the city matrix). */
    private var discounted: Boolean = false

    /** Cached route of the current trip, so the discount toggle needs no network. */
    private var cachedRoute: RouteSummary? = null
    private var cachedOrigin: LocationFix? = null
    private var cachedDestination: Destination? = null
    private var cachedTerminal: Terminal? = null
    private var isOnline = false

    init {
        viewModelScope.launch {
            repository.favoriteIds.collect { ids -> _favoriteIds.value = ids.toSet() }
        }
        viewModelScope.launch {
            repository.tripHistory.collect { records -> _history.value = records }
        }
        viewModelScope.launch {
            repository.pendingTrip()?.let { trip ->
                pendingTripId = trip.id
                pendingOrigin = LocationFix(trip.originLat, trip.originLng)
                pendingDestinationId = trip.destinationId
                maybeResumePendingTrip()
            }
        }
        viewModelScope.launch {
            repository.isOnline.collect { online ->
                isOnline = online
                if (!online) return@collect
                val waiting = _state.value as? UiState.WaitingForInternet
                if (waiting != null) {
                    startFetch(mapData.position, waiting.selected, waiting.terminal)
                } else {
                    maybeResumePendingTrip()
                }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            if (_state.value is UiState.NeedsPermission || _state.value is UiState.GpsError) {
                locate()
            }
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
                seedLoader.seed()
                val destinations = repository.destinations()
                val terminals = repository.terminals().associateBy { it.id }
                val routes = repository.routes().associateBy { it.id }
                mapData = MapData(fix, destinations, terminals, routes)
                dataLoaded = true
            } else {
                mapData = mapData.copy(position = fix)
            }
            _state.value = UiState.MapReady(mapData)
            maybeResumePendingTrip()
        }
    }

    fun onDestinationSelected(destination: Destination) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
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
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            fetchRoute(mapData.position, current.selected, current.terminal)
        }
    }

    /**
     * Toggles the 20% student / senior citizen / PWD fare. The route is
     * reused, so this never hits the network.
     */
    fun toggleDiscount() {
        val current = _state.value as? UiState.ResultShown ?: return
        val route = cachedRoute ?: return
        val origin = cachedOrigin ?: return
        val destination = cachedDestination ?: return
        val terminal = cachedTerminal ?: return
        discounted = !discounted
        viewModelScope.launch {
            val trip = repository.estimateTrip(
                origin = origin,
                destination = destination,
                terminal = terminal,
                route = route,
                discounted = discounted,
            )
            _state.value = UiState.ResultShown(mapData, trip)
        }
    }

    fun selectNewDestination() {
        fetchJob?.cancel()
        fetchJob = null
        _state.value = UiState.MapReady(mapData)
    }

    fun saveCurrentTrip(passengers: Int = 1) {
        val trip = (_state.value as? UiState.ResultShown)?.trip ?: return
        viewModelScope.launch { repository.saveTrip(trip, passengers) }
    }

    fun toggleFavorite(destinationId: String) {
        viewModelScope.launch {
            val favorite = destinationId !in _favoriteIds.value
            repository.toggleFavorite(destinationId, favorite)
        }
    }

    fun deleteTrip(id: Long) {
        viewModelScope.launch { repository.deleteTrip(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    private fun maybeResumePendingTrip() {
        if (!isOnline || !dataLoaded || _state.value !is UiState.MapReady) return
        resumePendingTrip()
    }

    private fun resumePendingTrip() {
        val origin = pendingOrigin ?: return
        val destinationId = pendingDestinationId ?: return
        if (fetchJob?.isActive == true) return
        fetchJob = viewModelScope.launch {
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

    private fun startFetch(
        origin: LocationFix,
        destination: Destination,
        terminal: Terminal?,
    ) {
        if (fetchJob?.isActive == true) return
        fetchJob = viewModelScope.launch {
            fetchRoute(origin, destination, terminal)
        }
    }

    private suspend fun fetchRoute(
        origin: LocationFix,
        destination: Destination,
        terminal: Terminal?,
    ) {
        try {
            val resolvedTerminal = terminal ?: fallbackTerminal(destination)
            val route = repository.fetchRoute(
                origin = origin,
                targetLat = if (destination.requiresBoat) resolvedTerminal.lat else destination.lat,
                targetLng = if (destination.requiresBoat) resolvedTerminal.lng else destination.lng,
            )
            // Cache so the discount toggle can re-price without another request.
            cachedRoute = route
            cachedOrigin = origin
            cachedDestination = destination
            cachedTerminal = resolvedTerminal
            val trip = repository.estimateTrip(
                origin = origin,
                destination = destination,
                terminal = resolvedTerminal,
                route = route,
                discounted = discounted,
            )
            clearPendingTrip()
            _state.value = UiState.ResultShown(mapData, trip)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            if (isOnline) {
                _state.value = UiState.RouteError(
                    data = mapData,
                    selected = destination,
                    terminal = terminal,
                    message = e.message ?: "Could not fetch the route.",
                )
            } else {
                val savedId = runCatching {
                    repository.savePendingTrip(origin, destination.id, pendingTripId)
                }.getOrNull()
                if (savedId != null) {
                    pendingTripId = savedId
                    pendingOrigin = origin
                    pendingDestinationId = destination.id
                }
                _state.value = UiState.WaitingForInternet(mapData, destination, terminal)
            }
        } catch (e: Exception) {
            _state.value = UiState.RouteError(
                data = mapData,
                selected = destination,
                terminal = terminal,
                message = e.message ?: "Could not fetch the route.",
            )
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
