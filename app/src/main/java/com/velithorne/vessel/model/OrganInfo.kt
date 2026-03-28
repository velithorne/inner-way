package com.velithorne.vessel.model

import com.velithorne.vessel.physiology.OrganType

/**
 * Static anatomy copy for inspection UI. Evolution may fork strings per species later.
 */
data class OrganInfo(
    val type: OrganType,
    val displayName: String,
    val description: String,
    val subsystemMapping: String,
) {
    companion object {
        fun forType(type: OrganType): OrganInfo = when (type) {
            OrganType.METABOLIC_HEART -> OrganInfo(
                type = type,
                displayName = "Metabolic Heart",
                description = "Pulsatile core that transduces energy reserve into organism-wide metabolic tone.",
                subsystemMapping = "Battery reserve, charging metabolism, vitality coupling",
            )
            OrganType.CORTEX_CLUSTER -> OrganInfo(
                type = type,
                displayName = "Cortex Cluster",
                description = "Distributed sensory-integration mesh; spikes with attention and interactive load.",
                subsystemMapping = "Screen interaction, neural activity proxy, cognitive stress",
            )
            OrganType.NEURAL_GEL -> OrganInfo(
                type = type,
                displayName = "Neural Gel",
                description = "Amorphous buffer medium surrounding the cortex; plasticity feels like viscosity.",
                subsystemMapping = "Memory pressure, synaptic reserve, low-memory risk",
            )
            OrganType.ARCHIVE_VAULT -> OrganInfo(
                type = type,
                displayName = "Archive Vault",
                description = "Dense stratified archive mass; structural storage is literal ballast.",
                subsystemMapping = "Storage utilization, structural load, reserve compaction",
            )
            OrganType.SIGNAL_LUNGS -> OrganInfo(
                type = type,
                displayName = "Signal Lungs",
                description = "Bilateral exchange pleura—inhale connectivity, exhale back-pressure.",
                subsystemMapping = "Network reach, transport type, metered constraint",
            )
            OrganType.VESTIBULAR_MUSCULATURE -> OrganInfo(
                type = type,
                displayName = "Vestibular Musculature",
                description = "Stabilizer lattice resisting inertial shearing—your grip, encoded.",
                subsystemMapping = "Motion intensity, orientation delta, postural sway",
            )
            OrganType.THERMAL_MEMBRANE -> OrganInfo(
                type = type,
                displayName = "Thermal Membrane",
                description = "Whole-surface entropic veil; leaks heat stress into visible shimmer.",
                subsystemMapping = "Pack temperature, fever projection, inflammatory coupling",
            )
        }
    }
}
