package com.sagay.touristnav.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sagay.touristnav.data.repo.TripRepository
import com.sagay.touristnav.domain.FareFormat
import com.sagay.touristnav.domain.FareMatrixLookup
import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.FareMatrixEntry
import com.sagay.touristnav.domain.models.FareOrigin
import com.sagay.touristnav.domain.models.FareRule
import com.sagay.touristnav.domain.models.Route
import com.sagay.touristnav.domain.models.VehicleType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Everything the tool needs, read straight from the bundled Room database. */
data class FareData(
    val origins: List<FareOrigin> = emptyList(),
    val routes: List<Route> = emptyList(),
    val destinations: List<Destination> = emptyList(),
    val matrix: List<FareMatrixEntry> = emptyList(),
    val rule: FareRule? = null,
)

/** The result of a matrix lookup for one origin zone and one service route. */
data class FareQuote(
    val originName: String? = null,
    val serviceLabel: String? = null,
    val vehicle: VehicleType? = null,
    val destinationZone: String? = null,
    val regular: Double? = null,
    val discounted: Double? = null,
    val basis: String = "Choose an origin zone and a route to see the matrix fare.",
) {
    val available: Boolean get() = regular != null && discounted != null
}

@HiltViewModel
class FareCalculatorViewModel @Inject constructor(
    private val repository: TripRepository,
) : ViewModel() {

    private val _data = MutableStateFlow(FareData())
    val data: StateFlow<FareData> = _data.asStateFlow()

    init {
        viewModelScope.launch {
            // All of this is bundled in Room, so the tool works offline.
            _data.value = runCatching {
                FareData(
                    origins = repository.fareOrigins(),
                    routes = repository.routes(),
                    destinations = repository.destinations(),
                    matrix = repository.fareMatrix(),
                    rule = repository.fareRule(),
                )
            }.getOrDefault(FareData())
        }
    }

    /**
     * Finds the published matrix row for [originZone] that applies to one of
     * the destinations served by [routeId]. Zone matching reuses the same
     * tolerant lookup as trip estimation, so the two surfaces agree.
     */
    fun quote(originZone: String?, routeId: String?): FareQuote {
        val snapshot = _data.value
        val origin = snapshot.origins.firstOrNull { it.zone == originZone }
        val route = snapshot.routes.firstOrNull { it.id == routeId }
        if (origin == null || route == null) return FareQuote()

        val zones = snapshot.destinations
            .filter { it.routeId == route.id }
            .mapNotNull { it.fareZone }
        val entry = zones
            .asSequence()
            .mapNotNull { zone -> FareMatrixLookup.find(snapshot.matrix, origin.zone, zone) }
            .firstOrNull()

        if (entry == null) {
            return FareQuote(
                originName = origin.name,
                serviceLabel = route.serviceLabel,
                vehicle = route.vehicle,
                basis = "No published matrix row matches this origin and route. " +
                    "The fare needs local verification.",
            )
        }

        val caveat = when (route.vehicle) {
            VehicleType.BOAT ->
                "Boat fares are not published; this is the tricycle matrix fare for " +
                    entry.destinationZone + "."
            else -> "Official fare matrix: ${origin.name} \u2192 ${entry.destinationZone}"
        }
        return FareQuote(
            originName = origin.name,
            serviceLabel = route.serviceLabel,
            vehicle = route.vehicle,
            destinationZone = entry.destinationZone,
            regular = entry.regularFare,
            discounted = entry.discountedFare,
            basis = caveat,
        )
    }
}

/**
 * Fare calculator tool. Pick an origin zone and a route, then compare the
 * official regular fare with the 20% discounted fare and price a group.
 */
@Composable
fun FareCalculatorScreen(
    onDismiss: () -> Unit,
    viewModel: FareCalculatorViewModel = hiltViewModel(),
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    var originZone by rememberSaveable { mutableStateOf("") }
    var routeId by rememberSaveable { mutableStateOf("") }
    var useDiscount by rememberSaveable { mutableStateOf(false) }
    var passengers by rememberSaveable { mutableIntStateOf(1) }
    val layoutDuration = Motion.duration(Motion.LAYOUT_MS)
    val quote = viewModel.quote(
        originZone.takeIf { it.isNotBlank() },
        routeId.takeIf { it.isNotBlank() },
    )
    val unitFare = when {
        !quote.available -> null
        useDiscount -> quote.discounted
        else -> quote.regular
    }

    Surface(modifier = Modifier.fillMaxSize(), color = SagayColors.WarmMist) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back to map")
                }
                Column {
                    Text(
                        "Fare calculator",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SagayColors.DeepOcean,
                    )
                    Text(
                        "Works offline \u00B7 bundled city matrix",
                        style = MaterialTheme.typography.bodySmall,
                        color = SagayColors.InkMuted,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = SagayColors.SunriseGold.copy(alpha = 0.18f),
            ) {
                Text(
                    "Fares are researched estimates from the official matrix and " +
                        "must be verified with local operators before use.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SagayColors.Ink,
                    modifier = Modifier.padding(16.dp),
                )
            }
            data.rule?.let { rule ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "Distance rule: \u20B1${rule.baseFare.toInt()} first " +
                        "${rule.baseKm.toInt()} km, +\u20B1${rule.extraPerKm.toInt()} " +
                        "per started km (${rule.source}).",
                    style = MaterialTheme.typography.bodySmall,
                    color = SagayColors.InkMuted,
                )
            }
            Spacer(Modifier.height(20.dp))

            if (data.origins.isEmpty() || data.routes.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                ) {
                    Text(
                        "Loading bundled fare data\u2026",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            SectionTitle("1. Where are you starting?")
            ChipRow {
                data.origins.forEach { origin ->
                    FilterChip(
                        selected = originZone == origin.zone,
                        onClick = { originZone = origin.zone },
                        label = { Text(origin.name) },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            SectionTitle("2. Which route will you take?")
            ChipRow {
                data.routes.forEach { route ->
                    FilterChip(
                        selected = routeId == route.id,
                        onClick = { routeId = route.id },
                        label = { Text(route.serviceLabel) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(
                                        Color(parseHexColor(route.colorHex)),
                                        CircleShape,
                                    ),
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            AnimatedVisibility(
                visible = quote.available || (originZone.isNotBlank() && routeId.isNotBlank()),
                enter = fadeIn(tween<Float>(layoutDuration)) +
                    expandVertically(tween<IntSize>(layoutDuration)),
                exit = fadeOut(tween<Float>(layoutDuration)) +
                    shrinkVertically(tween<IntSize>(layoutDuration)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = tween(
                                durationMillis = layoutDuration,
                                easing = Motion.easing,
                            ),
                        ),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                quote.serviceLabel.orEmpty(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SagayColors.DeepOcean,
                            )
                            Text(
                                quote.basis,
                                style = MaterialTheme.typography.bodySmall,
                                color = SagayColors.SecondaryTeal,
                            )
                            Spacer(Modifier.height(16.dp))
                            if (quote.available) {
                                FareRow(
                                    label = "Regular fare",
                                    amount = FareFormat.peso(quote.regular ?: 0.0),
                                    emphasized = !useDiscount,
                                )
                                Spacer(Modifier.height(8.dp))
                                FareRow(
                                    label = "20% discounted fare",
                                    amount = FareFormat.peso(quote.discounted ?: 0.0),
                                    emphasized = useDiscount,
                                )
                                Spacer(Modifier.height(16.dp))
                                FilterChip(
                                    selected = useDiscount,
                                    onClick = { useDiscount = !useDiscount },
                                    label = { Text("Apply 20% discount") },
                                    leadingIcon = {
                                        if (useDiscount) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SagayColors.DeepOcean,
                                        selectedLabelColor = Color.White,
                                    ),
                                )
                            } else {
                                Text(
                                    "No verified matrix fare for this pair. " +
                                        "Please ask at the terminal.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Group total",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Passengers",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { passengers = (passengers - 1).coerceIn(1, 20) }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Fewer passengers")
                                }
                                Text(
                                    "$passengers",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                IconButton(onClick = { passengers = (passengers + 1).coerceIn(1, 20) }) {
                                    Icon(Icons.Default.Add, contentDescription = "More passengers")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (unitFare != null) {
                                    FareFormat.peso(unitFare * passengers)
                                } else {
                                    "Not available without a verified fare"
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = SagayColors.Coral,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SagayColors.DeepOcean,
                    contentColor = Color.White,
                ),
            ) {
                Text("Back to map")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = SagayColors.Ink,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun FareRow(label: String, amount: String, emphasized: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = SagayColors.InkMuted,
            modifier = Modifier.weight(1f),
        )
        Text(
            amount,
            style = if (emphasized) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = if (emphasized) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (emphasized) SagayColors.DeepOcean else SagayColors.InkMuted,
        )
    }
}
