package com.sagay.touristnav.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sagay.touristnav.domain.models.Route
import com.sagay.touristnav.domain.models.Terminal

/**
 * The tourist cheat sheet: every route with the colour of the vehicle to ask
 * for, where to catch it, and the stops it passes.
 *
 * This answers the question the map markers cannot ? "which one is the red
 * one, and where do I catch it?".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalLegendSheet(
    routes: List<Route>,
    terminals: Map<String, Terminal>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                "Routes & terminals",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Every service is known by its colour \u2014 ask for it by name.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            routes.forEach { route ->
                RouteLegendRow(route, terminals[route.terminalId])
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun RouteLegendRow(route: Route, terminal: Terminal?) {
    Row(verticalAlignment = Alignment.Top) {
        RouteBadge(
            vehicle = route.vehicle,
            colorHex = route.colorHex,
            size = 44.dp,
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                route.serviceLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                route.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (terminal != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Catch it at ${terminal.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (route.stops.size > 1) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Stops: " + route.stops.joinToString(" \u2192 ") { it.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
