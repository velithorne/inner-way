package com.aura.shell.model

import android.graphics.drawable.Drawable

/**
 * Recently launched from Aura Shell (MRU). Not the system recents list.
 */
data class RecentAppEntry(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val lastLaunchTimeMillis: Long,
)
