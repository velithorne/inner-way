package com.collide.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.collide.app.ui.theme.CollideColors

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCollider: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCorpus: () -> Unit
) {
    val eventCount by viewModel.eventCount.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CollideColors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))

        Text("COLLIDE", color = CollideColors.accent, fontSize = 52.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 8.sp,
            style = MaterialTheme.typography.displayMedium)

        Spacer(Modifier.height(6.dp))
        Text("COMPRESSION RESEARCH LAB · PHASE 2", color = CollideColors.muted,
            fontSize = 10.sp, letterSpacing = 2.sp, style = MaterialTheme.typography.labelSmall)

        Spacer(Modifier.height(14.dp))
        Text(
            "Generate, test, verify, and archive\nexact-reconstruction compression events.",
            color = CollideColors.onSurface, fontSize = 14.sp,
            textAlign = TextAlign.Center, lineHeight = 22.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "All winners require byte-exact reconstruction + SHA-256 hash match.",
            color = CollideColors.muted, fontSize = 12.sp, textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        ColliderMainCard(onClick = onNavigateToCollider, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("LAB DISCOVERIES", eventCount.toString(), CollideColors.winner,
                modifier = Modifier.weight(1f), onClick = onNavigateToArchive)
            StatCard("PHASE", "2", CollideColors.accent,
                modifier = Modifier.weight(1f), onClick = null)
        }

        Spacer(Modifier.height(16.dp))

        LabActionRow(Icons.Default.TableView, "Fixture Corpus Run",
            "Test all fixtures, produce lab report", onNavigateToCorpus)
        Spacer(Modifier.height(10.dp))
        LabActionRow(Icons.Default.Storage, "Lab Discoveries Archive",
            "Browse verified winning compressions", onNavigateToArchive)
        Spacer(Modifier.height(10.dp))
        LabActionRow(Icons.Default.Settings, "Settings",
            "Configure defaults, transforms, limits", onNavigateToSettings)

        Spacer(Modifier.height(28.dp))
        Text("PHASE 2 · HONEST COMPRESSION RESEARCH UPGRADE",
            color = CollideColors.muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ColliderMainCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF001A2E), Color(0xFF00101F))))
            .border(
                1.dp,
                Brush.horizontalGradient(listOf(CollideColors.accent.copy(0.8f), CollideColors.accentAlt.copy(0.6f))),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Science, null, tint = CollideColors.accent, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text("COMPRESSION COLLIDER", color = CollideColors.accent, fontSize = 16.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Text("Load · Generate · Verify Exact Reconstruction · Archive Winners",
                color = CollideColors.onSurface, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = CollideColors.accent, contentColor = Color(0xFF003040)),
                shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()
            ) {
                Text("OPEN COLLIDER", fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, accentColor: Color,
                     modifier: Modifier = Modifier, onClick: (() -> Unit)?) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CollideColors.surface)
            .border(1.dp, CollideColors.muted.copy(0.3f), RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = accentColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(label, color = CollideColors.muted, fontSize = 9.sp, letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LabActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector,
                         label: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CollideColors.surface)
            .border(1.dp, CollideColors.muted.copy(0.25f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = CollideColors.accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(label, color = CollideColors.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = CollideColors.muted, fontSize = 12.sp)
        }
    }
}
