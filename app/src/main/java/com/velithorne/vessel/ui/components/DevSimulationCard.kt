package com.velithorne.vessel.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velithorne.vessel.config.SimulationMode

/**
 * Debug-only: shows active simulation profile and optional reset (not for release users).
 */
@Composable
fun DevSimulationCard(
    mode: SimulationMode,
    onResetSpecimen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = when (mode) {
                    SimulationMode.DEV_SIMULATION -> "Dev simulation mode · accelerated growth profile"
                    SimulationMode.RELEASE_REALTIME -> "Release growth profile"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Fresh specimen per dev build · tuning from GrowthProfile",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp),
            )
            OutlinedButton(
                onClick = onResetSpecimen,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Reset specimen now", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
