package com.velithorne.vessel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.velithorne.vessel.physiology.SpeciesState
import com.velithorne.vessel.ui.components.MetricCard
import com.velithorne.vessel.ui.components.OrganStateCard
import com.velithorne.vessel.ui.components.SectionHeader
import com.velithorne.vessel.ui.components.StatusChip
import com.velithorne.vessel.ui.components.VitalBar
import com.velithorne.vessel.util.Formatters
import com.velithorne.vessel.viewmodel.TelemetryViewModel

@Composable
fun PhysiologyDebugScreen(
    viewModel: TelemetryViewModel,
    modifier: Modifier = Modifier,
) {
    val snap by viewModel.physiology.collectAsState()
    val scroll = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Synthetic anatomy",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Classified biosign readout · phase 2",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
                StatusChip(label = "LIVE", highlight = true)
            }
            Text(
                text = "Sync: ${Formatters.formatTimestampMillis(snap.timestampMillis)} · ${snap.species.healthLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        SpeciesCard(snap.species)
        MetricCard {
            SectionHeader(
                title = "Organ lattice",
                subtitle = "Phase 3 renderer binds motion to these nodes",
            )
        }
        snap.organs.forEach { organ ->
            OrganStateCard(state = organ)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SpeciesCard(s: SpeciesState) {
    MetricCard {
        SectionHeader(
            title = "Whole organism",
            subtitle = s.stateSummary,
        )
        Text(
            text = s.healthLabel,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        VitalBar("Vitality", s.vitality)
        VitalBar("Stress", s.stress)
        VitalBar("Hunger", s.hunger)
        VitalBar("Fever", s.fever)
        VitalBar("Recovery", s.recovery)
        VitalBar("Respiration", s.respiration)
        VitalBar("Neural activity", s.neuralActivity)
        VitalBar("Mobility", s.mobility)
        VitalBar("Sleep pressure", s.sleepPressure)
        VitalBar("Signal arousal", s.signalArousal)
        VitalBar("Structural load", s.structuralLoad)
    }
}
