package com.aura.shell.voice

/**
 * High-level speech UI for the command layer (Compose).
 */
sealed class VoiceSurfaceState {
    data object Idle : VoiceSurfaceState()
    data object Listening : VoiceSurfaceState()
    data object Processing : VoiceSurfaceState()
    data class Error(val userMessage: String) : VoiceSurfaceState()
    data object PermissionNeeded : VoiceSurfaceState()
}
