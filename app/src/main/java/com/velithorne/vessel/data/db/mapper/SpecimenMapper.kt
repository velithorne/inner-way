package com.velithorne.vessel.data.db.mapper

import com.velithorne.vessel.data.db.entity.SpecimenEntity
import com.velithorne.vessel.lineage.SpecimenIdentity

object SpecimenMapper {
    fun toDomain(e: SpecimenEntity): SpecimenIdentity =
        SpecimenIdentity(
            specimenId = e.specimenId,
            displayLabel = e.displayLabel,
            seedType = e.seedType,
            creationTimestampMillis = e.creationTimestampMillis,
            lastUpdateTimestampMillis = e.lastUpdateTimestampMillis,
            buildVersionCreatedOn = e.buildVersionCreatedOn,
            lineageGeneration = e.lineageGeneration,
            active = e.active,
        )

    fun toEntity(d: SpecimenIdentity): SpecimenEntity =
        SpecimenEntity(
            specimenId = d.specimenId,
            displayLabel = d.displayLabel,
            seedType = d.seedType,
            creationTimestampMillis = d.creationTimestampMillis,
            lastUpdateTimestampMillis = d.lastUpdateTimestampMillis,
            buildVersionCreatedOn = d.buildVersionCreatedOn,
            lineageGeneration = d.lineageGeneration,
            active = d.active,
        )
}
