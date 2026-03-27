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
import com.velithorne.vessel.lineage.StageTransition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LineageCard(
    transitions: List<StageTransition>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Stage history",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            )
            if (transitions.isEmpty()) {
                Text(
                    text = "No transitions recorded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                val fmt = SimpleDateFormat("MMM d HH:mm", Locale.getDefault())
                for (t in transitions.take(12)) {
                    Text(
                        text = "${fmt.format(Date(t.timestampMillis))} · ${t.explanation}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}
