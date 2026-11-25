package com.sagay.touristnav.ui

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.animation.DecelerateInterpolator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagay.touristnav.R
import com.sagay.touristnav.domain.Haversine
import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.Route
import com.sagay.touristnav.domain.models.RoutePoint
import com.sagay.touristnav.domain.models.RouteStop
import com.sagay.touristnav.domain.models.Terminal
import com.sagay.touristnav.platform.OfflineMapPack
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private const val TAP_THRESHOLD_KM = 0.3
private val USER_RADIUS_DP = 20f
private val UNSELECTED_RADIUS_DP = 18f
private val SELECTED_RADIUS_DP = 26f

@Composable
fun MapScreen(viewModel: TripViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showTerminals by rememberSaveable { mutableStateOf(false) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showOfflinePack by rememberSaveable { mutableStateOf(false) }
    var showFareCalculator by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    // Bumped when an offline pack is downloaded/deleted so the live map
    // switches tile providers immediately, in any UI state.
    var packVersion by remember { mutableIntStateOf(0) }
    val openHistory = { showHistory = true }
    val openOfflinePack = { showOfflinePack = true }
    val openFareCalculator = { showFareCalculator = true }
    val openSettings = { showSettings = true }

    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        viewModel.onPermissionResult(
            result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true,
        )
    }

    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            viewModel.onPermissionResult(true)
        } else {
            launcher.launch(permissions)
        }
    }

    // The map is the base layer; the fare tool is a full-screen overlay so it
    // works identically in every UiState (including pre-permission states).
    Box(modifier = Modifier.fillMaxSize()) {
        when (val s = state) {
        is UiState.NeedsPermission -> PermissionPrompt(
            title = "Location needed",
            message = "Sagay Tourist Navigation needs your location to measure " +
                "road distance and estimate tricycle fares.",
            buttonLabel = "Allow location",
            onAction = { launcher.launch(permissions) },
        )

        is UiState.Locating -> CenterMessage("Finding your location\u2026")

        is UiState.GpsError -> PermissionPrompt(
            title = "Location unavailable",
            message = s.message,
            buttonLabel = "Retry",
            onAction = viewModel::retryGps,
        )

        is UiState.MapReady -> TripMap(
            data = s.data,
            selected = null,
            routePoints = emptyList(),
            onDestinationClick = viewModel::onDestinationSelected,
            onLocateMe = viewModel::locateMe,
            onShowHistory = openHistory,
            onShowOfflinePack = openOfflinePack,
            onShowFareCalculator = openFareCalculator,
            onShowSettings = openSettings,
            packVersion = packVersion,
            bottomContent = {
                DestinationSheet(
                    destinations = s.data.destinations,
                    terminals = s.data.terminals,
                    routes = s.data.routes,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onSelect = viewModel::onDestinationSelected,
                    onShowTerminals = { showTerminals = true },
                )
            },
        )

        is UiState.DestinationSelected -> TripMap(
            data = s.data,
            selected = s.selected,
            routePoints = emptyList(),
            onDestinationClick = viewModel::onDestinationSelected,
            onLocateMe = viewModel::locateMe,
            onShowHistory = openHistory,
            onShowOfflinePack = openOfflinePack,
            onShowFareCalculator = openFareCalculator,
            onShowSettings = openSettings,
            packVersion = packVersion,
            bottomContent = { RouteProgressCard("Looking up the road route\u2026") },
        )

        is UiState.FetchingRoute -> TripMap(
            data = s.data,
            selected = s.selected,
            routePoints = emptyList(),
            onDestinationClick = viewModel::onDestinationSelected,
            onLocateMe = viewModel::locateMe,
            onShowHistory = openHistory,
            onShowOfflinePack = openOfflinePack,
            onShowFareCalculator = openFareCalculator,
            onShowSettings = openSettings,
            packVersion = packVersion,
            bottomContent = { RouteProgressCard("Fetching road distance\u2026") },
        )

        is UiState.WaitingForInternet -> TripMap(
            data = s.data,
            selected = s.selected,
            routePoints = emptyList(),
            onDestinationClick = viewModel::onDestinationSelected,
            onLocateMe = viewModel::locateMe,
            onShowHistory = openHistory,
            onShowOfflinePack = openOfflinePack,
            onShowFareCalculator = openFareCalculator,
            onShowSettings = openSettings,
            packVersion = packVersion,
            bottomContent = {
                RouteProgressCard(
                    "No internet. This trip is saved and the route will load " +
                        "automatically once you are back online.",
                )
            },
        )

        is UiState.RouteError -> TripMap(
            data = s.data,
            selected = s.selected,
            routePoints = emptyList(),
            onDestinationClick = viewModel::onDestinationSelected,
            onLocateMe = viewModel::locateMe,
            onShowHistory = openHistory,
            onShowOfflinePack = openOfflinePack,
            onShowFareCalculator = openFareCalculator,
            onShowSettings = openSettings,
            packVersion = packVersion,
            bottomContent = {
                RouteErrorCard(message = s.message, onRetry = viewModel::retryRoute)
            },
        )

        is UiState.ResultShown -> TripMap(
            data = s.data,
            selected = s.trip.destination,
            routePoints = s.trip.routePoints,
            onDestinationClick = viewModel::onDestinationSelected,
            onLocateMe = viewModel::locateMe,
            onShowHistory = openHistory,
            onShowOfflinePack = openOfflinePack,
            onShowFareCalculator = openFareCalculator,
            onShowSettings = openSettings,
            packVersion = packVersion,
            routeStops = s.trip.stopService?.stops.orEmpty(),
            routeColorHex = s.trip.stopService?.colorHex ?: "#FFFFFF",
            bottomContent = {
                TripDetailsCard(
                    trip = s.trip,
                    terminals = s.data.terminals,
                    onSelectNew = viewModel::selectNewDestination,
                    onToggleDiscount = viewModel::toggleDiscount,
                    onSaveTrip = { viewModel.saveCurrentTrip() },
                    onStartGuidance = viewModel::startGuidance,
                    passengers = viewModel.passengerCount(),
                    onChangePassengers = viewModel::setPassengers,
                    onShare = { shareText(context, viewModel.currentShareText()) },
                )
            },
        )

        is UiState.Guidance -> TripMap(
            data = s.data,
            selected = s.trip.destination,
            routePoints = s.trip.routePoints,
            onDestinationClick = viewModel::onDestinationSelected,
            onLocateMe = viewModel::locateMe,
            onShowHistory = openHistory,
            onShowOfflinePack = openOfflinePack,
            onShowFareCalculator = openFareCalculator,
            onShowSettings = openSettings,
            packVersion = packVersion,
            routeStops = s.trip.stopService?.stops.orEmpty(),
            routeColorHex = s.trip.stopService?.colorHex ?: "#FFFFFF",
            bottomContent = {
                GuidanceCard(
                    snapshot = s.snapshot,
                    destinationName = s.trip.destination.name,
                    onEnd = viewModel::endGuidance,
                )
            },
        )
        }

        // Non-map states still get access to the two new tools.
        if (state is UiState.NeedsPermission ||
            state is UiState.Locating ||
            state is UiState.GpsError
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SmallFloatingActionButton(
                    onClick = openFareCalculator,
                    containerColor = androidx.compose.ui.graphics.Color.White,
                    contentColor = SagayColors.DeepOcean,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_payments),
                        contentDescription = "Fare calculator",
                    )
                }
                SmallFloatingActionButton(
                    onClick = openSettings,
                    containerColor = androidx.compose.ui.graphics.Color.White,
                    contentColor = SagayColors.DeepOcean,
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }

        val overlayDuration = Motion.duration(Motion.PAGE_MS)
        AnimatedVisibility(
            visible = showFareCalculator,
            enter = fadeIn(tween<Float>(overlayDuration)) +
                slideInVertically(tween<IntOffset>(overlayDuration)) { it / 8 },
            exit = fadeOut(tween<Float>(overlayDuration)) +
                slideOutVertically(tween<IntOffset>(overlayDuration)) { it / 8 },
            label = "fare-calculator-overlay",
        ) {
            FareCalculatorScreen(onDismiss = { showFareCalculator = false })
        }
    }

    if (showOfflinePack) {
        OfflinePackSheet(
            onDismiss = { showOfflinePack = false },
            onPackChanged = { packVersion += 1 },
        )
    }

    if (showHistory) {
        HistorySheet(
            viewModel = viewModel,
            onDismiss = { showHistory = false },
        )
    }

    if (showTerminals) {
        TerminalLegendSheet(
            routes = routesInOrder(state),
            terminals = terminalsInOrder(state).associateBy { it.id },
            onDismiss = { showTerminals = false },
        )
    }

    if (showSettings) {
        SettingsSheet(
            viewModel = viewModel,
            onDismiss = { showSettings = false },
        )
    }
}

/**
 * Terminals of the current state in T1, T2, T3 order, which is the order a
 * tourist learns them in.
 */
private fun terminalsInOrder(state: UiState): List<Terminal> {
    val terminals = when (state) {
        is UiState.MapReady -> state.data.terminals
        is UiState.DestinationSelected -> state.data.terminals
        is UiState.FetchingRoute -> state.data.terminals
        is UiState.WaitingForInternet -> state.data.terminals
        is UiState.RouteError -> state.data.terminals
        is UiState.ResultShown -> state.data.terminals
        is UiState.Guidance -> state.data.terminals
        else -> emptyMap()
    }
    return terminals.values.sortedBy { it.id }
}

/** The routes of the current state, in the order a tourist meets them. */
private fun routesInOrder(state: UiState): List<Route> {
    val routes = when (state) {
        is UiState.MapReady -> state.data.routes
        is UiState.DestinationSelected -> state.data.routes
        is UiState.FetchingRoute -> state.data.routes
        is UiState.WaitingForInternet -> state.data.routes
        is UiState.RouteError -> state.data.routes
        is UiState.ResultShown -> state.data.routes
        is UiState.Guidance -> state.data.routes
        else -> emptyMap()
    }
    return routes.values.sortedBy { it.id }
}

@Composable
private fun TripMap(
    data: MapData,
    selected: Destination?,
    routePoints: List<RoutePoint>,
    onDestinationClick: (Destination) -> Unit,
    onLocateMe: () -> Unit,
    routeStops: List<RouteStop> = emptyList(),
    routeColorHex: String = "#FFFFFF",
    onShowHistory: () -> Unit = {},
    onShowOfflinePack: () -> Unit = {},
    onShowFareCalculator: () -> Unit = {},
    onShowSettings: () -> Unit = {},
    packVersion: Int = 0,
    bottomContent: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val position = data.position
    val mapView = remember { MapView(context) }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    var lastCameraKey by remember { mutableStateOf("") }
    val cameraKey = "${selected?.id ?: "-"}|${routePoints.size}|${position.lat},${position.lng}"
    var locateRequest by remember { mutableStateOf(0) }
    val reduceMotion = Motion.reduced()
    val zoomDuration = Motion.duration(Motion.LAYOUT_MS)
    var zoomAnimator by remember { mutableStateOf<ValueAnimator?>(null) }
    val smoothZoom: (Float) -> Unit = { delta ->
        zoomAnimator?.cancel()
        val from = mapView.zoomLevelDouble
        val to = (from + delta.toDouble()).coerceIn(
            mapView.minZoomLevel.toDouble(),
            mapView.maxZoomLevel.toDouble(),
        )
        if (reduceMotion) {
            mapView.controller.setZoom(to)
        } else {
            ValueAnimator.ofFloat(from.toFloat(), to.toFloat()).apply {
                duration = zoomDuration.toLong()
                interpolator = DecelerateInterpolator()
                addUpdateListener { animation ->
                    mapView.controller.setZoom((animation.animatedValue as Float).toDouble())
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (zoomAnimator == animation) zoomAnimator = null
                    }
                })
                zoomAnimator = this
                start()
            }
        }
    }

    LaunchedEffect(locateRequest, position.lat, position.lng) {
        if (locateRequest > 0) {
            mapView.controller.setZoom(14.5)
            mapView.controller.setCenter(GeoPoint(position.lat, position.lng))
        }
    }

    // A pack downloaded during this session must be used by the map that is
    // already on screen, not only after the activity is recreated.
    LaunchedEffect(packVersion) {
        if (packVersion > 0) {
            OfflineMapPack(context).activate(mapView)
            mapView.invalidate()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    OfflineMapPack(context).activate(this)
                    setMultiTouchControls(true)
                    controller.setZoom(13.5)
                    controller.setCenter(GeoPoint(position.lat, position.lng))
                }
            },
            update = { mv ->
                mv.overlays.clear()
                mv.overlays.add(
                    mapTapOverlay(mv, data.destinations, onDestinationClick),
                )
                mv.overlays.add(userMarker(mv, context, position.lat, position.lng))
                data.destinations.forEach { destination ->
                    val route = data.routes[destination.routeId]
                    val terminal = data.terminals[destination.terminalId]
                    mv.overlays.add(
                        destinationMarker(
                            mapView = mv,
                            context = context,
                            destination = destination,
                            color = parseHexColor(
                                route?.colorHex ?: terminal?.colorHex,
                            ),
                            isSelected = selected?.id == destination.id,
                        ),
                    )
                }
                if (routePoints.size >= 2) {
                    mv.overlays.add(routePolyline(routePoints))
                }
                routeStops.forEach { stop ->
                    mv.overlays.add(
                        stopMarker(
                            mapView = mv,
                            context = context,
                            stop = stop,
                            color = parseHexColor(routeColorHex),
                        ),
                    )
                }
                if (cameraKey != lastCameraKey) {
                    lastCameraKey = cameraKey
                    val focusPoints = buildList {
                        add(GeoPoint(position.lat, position.lng))
                        selected?.let { add(GeoPoint(it.lat, it.lng)) }
                        routePoints.forEach { add(GeoPoint(it.lat, it.lng)) }
                        routeStops.forEach { add(GeoPoint(it.lat, it.lng)) }
                    }
                    // Manual camera fit: zoomToBoundingBox can hang forever in
                    // Projection.getCloserPixel on small/degenerate boxes.
                    val minLat = focusPoints.minOf { it.latitude }
                    val maxLat = focusPoints.maxOf { it.latitude }
                    val minLng = focusPoints.minOf { it.longitude }
                    val maxLng = focusPoints.maxOf { it.longitude }
                    val spanKm = Haversine.km(minLat, minLng, maxLat, maxLng)
                    mv.controller.setZoom(cameraZoomForKm(spanKm))
                    mv.controller.setCenter(
                        GeoPoint((minLat + maxLat) / 2.0, (minLng + maxLng) / 2.0),
                    )
                }
                mv.invalidate()
            },
        )
        FloatingActionButton(
            onClick = onShowSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 16.dp, end = 256.dp),
            containerColor = androidx.compose.ui.graphics.Color.White,
            contentColor = SagayColors.DeepOcean,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings and data")
        }
        FloatingActionButton(
            onClick = onShowFareCalculator,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 16.dp, end = 196.dp),
            containerColor = androidx.compose.ui.graphics.Color.White,
            contentColor = SagayColors.DeepOcean,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_payments),
                contentDescription = "Fare calculator",
            )
        }
        FloatingActionButton(
            onClick = onShowOfflinePack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 16.dp, end = 136.dp),
            containerColor = androidx.compose.ui.graphics.Color.White,
            contentColor = SagayColors.TealPrimary,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.Download, contentDescription = "Offline map")
        }
        FloatingActionButton(
            onClick = onShowHistory,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 16.dp, end = 76.dp),
            containerColor = androidx.compose.ui.graphics.Color.White,
            contentColor = SagayColors.TealPrimary,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Default.History, contentDescription = "Trip history")
        }
        FloatingActionButton(
            onClick = {
                locateRequest += 1
                onLocateMe()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 16.dp, end = 16.dp),
            containerColor = SagayColors.TealPrimary,
            contentColor = androidx.compose.ui.graphics.Color.White,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_my_location),
                contentDescription = "Locate me",
            )
        }
        ZoomControls(
            onZoom = smoothZoom,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 80.dp, end = 16.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            bottomContent()
        }
    }
}

/** Stacked zoom-out / zoom-in buttons on the map's left edge. */
@Composable
private fun ZoomControls(
    onZoom: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ZoomButton(label = "-", contentDescription = "Zoom out", onClick = { onZoom(-1f) })
        ZoomButton(label = "+", contentDescription = "Zoom in", onClick = { onZoom(1f) })
    }
}

@Composable
private fun ZoomButton(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    val pressDuration = Motion.duration(Motion.MICRO_MS)
    LaunchedEffect(pressed) {
        scale.animateTo(
            targetValue = if (pressed) 0.88f else 1f,
            animationSpec = tween(
                durationMillis = pressDuration,
                easing = Motion.easing,
            ),
        )
    }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.95f),
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SagayColors.TealPrimary,
            )
        }
    }
}

private fun mapTapOverlay(
    mapView: MapView,
    destinations: List<Destination>,
    onDestinationClick: (Destination) -> Unit,
): MapEventsOverlay =
    MapEventsOverlay(object : MapEventsReceiver {
        override fun singleTapConfirmedHelper(point: GeoPoint?): Boolean {
            if (point == null) return false
            nearestDestination(point.latitude, point.longitude, destinations)
                ?.let(onDestinationClick)
            return true
        }

        override fun longPressHelper(point: GeoPoint?): Boolean = false
    })

private fun userMarker(
    mapView: MapView,
    context: Context,
    lat: Double,
    lng: Double,
): Marker =
    Marker(mapView).apply {
        position = GeoPoint(lat, lng)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        icon = pinIcon(context, SagayColors.UserBlue.toArgb(), USER_RADIUS_DP)
        title = "You are here"
    }

/**
 * A locator pin: the teardrop that says "this is a place", with the route
 * colour inside it so the map reads at a glance.
 */
private fun pinIcon(
    context: Context,
    color: Int,
    sizeDp: Float,
    strokeColor: Int = android.graphics.Color.WHITE,
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val width = (sizeDp * density).toInt().coerceAtLeast(12)
    val height = (width * 1.35f).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = width / 2f
    val headRadius = width / 2f - 2f * density
    val headCy = headRadius + 2f * density
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    // Teardrop: a circle head with two straight sides down to the tip.
    val path = android.graphics.Path().apply {
        moveTo(cx - headRadius * 0.92f, headCy + headRadius * 0.55f)
        lineTo(cx, height - 2f * density)
        lineTo(cx + headRadius * 0.92f, headCy + headRadius * 0.55f)
        close()
    }
    canvas.drawPath(path, fill)
    canvas.drawCircle(cx, headCy, headRadius, fill)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    canvas.drawCircle(cx, headCy, headRadius, stroke)
    // The hole in the middle of the pin.
    val hole = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE }
    canvas.drawCircle(cx, headCy, headRadius * 0.38f, hole)
    return BitmapDrawable(context.resources, bitmap)
}

private fun destinationMarker(
    mapView: MapView,
    context: Context,
    destination: Destination,
    color: Int,
    isSelected: Boolean,
): Marker =
    Marker(mapView).apply {
        position = GeoPoint(destination.lat, destination.lng)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        icon = pinIcon(
            context = context,
            color = color,
            sizeDp = if (isSelected) SELECTED_RADIUS_DP else UNSELECTED_RADIUS_DP,
            strokeColor = if (isSelected) {
                SagayColors.SunriseDeep.toArgb()
            } else {
                android.graphics.Color.WHITE
            },
        )
        title = destination.name
    }

/**
 * A route stop marker: the route colour with the stop number inside, so the
 * order of the stops is readable straight from the map.
 */
private fun stopMarker(
    mapView: MapView,
    context: Context,
    stop: RouteStop,
    color: Int,
): Marker =
    Marker(mapView).apply {
        position = GeoPoint(stop.lat, stop.lng)
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = numberedCircleIcon(context, color, stop.order + 1)
        title = stop.name
    }

private fun numberedCircleIcon(
    context: Context,
    color: Int,
    number: Int,
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = (26f * density).toInt().coerceAtLeast(12)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = size / 2f
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    canvas.drawCircle(center, center, center - 2f * density, fill)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    canvas.drawCircle(center, center, center - 2f * density, stroke)
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = android.graphics.Color.BLACK
        textSize = 12f * density
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val baseline = center - (text.descent() + text.ascent()) / 2f
    canvas.drawText(number.toString(), center, baseline, text)
    return BitmapDrawable(context.resources, bitmap)
}

private fun routePolyline(points: List<RoutePoint>): Polyline =
    Polyline().apply {
        setPoints(points.map { GeoPoint(it.lat, it.lng) })
        outlinePaint.color = SagayColors.RouteBlue.toArgb()
        outlinePaint.strokeWidth = 10f
    }

private fun circleIcon(
    context: Context,
    color: Int,
    sizeDp: Float,
    strokeColor: Int = android.graphics.Color.WHITE,
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val size = (sizeDp * density).toInt().coerceAtLeast(8)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val radius = size / 2f - 2f * density
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    canvas.drawCircle(size / 2f, size / 2f, radius, fill)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }
    canvas.drawCircle(size / 2f, size / 2f, radius, stroke)
    return BitmapDrawable(context.resources, bitmap)
}

private fun nearestDestination(
    lat: Double,
    lng: Double,
    destinations: List<Destination>,
): Destination? {
    var best: Destination? = null
    var bestKm = Double.MAX_VALUE
    for (destination in destinations) {
        val km = Haversine.km(lat, lng, destination.lat, destination.lng)
        if (km < bestKm) {
            bestKm = km
            best = destination
        }
    }
    return if (bestKm <= TAP_THRESHOLD_KM) best else null
}

private fun cameraZoomForKm(km: Double): Double = when {
    km <= 1.0 -> 14.5
    km <= 3.0 -> 13.0
    km <= 10.0 -> 12.0
    km <= 30.0 -> 10.5
    else -> 9.0
}
