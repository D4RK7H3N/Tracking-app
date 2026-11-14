package com.sagay.touristnav.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sagay.touristnav.R
import com.sagay.touristnav.domain.models.BusRoute
import com.sagay.touristnav.domain.models.Terminal
import com.sagay.touristnav.domain.models.TripEstimate
import java.util.Locale
import kotlin.math.roundToInt

/**
 * V4 result card: the sunrise gradient strip answers the two questions
 * tourists ask first (how far, how much) with icons, and the details below
 * tell them **which vehicle to board** â€” the part a text-only card never
 * answered.
 */
@Composable
fun TripDetailsCard(
    trip: TripEstimate,
    onSelectNew: () -> Unit,
    onToggleDiscount: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SagayColors.bannerGradient)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    BannerLine(
                        iconRes = R.drawable.ic_route,
                        text = String.format(Locale.US, "%.1f km", trip.roadKm),
                    )
                    Spacer(Modifier.height(2.dp))
                    BannerLine(
                        iconRes = R.drawable.ic_schedule,
                        text = formatDuration(trip.durationMinutes),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "estimate only",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_payments),
                        contentDescription = "Fare",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        String.format(Locale.US, "â‚±%.0f", trip.fare),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                trip.destination.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                trip.destination.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (trip.boatNote != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_directions_boat),
                        contentDescription = "Boat",
                        tint = SagayColors.BoatTeal,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        trip.boatNote,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SagayColors.BoatTeal,
                    )
                }
            }
            if (trip.distanceWarning) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Distance looks unusual, please verify",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(14.dp))
            TerminalRow(trip.terminal)
            if (trip.busRoute != null && trip.busRoute.stops.size > 1) {
                Spacer(Modifier.height(12.dp))
                BusStopsBlock(trip.busRoute)
            }
            if (trip.originZoneName != null) {
                Spacer(Modifier.height(10.dp))
                IconDetailRow(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_route),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    label = "Fare zone",
                    value = trip.originZoneName,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                trip.fareBasis.ifBlank { "Fare basis: ${trip.fareSource}" },
                style = MaterialTheme.typography.bodySmall,
                color = if (trip.fareIsOfficial) {
                    SagayColors.BoatTeal
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = if (trip.fareIsOfficial) FontWeight.SemiBold else FontWeight.Normal,
            )
            Spacer(Modifier.height(10.dp))
            DiscountToggle(
                checked = trip.fareDiscounted,
                onToggle = onToggleDiscount,
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onSelectNew,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SagayColors.SunriseDeep,
                    contentColor = Color.White,
                ),
            ) {
                Text("Select new destination")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * The stops of a bus / jeepney route. Tricycle trips have no stops, so the
 * block only appears for bus services.
 */
@Composable
private fun BusStopsBlock(route: BusRoute) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_directions_bus),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                route.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        route.stops.forEach { stop ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(parseHexColor(route.colorHex))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        (stop.order + 1).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    stop.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The board here: the vehicle badge in the route colour, the terminal name,
 * and the service description ("Red Tricycle", "White Bus + Boat").
 */
@Composable
private fun TerminalRow(terminal: Terminal) {
    val vehicle = terminal.vehicles.firstOrNull()
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (vehicle != null) {
            RouteBadge(
                vehicle = vehicle,
                colorHex = terminal.colorHex,
                size = 40.dp,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(parseHexColor(terminal.colorHex))),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                terminal.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                terminal.serviceLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (terminal.serviceArea.isNotBlank()) {
                Text(
                    terminal.serviceArea,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** One line of the gradient banner: an icon and its value. */
@Composable
private fun BannerLine(iconRes: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

/**
 * 20% student / senior citizen / PWD fare, as published in the city's
 * temporary fare matrix.
 */
@Composable
private fun DiscountToggle(
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (checked) {
                    SagayColors.TealPrimary.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (checked) SagayColors.TealPrimary else Color.Transparent,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Text(
                    "âœ“",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Senior citizen / Student / PWD",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "20% discount (matrix fare)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun RouteProgressCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun RouteErrorCard(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Could not load the route",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(6.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text("Retry")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun formatDuration(minutes: Double): String =
    if (minutes < 60.0) {
        "${minutes.roundToInt()} min"
    } else {
        val h = (minutes / 60).toInt()
        val m = (minutes % 60).roundToInt()
        if (m == 0) "${h}h" else "${h}h ${m}m"
    }
