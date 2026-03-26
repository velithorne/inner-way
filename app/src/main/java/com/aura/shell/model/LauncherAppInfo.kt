package com.aura.shell.model

import android.graphics.drawable.Drawable

/**
 * Launchable app row (installed apps list).
 */
data class LauncherAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
)
