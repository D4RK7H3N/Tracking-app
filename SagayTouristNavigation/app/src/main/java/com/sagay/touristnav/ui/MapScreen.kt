package com.sagay.touristnav.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagay.touristnav.R
import com.sagay.touristnav.domain.Haversine
import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.RoutePoint
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
    val context = LocalContext.current

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

    when (val s = state) {
        is UiState.NeedsPermission -> PermissionPrompt(
            title = "Location needed",
            message = "Sagay Tourist Navigation needs your location to measure " +
                "road distance and estimate tricycle fares.",
            buttonLabel = "Allow location",
            onAction = { launcher.launch(permissions) },
        )

        is UiState.Locating -> CenterMessage("Finding your location…")

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
            bottomContent = {
                DestinationSheet(
                    destinations = s.data.destinations,
                    terminals = s.data.terminals,
                    onSelect = viewModel::onDestinationSelected,
                )
            },
        )

        is UiState.DestinationSelected -> TripMap(
            data = s.data,
            selected = s.selected,
            routePoints = emptyList(),
            onDestinationClick = viewModel::onDestinationSelected,
            onLocateMe = viewModel::locateMe,
            bottomContent = { RouteProgressCard("Looking up the road route…") },
        )

        is UiState.FetchingRoute -> TripMap(
            data = s.data,
            selected = s.selected,
            routePoints = emptyList(),
            onDestinationClick = viewModel::onDestinationSelected,
            onLocateMe = viewModel::locateMe,
            bottomContent = { RouteProgressCard("Fetching road distance…") },
        )

        is UiState.WaitingForInternet -> TripMap(
            data = s.data,
            selected = s.selected,
            routePoints = emptyList(),
            onDestinationClick = viewModel::onDestinationSelected,
            onLocateMe = viewModel::locateMe,
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
            bottomContent = {
                TripDetailsCard(
                    trip = s.trip,
                    onSelectNew = viewModel::selectNewDestination,
                )
            },
        )
    }
}

@Composable
private fun TripMap(
    data: MapData,
    selected: Destination?,
    routePoints: List<RoutePoint>,
    onDestinationClick: (Destination) -> Unit,
    onLocateMe: () -> Unit,
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

    LaunchedEffect(locateRequest, position.lat, position.lng) {
        if (locateRequest > 0) {
            mapView.controller.setZoom(14.5)
            mapView.controller.setCenter(GeoPoint(position.lat, position.lng))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK)
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
                    val terminal = data.terminals[destination.terminalId]
                    mv.overlays.add(
                        destinationMarker(
                            mapView = mv,
                            context = context,
                            destination = destination,
                            color = parseHexColor(terminal?.colorHex),
                            isSelected = selected?.id == destination.id,
                        ),
                    )
                }
                if (routePoints.size >= 2) {
                    mv.overlays.add(routePolyline(routePoints))
                }
                if (cameraKey != lastCameraKey) {
                    lastCameraKey = cameraKey
                    val focusPoints = buildList {
                        add(GeoPoint(position.lat, position.lng))
                        selected?.let { add(GeoPoint(it.lat, it.lng)) }
                        routePoints.forEach { add(GeoPoint(it.lat, it.lng)) }
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
            onZoomOut = mapView.controller::zoomOut,
            onZoomIn = mapView.controller::zoomIn,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp),
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
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ZoomButton(label = "−", contentDescription = "Zoom out", onClick = onZoomOut)
        ZoomButton(label = "+", contentDescription = "Zoom in", onClick = onZoomIn)
    }
}

@Composable
private fun ZoomButton(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.95f),
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        modifier = Modifier.size(44.dp),
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
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = circleIcon(context, SagayColors.UserBlue.toArgb(), USER_RADIUS_DP)
        title = "You are here"
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
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = circleIcon(
            context = context,
            color = color,
            sizeDp = if (isSelected) SELECTED_RADIUS_DP else UNSELECTED_RADIUS_DP,
            strokeColor = if (isSelected) SagayColors.SunriseDeep.toArgb() else android.graphics.Color.WHITE,
        )
        title = destination.name
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
