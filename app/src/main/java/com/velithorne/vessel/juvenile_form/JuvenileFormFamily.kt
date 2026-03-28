package com.velithorne.vessel.juvenile_form

import com.velithorne.vessel.branching.LineageBranch

/**
 * Branch-family-specific juvenile body biases (species-coherent).
 */
object JuvenileFormFamily {

    fun basePlan(lead: LineageBranch): JuvenileBodyPlan {
        val z = 0.12f
        return when (lead) {
            LineageBranch.THERMAL_SHELL -> JuvenileBodyPlan(
                crownMass = 0.22f, coreMass = 0.28f, lateralMass = 0.18f, reserveMass = 0.2f,
                shellMass = 0.92f, supportMass = 0.35f, archiveMass = 0.25f,
                dominantRegion = JuvenileRegion.SHELL_REGION,
            )
            LineageBranch.SIGNAL_FROND -> JuvenileBodyPlan(
                crownMass = 0.28f, coreMass = 0.3f, lateralMass = 0.9f, reserveMass = 0.22f,
                shellMass = 0.45f, supportMass = 0.32f, archiveMass = 0.2f,
                dominantRegion = JuvenileRegion.LATERAL_REGION_LEFT,
            )
            LineageBranch.CROWN_NEURAL -> JuvenileBodyPlan(
                crownMass = 0.95f, coreMass = 0.42f, lateralMass = 0.35f, reserveMass = 0.18f,
                shellMass = 0.48f, supportMass = 0.28f, archiveMass = 0.22f,
                dominantRegion = JuvenileRegion.CROWN_REGION,
            )
            LineageBranch.RESERVE_BASIN -> JuvenileBodyPlan(
                crownMass = 0.2f, coreMass = 0.35f, lateralMass = 0.25f, reserveMass = 0.92f,
                shellMass = 0.5f, supportMass = 0.45f, archiveMass = 0.28f,
                dominantRegion = JuvenileRegion.RESERVE_REGION,
            )
            LineageBranch.ARCHIVE_CORE -> JuvenileBodyPlan(
                crownMass = 0.25f, coreMass = 0.55f, lateralMass = 0.22f, reserveMass = 0.35f,
                shellMass = 0.55f, supportMass = 0.38f, archiveMass = 0.88f,
                dominantRegion = JuvenileRegion.ARCHIVE_REGION,
            )
            LineageBranch.MOTION_BRACED -> JuvenileBodyPlan(
                crownMass = 0.3f, coreMass = 0.38f, lateralMass = 0.4f, reserveMass = 0.3f,
                shellMass = 0.52f, supportMass = 0.9f, archiveMass = 0.2f,
                dominantRegion = JuvenileRegion.SUPPORT_REGION,
            )
            LineageBranch.BALANCED -> JuvenileBodyPlan(
                crownMass = 0.45f, coreMass = 0.45f, lateralMass = 0.45f, reserveMass = 0.45f,
                shellMass = 0.5f, supportMass = 0.45f, archiveMass = 0.4f,
                dominantRegion = JuvenileRegion.CORE_REGION,
            )
        }.let { p ->
            p.copy(
                crownMass = (p.crownMass + z).coerceIn(0.15f, 1f),
                coreMass = (p.coreMass + z).coerceIn(0.15f, 1f),
                lateralMass = (p.lateralMass + z).coerceIn(0.15f, 1f),
                reserveMass = (p.reserveMass + z).coerceIn(0.15f, 1f),
                shellMass = (p.shellMass + z).coerceIn(0.15f, 1f),
                supportMass = (p.supportMass + z).coerceIn(0.15f, 1f),
                archiveMass = (p.archiveMass + z).coerceIn(0.15f, 1f),
            )
        }
    }
}
