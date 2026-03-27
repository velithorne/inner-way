package com.velithorne.vessel.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velithorne.vessel.model.AmbientEcologyUiState

@Composable
fun AmbientEcologyCard(
    state: AmbientEcologyUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Ambient ecology",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = state.dominantDriverLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = state.lastSampleAgoLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "Samples since last open: ${state.backgroundSamplesSinceOpen}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = state.workScheduledHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                modifier = Modifier.padding(top = 4.dp),
            )
            state.debugPanel?.let { d ->
                Text(
                    text = "DBG · work=${d.periodicWorkScheduled} · lastEvt=${d.lastAmbientEventLabel ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = "DBG · snapWrite=${d.lastSnapshotWriteMillis ?: "—"} · applied=${d.lastAppliedSnapshotMillis ?: "—"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
