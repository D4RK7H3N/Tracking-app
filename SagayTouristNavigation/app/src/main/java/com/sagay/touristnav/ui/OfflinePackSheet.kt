package com.sagay.touristnav.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sagay.touristnav.platform.OfflineMapPack
import java.io.IOException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflinePackSheet(
    onDismiss: () -> Unit,
    onPackChanged: () -> Unit = {},
) {
    val context = LocalContext.current
    val pack = remember { OfflineMapPack(context) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var downloaded by remember { mutableStateOf(pack.isDownloaded()) }
    var sizeBytes by remember { mutableLongStateOf(pack.sizeBytes()) }
    var progress by remember { mutableIntStateOf(0) }
    val total = remember { pack.tileCount() }
    var downloading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

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
                "Offline map pack",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Sagay City, zoom 12\u201314 · $total tiles · " +
                    "%.1f MB".format(sizeBytes / (1024f * 1024f)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            if (downloaded) {
                Text(
                    "The map pack is on this device and is used automatically " +
                        "when there is no internet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    scope.launch {
                        pack.delete()
                        downloaded = false
                        sizeBytes = 0L
                        error = null
                        onPackChanged()
                    }
                }) {
                    Text("Delete offline map")
                }
            } else {
                if (downloading) {
                    LinearProgressIndicator(
                        progress = { if (total == 0) 0f else progress.toFloat() / total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("$progress of $total tiles")
                } else {
                    Text(
                        "Downloads the bounded city area for use without " +
                            "internet. Uses about 10\u201320 MB of storage.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        downloading = true
                        error = null
                        scope.launch {
                            try {
                                pack.download { done, _ -> progress = done }
                                downloaded = true
                                sizeBytes = pack.sizeBytes()
                                onPackChanged()
                            } catch (e: IOException) {
                                error = e.message ?: "Download failed. Check your connection."
                            } finally {
                                downloading = false
                            }
                        }
                    },
                    enabled = !downloading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (downloading) "Downloading…" else "Download offline map")
                }
            }
            error?.let { message ->
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}
