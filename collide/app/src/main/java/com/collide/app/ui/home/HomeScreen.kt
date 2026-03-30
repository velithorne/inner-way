package com.collide.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.collide.app.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onColliderClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val eventCount by viewModel.eventCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CollideBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        // App title
        Text(
            text = "COLLIDE",
            style = MaterialTheme.typography.displayLarge.copy(
                color = CollideAccent,
                fontSize = 56.sp,
                letterSpacing = 8.sp
            )
        )

        Spacer(Modifier.height(8.dp))

        // Subtitle
        Text(
            text = "Load code. Collide structures. Detect meaningful events.",
            style = MaterialTheme.typography.bodySmall.copy(color = CollideDim),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Phase label
        Text(
            text = "PHASE 1 — CODE COLLIDER",
            style = MaterialTheme.typography.labelSmall.copy(
                color = CollideAccentAlt,
                letterSpacing = 3.sp
            )
        )

        Spacer(Modifier.height(40.dp))

        // Event count badge
        Row(
            modifier = Modifier
                .background(CollideCard, RoundedCornerShape(8.dp))
                .border(1.dp, CollideDim.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$eventCount",
                style = MaterialTheme.typography.headlineLarge.copy(color = CollideGreen),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "saved events",
                    style = MaterialTheme.typography.bodySmall.copy(color = CollideOnSurface)
                )
                Text(
                    "in archive",
                    style = MaterialTheme.typography.labelSmall.copy(color = CollideDim)
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        // Main Code Collider button
        ColliderEntryCard(
            title = "Code Collider",
            subtitle = "Mutate · Recombine · Detect",
            description = "Load one or two code snippets and run bounded structural collision passes.",
            icon = Icons.Default.Science,
            accentColor = CollideAccent,
            onClick = onColliderClick
        )

        Spacer(Modifier.height(16.dp))

        // Archive button
        ColliderEntryCard(
            title = "Event Archive",
            subtitle = "$eventCount discoveries",
            description = "Browse, inspect, and replay saved structural events.",
            icon = Icons.Default.Archive,
            accentColor = CollideAccentAlt,
            onClick = onArchiveClick
        )

        Spacer(Modifier.weight(1f))

        // Settings row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onSettingsClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Settings, "Settings", tint = CollideDim, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Settings", style = MaterialTheme.typography.labelMedium.copy(color = CollideDim))
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ColliderEntryCard(
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    accentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CollideCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, title, tint = accentColor, modifier = Modifier.size(26.dp))
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = CollideOnBackground,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(color = accentColor)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall.copy(color = CollideDim),
                    lineHeight = 16.sp
                )
            }

            Icon(
                Icons.Default.Science,
                null,
                tint = accentColor.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
