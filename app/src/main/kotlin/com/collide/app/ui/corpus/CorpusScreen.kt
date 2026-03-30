package com.collide.app.ui.corpus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.collide.app.domain.model.*
import com.collide.app.ui.collider.LabCard
import com.collide.app.ui.collider.MetricRow
import com.collide.app.ui.collider.formatBytes
import com.collide.app.ui.theme.CollideColors

@Composable
fun CorpusScreen(
    viewModel: CorpusViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedMode by remember { mutableStateOf(RunMode.SAFE) }
    var selectedBaseline by remember { mutableStateOf(BaselineStrategy.RAW_DEFLATE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CollideColors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CollideColors.surface)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = CollideColors.accent)
            }
            Column {
                Text("FIXTURE CORPUS RUN", color = CollideColors.accent, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("Run all fixtures, collect summary", color = CollideColors.muted,
                    fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LabCard(title = "CORPUS CONFIGURATION") {
                Text("Run Mode", color = CollideColors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RunMode.entries.forEach { mode ->
                        ModeChip(mode.displayName, selectedMode == mode, !uiState.isRunning) {
                            selectedMode = mode
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Baseline", color = CollideColors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BaselineStrategy.entries.forEach { s ->
                        ModeChip(s.displayName, selectedBaseline == s, !uiState.isRunning) {
                            selectedBaseline = s
                        }
                    }
                }
            }

            if (!uiState.isRunning) {
                Button(
                    onClick = {
                        viewModel.startCorpusRun(context, selectedMode, selectedBaseline,
                            maxCandidatesOverride = selectedMode.maxCandidates.coerceAtMost(40))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CollideColors.accent, contentColor = Color(0xFF003040)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text("RUN FIXTURE PACK", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            } else {
                Button(
                    onClick = viewModel::cancelRun,
                    colors = ButtonDefaults.buttonColors(containerColor = CollideColors.failure, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Stop, null)
                    Spacer(Modifier.width(8.dp))
                    Text("CANCEL", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                LabCard(title = "PROGRESS") {
                    Text(
                        "Testing fixture ${uiState.currentFixtureIndex + 1} / ${uiState.totalFixtures}",
                        color = CollideColors.onSurface, fontSize = 13.sp
                    )
                    if (uiState.currentFixture.isNotEmpty()) {
                        Text(uiState.currentFixture, color = CollideColors.muted, fontSize = 12.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { uiState.currentFixtureIndex.toFloat() / uiState.totalFixtures.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth(),
                        color = CollideColors.accent, trackColor = CollideColors.surfaceVariant
                    )
                }
            }

            uiState.error?.let { err ->
                Text(err, color = CollideColors.failure, fontSize = 13.sp,
                    modifier = Modifier.padding(8.dp))
            }

            uiState.summary?.let { summary ->
                CorpusSummaryCard(summary)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CorpusSummaryCard(summary: CorpusSummary) {
    LabCard(title = "CORPUS SUMMARY — LAB REPORT") {
        MetricRow("Run Mode", summary.runMode)
        MetricRow("Baseline", summary.baselineStrategy)
        MetricRow("Total Fixtures", summary.totalFixtures.toString())
        MetricRow("Fixtures with Wins", summary.fixturesWithWins.toString(),
            valueColor = if (summary.fixturesWithWins > 0) CollideColors.winner else CollideColors.onSurface)
        MetricRow("Fixtures without Wins", summary.fixturesWithoutWins.toString())
        MetricRow("Total Verified Winners", summary.totalWinners.toString(),
            valueColor = if (summary.totalWinners > 0) CollideColors.winner else CollideColors.onSurface)
        MetricRow("Total Candidates Evaluated", summary.totalCandidatesEvaluated.toString())
        MetricRow("Average Savings (winners)", "%.1f%%".format(summary.averageSavingsPct))
        MetricRow("Total Elapsed", "${summary.totalElapsedMs} ms")

        summary.bestCase?.let { best ->
            Spacer(Modifier.height(10.dp))
            SectionLabel("BEST CASE")
            MetricRow("Fixture", best.fixtureName)
            MetricRow("Best Savings", formatBytes(best.bestSavings), valueColor = CollideColors.winner)
            MetricRow("Savings %", "%.1f%%".format(best.bestSavingsPct), valueColor = CollideColors.winner)
        }

        summary.worstCase?.let { worst ->
            Spacer(Modifier.height(8.dp))
            SectionLabel("WORST CASE")
            MetricRow("Fixture", worst.fixtureName)
            MetricRow("Winners Found", worst.winnersFound.toString())
        }

        Spacer(Modifier.height(12.dp))
        SectionLabel("PER-FIXTURE RESULTS")
        Spacer(Modifier.height(4.dp))
        summary.fixtureResults.forEach { fr ->
            FixtureResultRow(fr)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(label, color = CollideColors.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp)
}

@Composable
private fun FixtureResultRow(fr: FixtureResult) {
    val hasBg = fr.hadWin
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (hasBg) CollideColors.winner.copy(0.08f) else CollideColors.surfaceVariant)
            .border(1.dp,
                if (hasBg) CollideColors.winner.copy(0.3f) else CollideColors.muted.copy(0.2f),
                RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(fr.fixtureName, color = CollideColors.onBackground, fontSize = 12.sp,
                fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${fr.candidatesEvaluated} evaluated · ${fr.exactnessFailures} failures · ${fr.elapsedMs}ms",
                color = CollideColors.muted, fontSize = 10.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            if (fr.hadWin) {
                Text("${fr.winnersFound} wins", color = CollideColors.winner, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold)
                Text("%.1f%%".format(fr.bestSavingsPct), color = CollideColors.winner, fontSize = 10.sp)
            } else {
                Text("No wins", color = CollideColors.muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ModeChip(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) CollideColors.accent.copy(0.2f) else CollideColors.surfaceVariant)
            .border(1.dp, if (selected) CollideColors.accent else CollideColors.muted.copy(0.3f),
                RoundedCornerShape(20.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = if (selected) CollideColors.accent else CollideColors.muted,
            fontSize = 12.sp, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
    }
}
