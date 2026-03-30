package com.collide.app.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.collide.app.domain.model.ColliderConfig
import com.collide.app.domain.model.DetectorSensitivity
import com.collide.app.domain.model.InputSample
import com.collide.app.domain.model.RunMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "collide_settings")

class AppSettings(private val context: Context) {

    companion object {
        val KEY_RUN_MODE = stringPreferencesKey("run_mode")
        val KEY_MAX_CANDIDATES = intPreferencesKey("max_candidates")
        val KEY_SEARCH_DEPTH = intPreferencesKey("search_depth")
        val KEY_DETECTOR_SENSITIVITY = stringPreferencesKey("detector_sensitivity")
        val KEY_SAVE_NEAR_MISSES = booleanPreferencesKey("save_near_misses")
        val KEY_MAX_INPUT_SIZE = intPreferencesKey("max_input_size")
        val KEY_TWO_INPUT_DEFAULT = booleanPreferencesKey("two_input_default")
    }

    val configFlow: Flow<ColliderConfig> = context.dataStore.data.map { prefs ->
        val runMode = try {
            RunMode.valueOf(prefs[KEY_RUN_MODE] ?: RunMode.BALANCED.name)
        } catch (e: Exception) { RunMode.BALANCED }

        ColliderConfig(
            runMode = runMode,
            maxCandidates = prefs[KEY_MAX_CANDIDATES] ?: runMode.maxCandidates,
            searchDepth = prefs[KEY_SEARCH_DEPTH] ?: runMode.searchDepth,
            detectorSensitivity = try {
                DetectorSensitivity.valueOf(prefs[KEY_DETECTOR_SENSITIVITY] ?: DetectorSensitivity.MEDIUM.name)
            } catch (e: Exception) { DetectorSensitivity.MEDIUM },
            saveNearMisses = prefs[KEY_SAVE_NEAR_MISSES] ?: false,
            maxInputSizeBytes = prefs[KEY_MAX_INPUT_SIZE] ?: InputSample.MAX_INPUT_SIZE_BYTES,
            twoInputMode = prefs[KEY_TWO_INPUT_DEFAULT] ?: false
        )
    }

    suspend fun setRunMode(mode: RunMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_RUN_MODE] = mode.name
            // Reset dependent settings when mode changes
            prefs[KEY_MAX_CANDIDATES] = mode.maxCandidates
            prefs[KEY_SEARCH_DEPTH] = mode.searchDepth
        }
    }

    suspend fun setMaxCandidates(count: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_MAX_CANDIDATES] = count }
    }

    suspend fun setSearchDepth(depth: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_SEARCH_DEPTH] = depth }
    }

    suspend fun setDetectorSensitivity(sensitivity: DetectorSensitivity) {
        context.dataStore.edit { prefs -> prefs[KEY_DETECTOR_SENSITIVITY] = sensitivity.name }
    }

    suspend fun setSaveNearMisses(save: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_SAVE_NEAR_MISSES] = save }
    }

    suspend fun setMaxInputSize(size: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_MAX_INPUT_SIZE] = size }
    }

    suspend fun setTwoInputDefault(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_TWO_INPUT_DEFAULT] = enabled }
    }
}
