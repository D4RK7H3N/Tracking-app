package com.sagay.touristnav.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sagay.touristnav.R
import com.sagay.touristnav.domain.models.Destination
import com.sagay.touristnav.domain.models.Route
import com.sagay.touristnav.domain.models.Terminal
import com.sagay.touristnav.domain.models.VehicleType

@Composable
fun DestinationSheet(
    destinations: List<Destination>,
    terminals: Map<String, Terminal>,
    routes: Map<String, Route> = emptyMap(),
    favoriteIds: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {},
    onSelect: (Destination) -> Unit,
    onShowTerminals: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var routeFilter by rememberSaveable { mutableStateOf("All") }
    val filters = listOf("All", "Starred", "Red", "Yellow", "Pink", "Bus", "Boat")
    val filtered = remember(destinations, query, routes, routeFilter, favoriteIds) {
        val q = query.trim().lowercase()
        destinations.filter { destination ->
            val matchesQuery = q.isEmpty() ||
                destination.name.lowercase().contains(q) ||
                destination.category.lowercase().contains(q)
            val route = routes[destination.routeId]
            val matchesRoute = when (routeFilter) {
                "Starred" -> destination.id in favoriteIds
                "Bus" -> route?.vehicle == VehicleType.BUS
                "Boat" -> destination.requiresBoat || route?.vehicle == VehicleType.BOAT
                "All" -> true
                else -> route?.colorName.equals(routeFilter, ignoreCase = true) == true
            }
            matchesQuery && matchesRoute
        }
    }

    val layoutDuration = Motion.duration(Motion.LAYOUT_MS)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.42f)
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = layoutDuration,
                    easing = Motion.easing,
                ),
            ),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                singleLine = true,
                placeholder = { Text("Search a destination or category\u2026") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = SagayColors.TealPrimary,
                    unfocusedBorderColor = SagayColors.Hairline,
                ),
            )
            TerminalsButton(onShowTerminals)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                filters.forEach { label ->
                    FilterChip(
                        selected = routeFilter == label,
                        onClick = { routeFilter = label },
                        label = { Text(label) },
                    )
                }
            }
            Text(
                "${filtered.size} destination${if (filtered.size == 1) "" else "s"}" +
                    if (routeFilter == "All") "" else " \u00B7 $routeFilter",
                style = MaterialTheme.typography.labelMedium,
                color = SagayColors.SecondaryTeal,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
            )
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = SagayColors.InkMuted,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "No destinations match.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Try another search or filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(
                            onClick = {
                                query = ""
                                routeFilter = "All"
                            },
                        ) {
                            Text("Clear filters")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(filtered, key = { it.id }) { destination ->
                        val terminal = terminals[destination.terminalId]
                        val route = routes[destination.routeId]
                        ListItem(
                            headlineContent = {
                                Text(destination.name, fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = {
                                Text(
                                    listOfNotNull(
                                        destination.category,
                                        // Without a route the terminal's own
                                        // vehicles are not this
                                        // destination's service.
                                        route?.serviceLabel
                                            ?: terminal?.serviceLabel,
                                    ).joinToString(" \u00B7 "),
                                )
                            },
                            leadingContent = {
                                if (route != null) {
                                    RouteBadge(
                                        vehicle = route.vehicle,
                                        colorHex = route.colorHex,
                                        size = 36.dp,
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Color(
                                                    parseHexColor(
                                                        route?.colorHex ?: terminal?.colorHex,
                                                    ),
                                                ),
                                            ),
                                    )
                                }
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val favorite = destination.id in favoriteIds
                                    IconButton(
                                        onClick = { onToggleFavorite(destination.id) },
                                    ) {
                                        Icon(
                                            imageVector = if (favorite) {
                                                Icons.Default.Star
                                            } else {
                                                Icons.Default.StarBorder
                                            },
                                            contentDescription = if (favorite) {
                                                "Remove from favourites"
                                            } else {
                                                "Add to favourites"
                                            },
                                            tint = if (favorite) {
                                                SagayColors.SunriseDeep
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                    }
                                    Icon(
                                        Icons.Default.KeyboardArrowRight,
                                        contentDescription = null,
                                    )
                                }
                            },
                            modifier = Modifier.clickable { onSelect(destination) },
                        )
                    }
                }
            }
        }
    }
}

/** Opens the legend of terminals, route colours and vehicles. */
@Composable
private fun TerminalsButton(onShowTerminals: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onShowTerminals) {
            Icon(
                painter = painterResource(R.drawable.ic_groups),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                "Terminals & routes",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
