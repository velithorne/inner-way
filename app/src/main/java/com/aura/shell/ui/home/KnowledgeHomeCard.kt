package com.aura.shell.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aura.shell.knowledge.KnowledgeListItem
import com.aura.shell.ui.theme.AuraSurfaceElevated

@Composable
fun KnowledgeHomeCard(
    preview: KnowledgeListItem?,
    clusterPreview: List<KnowledgeListItem>,
    trendingTag: String?,
    onOpenKnowledge: () -> Unit,
    onOpenCluster: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                AuraSurfaceElevated,
                RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onOpenKnowledge)
            .padding(16.dp),
    ) {
        Text(
            text = "Knowledge",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (preview == null && clusterPreview.isEmpty()) {
            Text(
                text = "Notes and imports stay on this device. Tap to open.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        } else {
            preview?.let { p ->
                Text(
                    text = "Latest · ${p.title}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (clusterPreview.size > 1) {
                Text(
                    text = clusterPreview.drop(1).joinToString(" · ") { it.title },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = "Tap for full list · hub icon in Knowledge opens this cluster",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable(onClick = onOpenCluster),
                )
            } else {
                preview?.let { p ->
                    Text(
                        text = p.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            trendingTag?.let { t ->
                Text(
                    text = "Recurring tag: $t",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
