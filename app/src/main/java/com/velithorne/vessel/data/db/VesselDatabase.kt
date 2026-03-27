package com.velithorne.vessel.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.velithorne.vessel.data.db.dao.AdaptationEventDao
import com.velithorne.vessel.data.db.dao.AmbientEcologyMetaDao
import com.velithorne.vessel.data.db.dao.AmbientEventDao
import com.velithorne.vessel.data.db.dao.EcologySnapshotDao
import com.velithorne.vessel.data.db.dao.GrowthEventDao
import com.velithorne.vessel.data.db.dao.ReturnSummaryDao
import com.velithorne.vessel.data.db.dao.SeedPodStateDao
import com.velithorne.vessel.data.db.dao.SpecimenDao
import com.velithorne.vessel.data.db.entity.AdaptationEventEntity
import com.velithorne.vessel.data.db.entity.AmbientEcologyMetaEntity
import com.velithorne.vessel.data.db.entity.AmbientEventEntity
import com.velithorne.vessel.data.db.entity.EcologySnapshotEntity
import com.velithorne.vessel.data.db.entity.GrowthEventEntity
import com.velithorne.vessel.data.db.entity.GrowthStageEventEntity
import com.velithorne.vessel.data.db.entity.ReturnSummaryEntity
import com.velithorne.vessel.data.db.entity.SeedPodStateEntity
import com.velithorne.vessel.data.db.entity.SpecimenEntity

@Database(
    entities = [
        SpecimenEntity::class,
        SeedPodStateEntity::class,
        GrowthStageEventEntity::class,
        GrowthEventEntity::class,
        AdaptationEventEntity::class,
        ReturnSummaryEntity::class,
        EcologySnapshotEntity::class,
        AmbientEventEntity::class,
        AmbientEcologyMetaEntity::class,
    ],
    version = 10,
    exportSchema = false,
)
abstract class VesselDatabase : RoomDatabase() {
    abstract fun specimenDao(): SpecimenDao
    abstract fun seedPodStateDao(): SeedPodStateDao
    abstract fun growthEventDao(): GrowthEventDao
    abstract fun adaptationDao(): AdaptationEventDao
    abstract fun returnSummaryDao(): ReturnSummaryDao
    abstract fun ecologySnapshotDao(): EcologySnapshotDao
    abstract fun ambientEventDao(): AmbientEventDao
    abstract fun ambientEcologyMetaDao(): AmbientEcologyMetaDao

    companion object {
        fun create(context: Context): VesselDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                VesselDatabase::class.java,
                "velithorne_vessel.db",
            )
                .fallbackToDestructiveMigration()
                .build()
    }
}
