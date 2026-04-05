package com.velithorne.innerway.boot

import android.content.Context
import androidx.core.content.edit
import com.velithorne.innerway.genome.ActiveGenome
import com.velithorne.innerway.genome.BaseGenome
import com.velithorne.innerway.identity.VelithorneIdentity
import com.velithorne.innerway.memory.MemoryEntity
import com.velithorne.innerway.memory.MemoryKind
import com.velithorne.innerway.memory.MemoryRepository
import com.velithorne.innerway.mind.GrowthStage
import com.velithorne.innerway.mind.InternalState

/**
 * Ensures persistent identity and a single boot memory on cold install.
 */
object SpeciesInitializer {

    private const val PREFS = "velithorne_identity"
    private const val KEY_INITIALIZED = "species_initialized_v1"

    suspend fun ensureSpeciesSeed(context: Context, repository: MemoryRepository) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_INITIALIZED, false)) return

        VelithorneIdentity.persistDefault(context)
        ActiveGenome.persistDefault(context)

        repository.insert(
            MemoryEntity(
                timestampMillis = System.currentTimeMillis(),
                bodyConditionsSummary = "boot: substrate online; ${BaseGenome.SPECIES_NAME} lawSet=${BaseGenome.LAW_VERSION}",
                interpretedState = InternalState.CALM.name,
                triggeredBehavior = "substrate_listen",
                importantEvents = "first_awakening",
                growthTransition = GrowthStage.SEED.name,
                stressIncident = false,
                careIncident = false,
                memoryKind = MemoryKind.EVOLUTION_UNLOCK,
            )
        )

        prefs.edit { putBoolean(KEY_INITIALIZED, true) }
    }
}
