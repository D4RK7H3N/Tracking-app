package com.sagay.touristnav.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorySheet(
    viewModel: TripViewModel,
    onDismiss: () -> Unit,
) {
    val records by viewModel.history.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val total = records.sumOf { it.total }
    val mostUsed = records
        .groupingBy { it.vehicleLabel }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
        .orEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                "Trip history",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${records.size} trips \u00B7 \u20B1${"%.2f".format(total)} total",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (mostUsed.isNotEmpty()) {
                Text(
                    "Most used service: $mostUsed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            if (records.isEmpty()) {
                Text(
                    "No saved trips yet. Open a destination and choose " +
                        "\"Save to trip history\".",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(records, key = { it.id }) { record ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    record.destinationName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    listOfNotNull(
                                        record.originZone.ifEmpty { null },
                                        record.vehicleLabel.ifEmpty { null },
                                        if (record.discounted) "discounted" else null,
                                        "${record.passengers} passenger(s)",
                                    ).joinToString(" \u00B7 "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "\u20B1${"%.2f".format(record.total)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            IconButton(onClick = { viewModel.deleteTrip(record.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete trip",
                                )
                            }
                        }
                    }
                }
                TextButton(onClick = viewModel::clearHistory) {
                    Text("Clear all history")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
