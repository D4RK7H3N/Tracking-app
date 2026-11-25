package com.sagay.touristnav.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sagay.touristnav.domain.models.Route
import com.sagay.touristnav.domain.models.Terminal

/**
 * The tourist cheat sheet: every route with the colour of the vehicle to ask
 * for, where to catch it, and the stops it passes.
 *
 * Now searchable by route name, colour/vehicle label, terminal name or stop
 * name, so a large legend stays usable offline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalLegendSheet(
    routes: List<Route>,
    terminals: Map<String, Terminal>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(routes, terminals, query) {
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            routes
        } else {
            routes.filter { route ->
                val terminal = terminals[route.terminalId]
                listOfNotNull(
                    route.name,
                    route.serviceLabel,
                    route.colorName,
                    route.vehicle.displayName,
                    terminal?.name,
                    terminal?.serviceArea,
                ).plus(route.stops.map { it.name })
                    .any { it.contains(q, ignoreCase = true) }
            }
        }
    }
    val layoutDuration = Motion.duration(Motion.LAYOUT_MS)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SagayColors.WarmMist,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = layoutDuration,
                        easing = Motion.easing,
                    ),
                )
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                "Routes & terminals",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = SagayColors.DeepOcean,
            )
            Text(
                "Every service is known by its colour \u2014 ask for it by name.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search route, colour, vehicle or terminal\u2026") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = SagayColors.DeepOcean,
                    unfocusedBorderColor = SagayColors.SoftHairline,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${filtered.size} route${if (filtered.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                color = SagayColors.SecondaryTeal,
            )
            Spacer(Modifier.height(12.dp))
            if (filtered.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "No routes match \u201c$query\u201d.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Try a colour, a vehicle or a terminal name.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (query.isNotBlank()) {
                        TextButton(onClick = { query = "" }) {
                            Text("Clear search")
                        }
                    }
                }
            } else {
                filtered.forEach { route ->
                    RouteLegendRow(route, terminals[route.terminalId])
                    Spacer(Modifier.height(14.dp))
                }
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
                fontWeight = FontWeight.Bold,
                color = SagayColors.Ink,
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
                    color = SagayColors.SecondaryTeal,
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
