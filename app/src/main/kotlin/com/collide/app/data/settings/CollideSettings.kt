package com.collide.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.collide.app.domain.model.BaselineStrategy
import com.collide.app.domain.model.RunMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "collide_settings")

object CollideSettingsKeys {
    val DEFAULT_BASELINE = stringPreferencesKey("default_baseline")
    val DEFAULT_RUN_MODE = stringPreferencesKey("default_run_mode")
    val DEFAULT_MAX_CANDIDATES = intPreferencesKey("default_max_candidates")
    val DEFAULT_MAX_CHAIN_LENGTH = intPreferencesKey("default_max_chain_length")
    val SAVE_NEAR_MISS = booleanPreferencesKey("save_near_miss")
    val MAX_FILE_SIZE_BYTES = longPreferencesKey("max_file_size_bytes")
}

data class CollideSettings(
    val defaultBaseline: BaselineStrategy = BaselineStrategy.RAW_DEFLATE,
    val defaultRunMode: RunMode = RunMode.BALANCED,
    val defaultMaxCandidates: Int = RunMode.BALANCED.maxCandidates,
    val defaultMaxChainLength: Int = RunMode.BALANCED.maxChainLength,
    val saveNearMiss: Boolean = false,
    val maxFileSizeBytes: Long = 2 * 1024 * 1024L // 2 MB default
)

class CollideSettingsStore(private val context: Context) {

    val settings: Flow<CollideSettings> = context.dataStore.data.map { prefs ->
        CollideSettings(
            defaultBaseline = prefs[CollideSettingsKeys.DEFAULT_BASELINE]
                ?.let { BaselineStrategy.valueOf(it) } ?: BaselineStrategy.RAW_DEFLATE,
            defaultRunMode = prefs[CollideSettingsKeys.DEFAULT_RUN_MODE]
                ?.let { RunMode.valueOf(it) } ?: RunMode.BALANCED,
            defaultMaxCandidates = prefs[CollideSettingsKeys.DEFAULT_MAX_CANDIDATES]
                ?: RunMode.BALANCED.maxCandidates,
            defaultMaxChainLength = prefs[CollideSettingsKeys.DEFAULT_MAX_CHAIN_LENGTH]
                ?: RunMode.BALANCED.maxChainLength,
            saveNearMiss = prefs[CollideSettingsKeys.SAVE_NEAR_MISS] ?: false,
            maxFileSizeBytes = prefs[CollideSettingsKeys.MAX_FILE_SIZE_BYTES] ?: (2 * 1024 * 1024L)
        )
    }

    suspend fun updateBaseline(strategy: BaselineStrategy) {
        context.dataStore.edit { it[CollideSettingsKeys.DEFAULT_BASELINE] = strategy.name }
    }

    suspend fun updateRunMode(mode: RunMode) {
        context.dataStore.edit { it[CollideSettingsKeys.DEFAULT_RUN_MODE] = mode.name }
    }

    suspend fun updateMaxCandidates(max: Int) {
        context.dataStore.edit { it[CollideSettingsKeys.DEFAULT_MAX_CANDIDATES] = max }
    }

    suspend fun updateMaxChainLength(max: Int) {
        context.dataStore.edit { it[CollideSettingsKeys.DEFAULT_MAX_CHAIN_LENGTH] = max }
    }

    suspend fun updateSaveNearMiss(save: Boolean) {
        context.dataStore.edit { it[CollideSettingsKeys.SAVE_NEAR_MISS] = save }
    }

    suspend fun updateMaxFileSize(bytes: Long) {
        context.dataStore.edit { it[CollideSettingsKeys.MAX_FILE_SIZE_BYTES] = bytes }
    }
}
