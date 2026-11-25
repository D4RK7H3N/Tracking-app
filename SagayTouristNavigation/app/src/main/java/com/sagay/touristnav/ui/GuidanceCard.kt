package com.sagay.touristnav.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sagay.touristnav.domain.GuidanceSnapshot
import java.util.Locale

@Composable
fun GuidanceCard(
    snapshot: GuidanceSnapshot,
    destinationName: String,
    onEnd: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                destinationName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "${String.format(Locale.US, "%.1f", snapshot.remainingKm)} km remaining",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = SagayColors.TealPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${String.format(Locale.US, "%.1f", snapshot.travelledKm)} km travelled",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { snapshot.progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                snapshot.nextPrompt,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onEnd, modifier = Modifier.fillMaxWidth()) {
                Text("End guidance")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
