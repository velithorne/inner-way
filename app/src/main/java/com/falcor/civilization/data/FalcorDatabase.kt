package com.falcor.civilization.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.falcor.civilization.data.dao.*
import com.falcor.civilization.data.entities.*

@Database(
    entities = [
        ProjectEntity::class,
        HypothesisEntity::class,
        PreregPlanEntity::class,
        ExperimentConfigEntity::class,
        RunEntity::class,
        RunArtifactEntity::class,
        ObservationEntity::class,
        AnalysisResultEntity::class,
        FindingEntity::class,
        LedgerEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class FalcorDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun hypothesisDao(): HypothesisDao
    abstract fun preregPlanDao(): PreregPlanDao
    abstract fun experimentConfigDao(): ExperimentConfigDao
    abstract fun runDao(): RunDao
    abstract fun runArtifactDao(): RunArtifactDao
    abstract fun observationDao(): ObservationDao
    abstract fun analysisResultDao(): AnalysisResultDao
    abstract fun findingDao(): FindingDao
    abstract fun ledgerEntryDao(): LedgerEntryDao
}
