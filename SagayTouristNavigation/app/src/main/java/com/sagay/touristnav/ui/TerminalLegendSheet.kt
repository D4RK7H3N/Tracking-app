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
import com.sagay.touristnav.domain.models.Terminal

/**
 * The tourist cheat sheet: every terminal with its route colour, the vehicle
 * that boards there, and the areas it serves.
 *
 * This answers the question the map markers cannot — "which one is the red
 * one, and where do I catch it?".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalLegendSheet(
    terminals: List<Terminal>,
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
                "Terminals & routes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Every service is known by its colour — ask for it by name.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            terminals.forEach { terminal ->
                TerminalLegendRow(terminal)
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun TerminalLegendRow(terminal: Terminal) {
    Row(verticalAlignment = Alignment.Top) {
        val vehicle = terminal.vehicles.firstOrNull()
        if (vehicle != null) {
            RouteBadge(
                vehicle = vehicle,
                colorHex = terminal.colorHex,
                size = 44.dp,
            )
        } else {
            Spacer(Modifier.size(44.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                terminal.serviceLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                terminal.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (terminal.serviceArea.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    terminal.serviceArea,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val extra = terminal.vehicles.drop(1)
            if (extra.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    extra.forEach { other ->
                        VehicleIcon(
                            vehicle = other,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                            contentDescription = other.displayName,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            other.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                }
            }
        }
    }
}
