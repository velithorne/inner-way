package com.velithorne.vessel.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velithorne.vessel.lineage.AdaptationMarker
import com.velithorne.vessel.util.Formatters

@Composable
fun AdaptationChip(marker: AdaptationMarker, modifier: Modifier = Modifier) {
    SuggestionChip(
        onClick = { },
        label = {
            Text(
                text = "${marker.explanationLabel} · ${Formatters.formatUnitInterval(marker.visibleBiasApplied)}",
                style = MaterialTheme.typography.labelSmall,
            )
        },
        modifier = modifier.padding(end = 6.dp, bottom = 6.dp),
    )
}
