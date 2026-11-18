package com.sagay.touristnav.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
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

@Composable
fun DestinationSheet(
    destinations: List<Destination>,
    terminals: Map<String, Terminal>,
    routes: Map<String, Route> = emptyMap(),
    onSelect: (Destination) -> Unit,
    onShowTerminals: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(destinations, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) {
            destinations
        } else {
            destinations.filter {
                it.name.lowercase().contains(q) || it.category.lowercase().contains(q)
            }
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
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
            AnimatedContent(
                targetState = filtered.isEmpty(),
                transitionSpec = {
                    fadeIn(tween(layoutDuration)) togetherWith fadeOut(tween(layoutDuration))
                },
                label = "destination-list",
                modifier = Modifier.fillMaxSize(),
            ) { empty ->
                if (empty) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No destinations match your search.")
                    }
                } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
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
                                                        route?.colorHex,
                                                    ),
                                                ),
                                            ),
                                    )
                                }
                            },
                            trailingContent = {
                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                            },
                            modifier = Modifier.clickable { onSelect(destination) },
                        )
                    }
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
