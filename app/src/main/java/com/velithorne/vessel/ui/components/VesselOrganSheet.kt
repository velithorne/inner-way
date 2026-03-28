package com.velithorne.vessel.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.velithorne.vessel.model.OrganInspectionState
import com.velithorne.vessel.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VesselOrganSheet(
    inspection: OrganInspectionState?,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible || inspection == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = inspection.info.displayName.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = inspection.info.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "Maps to: ${inspection.info.subsystemMapping}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = inspection.conditionSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            inspection.organState?.let { o ->
                OrganDetailRow("Health", Formatters.formatUnitInterval(o.health))
                OrganDetailRow("Activity", Formatters.formatUnitInterval(o.activity))
                OrganDetailRow("Load", Formatters.formatUnitInterval(o.load))
                OrganDetailRow("Inflammation", Formatters.formatUnitInterval(o.inflammation))
                OrganDetailRow("Reserve", Formatters.formatUnitInterval(o.reserve))
            } ?: OrganDetailRow("Metrics", Formatters.unavailable())
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = inspection.reactionNote,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Growth driver",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
            )
            Text(
                text = inspection.growthDriver,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = "Formation logic",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                text = inspection.formationLogic,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
