package com.collide.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
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
    onNavigateToSettings: () -> Unit
) {
    val eventCount by viewModel.eventCount.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CollideColors.background)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(56.dp))

        // Title
        Text(
            text = "COLLIDE",
            color = CollideColors.accent,
            fontSize = 52.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 8.sp,
            style = MaterialTheme.typography.displayMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "COMPRESSION COLLIDER LAB",
            color = CollideColors.muted,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
            style = MaterialTheme.typography.labelSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mission subtitle
        Text(
            text = "Generate, test, verify, and archive\nexact-reconstruction compression events.",
            color = CollideColors.onSurface,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Main CTA - Compression Collider
        ColliderMainCard(
            onClick = onNavigateToCollider,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "EVENTS ARCHIVED",
                value = eventCount.toString(),
                accentColor = CollideColors.winner,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToArchive
            )

            StatCard(
                label = "PHASE",
                value = "1",
                accentColor = CollideColors.accent,
                modifier = Modifier.weight(1f),
                onClick = null
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Archive shortcut
        LabActionRow(
            icon = Icons.Default.Storage,
            label = "Event Archive",
            subtitle = "Browse saved winning compressions",
            onClick = onNavigateToArchive
        )

        Spacer(modifier = Modifier.height(12.dp))

        LabActionRow(
            icon = Icons.Default.Settings,
            label = "Settings",
            subtitle = "Configure defaults and limits",
            onClick = onNavigateToSettings
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Phase indicator
        Text(
            text = "PHASE 1 · COMPRESSION COLLIDER ONLY",
            color = CollideColors.muted,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun ColliderMainCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF001A2E), Color(0xFF00101F))
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(CollideColors.accent.copy(alpha = 0.8f), CollideColors.accentAlt.copy(alpha = 0.6f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Default.Science,
                contentDescription = null,
                tint = CollideColors.accent,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "COMPRESSION COLLIDER",
                color = CollideColors.accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Load a file · Generate recipe candidates · Verify · Discover wins",
                color = CollideColors.onSurface,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CollideColors.accent,
                    contentColor = Color(0xFF003040)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "OPEN COLLIDER",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CollideColors.surface)
            .border(1.dp, CollideColors.muted.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                color = accentColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = CollideColors.muted,
                fontSize = 9.sp,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun LabActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CollideColors.surface)
            .border(1.dp, CollideColors.muted.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CollideColors.accent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = label,
                color = CollideColors.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = CollideColors.muted,
                fontSize = 12.sp
            )
        }
    }
}
