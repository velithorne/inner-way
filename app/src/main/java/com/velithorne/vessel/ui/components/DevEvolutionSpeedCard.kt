package com.velithorne.vessel.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velithorne.vessel.config.GrowthProfileScaler

/**
 * Debug dev mode: scales structural + visual evolution vs default dev [GrowthProfile] (1× = unchanged).
 */
@Composable
fun DevEvolutionSpeedCard(
    multiplier: Float,
    onMultiplierChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clamped = GrowthProfileScaler.clampMultiplier(multiplier)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "Evolution speed (dev)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Scales structural stages, branching drift, morphogenesis lerps, and seed-pod display rates. 1× matches the default dev profile.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp),
            )
            Slider(
                value = clamped,
                onValueChange = onMultiplierChange,
                valueRange = 0.25f..8f,
                steps = 30,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            Text(
                text = String.format("%.2f×", clamped),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            )
            OutlinedButton(
                onClick = { onMultiplierChange(1f) },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Reset to 1×", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
