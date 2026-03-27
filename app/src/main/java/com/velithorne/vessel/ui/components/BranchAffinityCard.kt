package com.velithorne.vessel.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velithorne.vessel.lineage.SpecimenLineage

@Composable
fun BranchAffinityCard(
    lineage: SpecimenLineage,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Morphology branching",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = lineage.branchFamilyBlurb,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 6.dp),
            )
            BranchTendencyRow(
                leadingLabel = lineage.leadingBranchLabel,
                readinessPercent = lineage.branchReadinessPercent,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            BranchReadinessBar(fraction = lineage.branchReadinessPercent / 100f)
            if (lineage.branchAffinityPercentsLine.isNotEmpty()) {
                Text(
                    text = lineage.branchAffinityPercentsLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                text = lineage.branchSummaryLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = lineage.branchReasonLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
