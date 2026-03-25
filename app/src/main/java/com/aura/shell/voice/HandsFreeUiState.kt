package com.aura.shell.voice

/**
 * Foreground passive listening (Phase 3.1). Not background always-on.
 */
sealed class HandsFreeUiState {
    data object Disabled : HandsFreeUiState()
    data object Armed : HandsFreeUiState()
    data object ListeningForWake : HandsFreeUiState()
    data object WakeDetected : HandsFreeUiState()
    data object ListeningForCommand : HandsFreeUiState()
    data object Processing : HandsFreeUiState()
    data class Success(val message: String) : HandsFreeUiState()
    data object Timeout : HandsFreeUiState()
    data class Error(val message: String) : HandsFreeUiState()
    data object PermissionNeeded : HandsFreeUiState()
}
