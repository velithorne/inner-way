package com.velithorne.vessel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
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
import com.velithorne.vessel.ui.components.AmbientEcologyCard
import com.velithorne.vessel.ui.components.BranchAffinityCard
import com.velithorne.vessel.ui.components.BranchVariantCard
import com.velithorne.vessel.ui.components.JuvenileFormCard
import com.velithorne.vessel.ui.components.GenesisTraitCard
import com.velithorne.vessel.ui.components.MorphologyTraitCard
import com.velithorne.vessel.ui.components.BirthStateChip
import com.velithorne.vessel.ui.components.RegionChip
import com.velithorne.vessel.ui.components.TopologyStageChip
import com.velithorne.vessel.ui.components.AdaptationChip
import com.velithorne.vessel.ui.components.GrowthEventRow
import com.velithorne.vessel.ui.components.LineageCard
import com.velithorne.vessel.ui.components.SpecimenIdentityCard
import com.velithorne.vessel.viewmodel.LineageViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LineageScreen(
    viewModel: LineageViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.ui.collectAsState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Lineage",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        state.summary?.let { s ->
            Text(
                text = s.headline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            )
            s.subline?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                )
            }
        }
        state.lineage?.let { l ->
            SpecimenIdentityCard(lineage = l)
            BranchAffinityCard(lineage = l)
            BranchVariantCard(lineage = l)
        }
        if (state.genesisSummaryLines.isNotEmpty()) {
            GenesisTraitCard(title = "Birth topology", lines = state.genesisSummaryLines)
            state.genesisDriverLine?.let { drv ->
                Text(
                    text = "Genesis contour: $drv",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            state.birthStateChipLabel?.let { chip ->
                BirthStateChip(label = chip, modifier = Modifier.padding(top = 6.dp))
            }
        }
        if (state.juvenileFormLines.isNotEmpty()) {
            JuvenileFormCard(lines = state.juvenileFormLines)
            state.juvenileTopologyStageLine?.let { stage ->
                TopologyStageChip(
                    label = stage,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (state.juvenileTraitChips.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (chip in state.juvenileTraitChips) {
                        RegionChip(label = chip)
                    }
                }
            }
        }
        if (state.visibleTopologyLines.isNotEmpty() || state.morphologyDriverLine != null) {
            MorphologyTraitCard(
                title = "Visible topology",
                lines = state.visibleTopologyLines,
                driverLine = state.morphologyDriverLine,
            )
        }
        state.ambientEcology?.let { AmbientEcologyCard(state = it) }
        Text(
            text = "Adaptation shaping",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        )
        FlowRow {
            for (m in state.adaptations) {
                AdaptationChip(marker = m)
            }
        }
        LineageCard(transitions = state.stageTransitions)
        Text(
            text = "Recent growth events",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        )
        for (e in state.growthEvents.take(20)) {
            GrowthEventRow(event = e)
        }
    }
}
