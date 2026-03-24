package com.aura.shell.ui.home

import androidx.compose.ui.graphics.Color
import com.aura.shell.ui.theme.AuraAccent
import com.aura.shell.ui.theme.AuraAccentDim
import com.aura.shell.ui.theme.AuraOutline
import com.aura.shell.ui.theme.AuraTextMuted

data class PinnedCardUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val accent: Color,
)

fun defaultPinnedCards(): List<PinnedCardUi> = listOf(
    PinnedCardUi("continue", "Continue", "Resume last focus", AuraAccent),
    PinnedCardUi("workbench", "Workbench", "Tools & drafts", AuraAccentDim),
    PinnedCardUi("daily", "Daily Flow", "Rhythm & routine", AuraOutline),
    PinnedCardUi("quick", "Quick Actions", "Shortcuts", AuraTextMuted),
)
