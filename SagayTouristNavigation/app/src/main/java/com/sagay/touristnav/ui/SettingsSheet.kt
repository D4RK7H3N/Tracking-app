package com.sagay.touristnav.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement

/**
 * Settings & data: motion preference, destructive data actions and the
 * provenance caveat that must travel with every researched value.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    viewModel: TripViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    MotionPrefs.ensureLoaded(context.applicationContext)
    val override = MotionPrefs.override.value
    val systemReduced = MotionPrefs.systemReduced(context)
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val version = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SagayColors.WarmMist,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                "Settings & data",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = SagayColors.DeepOcean,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Preferences stay on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = SagayColors.InkMuted,
            )
            Spacer(Modifier.height(20.dp))

            SheetSection("Motion") {
                Text(
                    "System reduce motion: ${if (systemReduced) "On" else "Off"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SagayColors.Ink,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Choose how animations behave inside this app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SagayColors.InkMuted,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val choices: List<Pair<Boolean?, String>> = listOf(
                        null to "Follow system",
                        false to "Full motion",
                        true to "Reduce motion",
                    )
                    choices.forEach { (value, label) ->
                        FilterChip(
                            selected = override == value,
                            onClick = { MotionPrefs.setOverride(context, value) },
                            label = { Text(label) },
                        )
                    }
                }
            }

            SheetSection("Trip data") {
                if (!confirmClear) {
                    OutlinedButton(onClick = { confirmClear = true }) {
                        Text("Clear trip history")
                    }
                } else {
                    Text(
                        "Delete all saved trips? This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { confirmClear = false }) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                viewModel.clearHistory()
                                confirmClear = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SagayColors.Coral,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text("Clear all")
                        }
                    }
                }
            }

            SheetSection("About") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    color = Color.White,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Sagay Tourist Navigator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Version $version",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SagayColors.InkMuted,
                        )
                        Text(
                            "Package ${context.packageName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SagayColors.InkMuted,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Data caveat: vehicle colours, coordinates and fares " +
                                "are researched placeholders. They are not official " +
                                "or verified data and must be confirmed with the " +
                                "Sagay City Tourism Office and LTFRB-franchised " +
                                "operators before relying on them.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SagayColors.Ink,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SheetSection(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = SagayColors.SecondaryTeal,
    )
    Spacer(Modifier.height(8.dp))
    content()
    Spacer(Modifier.height(22.dp))
}
