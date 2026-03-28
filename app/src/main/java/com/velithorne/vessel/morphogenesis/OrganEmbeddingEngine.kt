package com.velithorne.vessel.morphogenesis

import com.velithorne.vessel.physiology.OrganType

/**
 * How deeply organs sit inside accreted tissue (0 = surface, 1 = embedded).
 */
data class OrganEmbeddingFactors(
    val metabolicHeart: Float,
    val cortexCluster: Float,
    val neuralGel: Float,
    val archiveVault: Float,
    val signalLungs: Float,
    val vestibularMusc: Float,
    val thermalMembrane: Float,
) {
    fun forType(type: OrganType): Float = when (type) {
        OrganType.METABOLIC_HEART -> metabolicHeart
        OrganType.CORTEX_CLUSTER -> cortexCluster
        OrganType.NEURAL_GEL -> neuralGel
        OrganType.ARCHIVE_VAULT -> archiveVault
        OrganType.SIGNAL_LUNGS -> signalLungs
        OrganType.VESTIBULAR_MUSCULATURE -> vestibularMusc
        OrganType.THERMAL_MEMBRANE -> thermalMembrane
    }

    companion object {
        fun compute(
            acc: PressureAccumulator,
            field: GrowthPressureField,
            body: BodyMassFieldState,
            chamber: ChamberMassModel,
            tuning: GrowthTuning,
        ): OrganEmbeddingFactors {
            val base = (0.35f + body.totalOccupancy * 0.45f + tuning.organEmbedBase).coerceIn(0.2f, 0.95f)
            val neuralBoost = (field.cortical * 0.25f + acc.neural * 0.2f) * tuning.organEmbedNeuralMul
            val archiveBoost = (field.lowerArchive * 0.3f + acc.archive * 0.15f) * tuning.organEmbedArchiveMul
            val signalBoost = (field.lateralSignal * 0.22f + acc.signal * 0.12f) * tuning.organEmbedSignalMul
            return OrganEmbeddingFactors(
                metabolicHeart = (base + chamber.centralMetabolic * 0.18f).coerceIn(0.25f, 0.92f),
                cortexCluster = (base + neuralBoost + chamber.cranialCortex * 0.2f).coerceIn(0.28f, 0.95f),
                neuralGel = (base + neuralBoost * 0.9f + chamber.cranialCortex * 0.12f).coerceIn(0.3f, 0.95f),
                archiveVault = (base + archiveBoost + chamber.lowerArchiveBasin * 0.22f).coerceIn(0.3f, 0.95f),
                signalLungs = (base + signalBoost + chamber.lateralSignal * 0.18f).coerceIn(0.25f, 0.9f),
                vestibularMusc = (base + field.supportTendon * 0.15f + acc.motion * 0.1f).coerceIn(0.22f, 0.88f),
                thermalMembrane = (0.15f + chamber.perimeterShell * 0.35f + field.perimeterShell * 0.25f).coerceIn(0.12f, 0.75f),
            )
        }
    }
}
