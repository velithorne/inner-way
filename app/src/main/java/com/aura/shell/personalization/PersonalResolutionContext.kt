package com.aura.shell.personalization

/**
 * Stores passed into [com.aura.shell.command.AppResolutionEngine] for aliases and learned weights.
 * Lookup keys for learning are normalized per-resolve from the command target string.
 */
data class PersonalResolutionContext(
    val aliasStore: PersonalAliasStore,
    val learningStore: PreferenceLearningStore,
)
