package com.velithorne.vessel.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velithorne.vessel.lineage.GrowthEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GrowthEventRow(event: GrowthEvent, modifier: Modifier = Modifier) {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.timestampMillis))
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text(
            text = "$time — ${event.explanation}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f),
        )
        if (event.occurredDuringOfflineCatchUp) {
            Text(
                text = "offline catch-up",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
