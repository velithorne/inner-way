package com.velithorne.innerway.render

import com.velithorne.innerway.mind.BodyExpressionModel
import com.velithorne.innerway.mind.GrowthStage
import com.velithorne.innerway.mind.InternalState
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Incremental substrate growth with **visible** time-based construction (edges extend, nodes form, plates crystallize).
 */
class GrowthEngine(
    private val random: Random = Random.Default,
) {

    private val nodeById = LinkedHashMap<String, GrowthNode>()
    private val edges = mutableListOf<GrowthEdge>()
    private val plates = mutableListOf<GrowthPlate>()
    private var nextId = 0
    private var seeded = false
    private var plateCooldown = 0f

    fun snapshot(): GrowthState = GrowthState(
        nodes = nodeById.values.toList(),
        edges = edges.toList(),
        plates = plates.toList(),
    )

    fun debugStats(stage: GrowthStage, state: InternalState, expression: BodyExpressionModel, dt: Float): GrowthDebugStats {
        val caps = stageCaps(stage)
        val gm = growthMultiplier(state, expression)
        val tips = nodeById.values.count { it.type == NodeType.ACTIVE_TIP && it.active }
        val nodes = nodeById.values.toList()
        val avgProg = if (nodes.isEmpty()) 0f else nodes.map { it.growthProgress }.average().toFloat()
        val formingNodes = nodes.count { it.growthPhase != GrowthPhase.MATURE }
        val matureNodes = nodes.count { it.growthPhase == GrowthPhase.MATURE }
        val growingEdges = edges.count { it.growthProgress < 0.999f }
        val matureRatio = if (nodes.isEmpty()) 0f else matureNodes.toFloat() / nodes.size.toFloat()
        return GrowthDebugStats(
            nodeCount = nodeById.size,
            edgeCount = edges.size,
            plateCount = plates.size,
            activeTipCount = tips,
            growthRate = gm,
            branchExtensionRate = caps.baseStep * gm,
            maxNodesCap = caps.maxNodes,
            avgGrowthProgress = avgProg,
            growingEdgesCount = growingEdges,
            formingNodesCount = formingNodes,
            matureVsGrowingRatio = matureRatio,
        )
    }

    fun update(
        stage: GrowthStage,
        state: InternalState,
        expression: BodyExpressionModel,
        dt: Float,
        widthPx: Float,
        heightPx: Float,
    ) {
        val d = dt.coerceIn(0f, 0.5f)
        val now = System.currentTimeMillis()
        val aspect = (widthPx / heightPx.coerceAtLeast(1f)).coerceIn(0.45f, 2.2f)

        if (!seeded) {
            seedCore(now)
            seeded = true
        }

        val caps = stageCaps(stage)
        val gMul = growthMultiplier(state, expression)
        val growthSpeed = baseGrowthSpeed(state, expression, caps) * gMul

        // Age (simulation)
        nodeById.keys.toList().forEach { id ->
            nodeById[id]?.let { n -> nodeById[id] = n.copy(age = n.age + d) }
        }
        for (i in edges.indices) {
            val e = edges[i]
            edges[i] = e.copy(age = e.age + d, conductivity = min(1f, e.conductivity + d * 0.012f))
        }
        for (i in plates.indices) {
            val p = plates[i]
            plates[i] = p.copy(age = p.age + d)
        }

        // Lifecycle: progress advances every frame
        advanceEdgeGrowth(d, growthSpeed, now)
        advanceNodeFormation(d, growthSpeed, now)
        advancePlateCrystallization(d, growthSpeed, now)

        val stepScale = d * gMul
        applyStress(state, d)
        applyRecovering(state, d)
        plateCooldown = (plateCooldown - d).coerceAtLeast(0f)

        if (stepScale > 0.025f || state == InternalState.RECOVERING) {
            extendTips(stage, caps, stepScale, aspect, expression, state, d, now, growthSpeed)
            maybeBranch(stage, caps, stepScale, now)
            maybePlate(stage, caps, state, now)
        }

        enforceCaps(caps)
    }

    private fun baseGrowthSpeed(state: InternalState, ex: BodyExpressionModel, caps: StageCaps): Float {
        var s = caps.baseStep * 2.8f
        when (state) {
            InternalState.CALM -> s *= 1f
            InternalState.STRESSED -> s *= 0.08f
            InternalState.RECOVERING -> s *= 0.55f
            InternalState.CURIOUS, InternalState.ALERT -> s *= 1.35f
            InternalState.RESTING, InternalState.DORMANT -> s *= 0.12f
            InternalState.HUNGRY -> s *= 0.65f
            InternalState.DEFENSIVE -> s *= 0.4f
            else -> s *= 0.85f
        }
        s *= 0.75f + ex.openness * 0.35f
        return s.coerceIn(0.02f, 0.45f)
    }

    private fun advanceEdgeGrowth(d: Float, speed: Float, @Suppress("UNUSED_PARAMETER") now: Long) {
        for (i in edges.indices) {
            val e = edges[i]
            if (e.growthProgress >= 1f) continue
            val dp = d * speed * 1.15f
            val np = (e.growthProgress + dp).coerceIn(0f, 1f)
            val phase = growthPhaseFromProgress(np)
            edges[i] = e.copy(
                growthProgress = np,
                growthPhase = phase,
            )
        }
    }

    private fun advanceNodeFormation(d: Float, speed: Float, @Suppress("UNUSED_PARAMETER") now: Long) {
        nodeById.keys.toList().forEach { id ->
            val n = nodeById[id] ?: return@forEach
            if (n.growthProgress >= 1f && n.growthPhase == GrowthPhase.MATURE) return@forEach
            val dp = d * speed * 1.25f
            val np = (n.growthProgress + dp).coerceIn(0f, 1f)
            nodeById[id] = n.copy(
                growthProgress = np,
                growthPhase = growthPhaseFromProgress(np),
            )
        }
    }

    private fun advancePlateCrystallization(d: Float, speed: Float, now: Long) {
        for (i in plates.indices) {
            val p = plates[i]
            if (p.growthProgress >= 1f) continue
            val dp = d * speed * 0.85f
            val np = (p.growthProgress + dp).coerceIn(0f, 1f)
            plates[i] = p.copy(
                growthProgress = np,
                growthPhase = growthPhaseFromProgress(np),
            )
        }
    }

    private fun newId(): String = "n${nextId++}"

    private fun addNode(n: GrowthNode) {
        nodeById[n.id] = n
    }

    private fun seedCore(now: Long) {
        val cx = 0.5f
        val cy = 0.52f
        val t0 = now - 60_000L
        val seed = GrowthNode(
            newId(), cx, cy, 1f, 0f, NodeType.SEED_CORE, false, 0f,
            GrowthPhase.MATURE, 1f, t0,
        )
        addNode(seed)
        val r = 0.038f
        val r1 = GrowthNode(
            newId(), cx + r * 1.3f, cy + r * 0.4f, 0.55f, 0f, NodeType.ROOT, false, 0.7f,
            GrowthPhase.MATURE, 1f, t0,
        )
        val r2 = GrowthNode(
            newId(), cx - r * 1.15f, cy - r * 0.55f, 0.52f, 0f, NodeType.ROOT, false, -2.05f,
            GrowthPhase.MATURE, 1f, t0,
        )
        addNode(r1)
        addNode(r2)
        edges.add(edge(seed.id, r1.id, 0.41f, t0))
        edges.add(edge(seed.id, r2.id, 0.39f, t0))

        val t1 = GrowthNode(
            newId(), cx + r * 2.5f, cy + r * 0.75f, 0.8f, 0f, NodeType.ACTIVE_TIP, true, 0.52f,
            GrowthPhase.MATURE, 1f, t0,
        )
        val t2 = GrowthNode(
            newId(), cx - r * 2.3f, cy - r * 1.05f, 0.78f, 0f, NodeType.ACTIVE_TIP, true, -2.0f,
            GrowthPhase.MATURE, 1f, t0,
        )
        addNode(t1)
        addNode(t2)
        edges.add(edge(r1.id, t1.id, 0.35f, t0))
        edges.add(edge(r2.id, t2.id, 0.33f, t0))
    }

    private fun edge(from: String, to: String, targetThickness: Float, createdAt: Long): GrowthEdge =
        GrowthEdge(
            fromId = from,
            toId = to,
            thickness = targetThickness,
            conductivity = 0.68f,
            age = 0f,
            growthPhase = GrowthPhase.MATURE,
            growthProgress = 1f,
            createdAt = createdAt,
        )

    private fun newEdge(from: String, to: String, targetThickness: Float, now: Long): GrowthEdge =
        GrowthEdge(
            fromId = from,
            toId = to,
            thickness = targetThickness,
            conductivity = 0.55f,
            age = 0f,
            growthPhase = GrowthPhase.GROWING,
            growthProgress = 0f,
            createdAt = now,
        )

    private data class StageCaps(
        val maxNodes: Int,
        val maxEdges: Int,
        val maxPlates: Int,
        val maxReach: Float,
        val baseStep: Float,
        val branchProb: Float,
        val plateProb: Float,
    )

    private fun stageCaps(s: GrowthStage): StageCaps = when (s) {
        GrowthStage.SEED -> StageCaps(32, 40, 3, 0.26f, 0.0105f, 0.07f, 0.055f)
        GrowthStage.INFANT -> StageCaps(72, 88, 12, 0.38f, 0.0135f, 0.12f, 0.13f)
        GrowthStage.CHILD -> StageCaps(130, 158, 24, 0.46f, 0.0165f, 0.15f, 0.17f)
        GrowthStage.ADOLESCENT -> StageCaps(220, 268, 38, 0.52f, 0.0185f, 0.17f, 0.19f)
        GrowthStage.MATURE -> StageCaps(360, 430, 52, 0.58f, 0.0205f, 0.19f, 0.21f)
    }

    private fun growthMultiplier(state: InternalState, ex: BodyExpressionModel): Float {
        var m = when (state) {
            InternalState.CALM -> 1f
            InternalState.STRESSED -> 0.14f
            InternalState.RECOVERING -> 0.58f + ex.brightness * 0.22f
            InternalState.CURIOUS, InternalState.ALERT -> 1.48f
            InternalState.RESTING, InternalState.DORMANT -> 0.1f
            InternalState.HUNGRY -> 0.6f
            InternalState.DEFENSIVE -> 0.36f
            else -> 0.8f
        }
        m *= 0.6f + ex.openness * 0.45f
        return m.coerceIn(0.03f, 1.9f)
    }

    private fun applyStress(state: InternalState, d: Float) {
        if (state != InternalState.STRESSED) return
        val seed = nodeById.values.find { it.type == NodeType.SEED_CORE } ?: return
        nodeById.values.filter { it.type == NodeType.ACTIVE_TIP }.forEach { tip ->
            nodeById[tip.id] = tip.copy(energy = (tip.energy - 0.35f * d).coerceIn(0.1f, 1f), active = false)
            val vx = seed.x - tip.x
            val vy = seed.y - tip.y
            val len = hypot(vx, vy).coerceAtLeast(1e-4f)
            val pull = 0.014f * d
            nodeById[tip.id] = nodeById[tip.id]!!.copy(
                x = (tip.x + vx / len * pull).coerceIn(0.04f, 0.96f),
                y = (tip.y + vy / len * pull).coerceIn(0.04f, 0.96f),
            )
        }
    }

    private fun applyRecovering(state: InternalState, d: Float) {
        if (state != InternalState.RECOVERING) return
        nodeById.values.filter { it.type == NodeType.ACTIVE_TIP }.forEach { tip ->
            nodeById[tip.id] = tip.copy(energy = min(1f, tip.energy + 0.3f * d), active = true)
        }
    }

    private fun extendTips(
        stage: GrowthStage,
        caps: StageCaps,
        stepScale: Float,
        aspect: Float,
        expression: BodyExpressionModel,
        state: InternalState,
        d: Float,
        now: Long,
        growthSpeed: Float,
    ) {
        val seed = nodeById.values.find { it.type == NodeType.SEED_CORE } ?: return
        val tips = nodeById.values.filter { it.type == NodeType.ACTIVE_TIP && it.active }.toList()

        for (tip in tips) {
            if (nodeById.size >= caps.maxNodes - 2) break

            var dir = tip.directionRad
            dir += (random.nextFloat() - 0.5f) * 0.12f * (if (state == InternalState.CURIOUS) 1.7f else 1f)
            dir += (expression.instability - 0.28f) * 0.1f

            val step = caps.baseStep * stepScale * 48f * (0.82f + expression.pulseIntensity * 0.28f)
            val nx = (tip.x + cos(dir) * step).coerceIn(0.03f, 0.97f)
            val ny = (tip.y + sin(dir) * step / aspect).coerceIn(0.03f, 0.97f)

            val reach = hypot(nx - seed.x, ny - seed.y)
            if (reach > caps.maxReach) {
                nodeById[tip.id] = tip.copy(directionRad = atan2(0.5f - tip.y, 0.5f - tip.x) + (random.nextFloat() - 0.5f))
                continue
            }

            val parentEdge = edges.find { it.toId == tip.id } ?: continue
            val parentId = parentEdge.fromId
            val thick = parentEdge.thickness

            val accreteChance = (0.08f + stage.ordinal * 0.04f) * stepScale * 25f * (0.4f + tip.age * 0.08f).coerceAtMost(1.2f)
            val shouldAccrete = tip.age > 0.45f && random.nextFloat() < accreteChance.coerceIn(0f, 0.55f)

            if (shouldAccrete && nodeById.size < caps.maxNodes - 3) {
                val junction = GrowthNode(
                    id = newId(),
                    x = tip.x,
                    y = tip.y,
                    energy = tip.energy * 0.9f,
                    age = 0f,
                    type = NodeType.JUNCTION,
                    active = false,
                    directionRad = dir,
                    growthPhase = GrowthPhase.FORMING,
                    growthProgress = 0f,
                    createdAt = now,
                )
                addNode(junction)
                edges.removeAll { it.toId == tip.id }
                edges.add(newEdge(parentId, junction.id, thick * 0.96f, now))

                val newTip = GrowthNode(
                    id = newId(),
                    x = nx,
                    y = ny,
                    energy = 0.82f,
                    age = 0f,
                    type = NodeType.ACTIVE_TIP,
                    active = true,
                    directionRad = dir,
                    growthPhase = GrowthPhase.FORMING,
                    growthProgress = 0f,
                    createdAt = now,
                )
                addNode(newTip)
                edges.add(newEdge(junction.id, newTip.id, thick * 0.9f, now))
                nodeById.remove(tip.id)
            } else {
                nodeById[tip.id] = tip.copy(x = nx, y = ny, directionRad = dir, age = tip.age + d * 0.5f)
                val ei = edges.indexOfFirst { it.toId == tip.id }
                if (ei >= 0) {
                    val e = edges[ei]
                    edges[ei] = e.copy(thickness = min(0.62f, thick * 1.002f))
                    // Nudge incomplete edge toward completion while tip extends
                    if (e.growthProgress < 1f) {
                        edges[ei] = edges[ei].copy(growthProgress = min(1f, edges[ei].growthProgress + d * growthSpeed * 2f))
                    }
                }
            }
        }
    }

    private fun maybeBranch(stage: GrowthStage, caps: StageCaps, stepScale: Float, now: Long) {
        if (nodeById.size >= caps.maxNodes - 4 || edges.size >= caps.maxEdges - 3) return
        if (random.nextFloat() > caps.branchProb * stepScale * 7f) return
        val juns = nodeById.values.filter { it.type == NodeType.JUNCTION && it.age > 0.8f }
        if (juns.isEmpty()) return
        val j = juns.random(random)
        val dir = j.directionRad + (random.nextFloat() - 0.5f) * 2.4f
        val span = caps.baseStep * 16f
        val nx = (j.x + cos(dir) * span).coerceIn(0.05f, 0.95f)
        val ny = (j.y + sin(dir) * span).coerceIn(0.05f, 0.95f)
        val tip = GrowthNode(
            newId(), nx, ny, 0.74f, 0f, NodeType.ACTIVE_TIP, true, dir,
            GrowthPhase.FORMING, 0f, now,
        )
        addNode(tip)
        edges.add(newEdge(j.id, tip.id, 0.29f, now))
    }

    private fun maybePlate(stage: GrowthStage, caps: StageCaps, state: InternalState, now: Long) {
        if (plates.size >= caps.maxPlates || plateCooldown > 0f) return
        if (state == InternalState.STRESSED && random.nextFloat() > 0.18f) return
        if (random.nextFloat() > caps.plateProb * 0.055f) return

        val pool = nodeById.values.filter { it.type != NodeType.SEED_CORE }.toList()
        if (pool.size < 4) return
        val a = pool.random(random)
        val b = pool.filter { it.id != a.id }.random(random)
        val c = pool.filter { it.id != a.id && it.id != b.id }.random(random)
        if (hypot(a.x - b.x, a.y - b.y) > 0.22f) return
        if (hypot(b.x - c.x, b.y - c.y) > 0.22f) return
        if (hypot(a.x - c.x, a.y - c.y) > 0.22f) return

        plates.add(
            GrowthPlate(
                anchorNodeIds = listOf(a.id, b.id, c.id),
                opacity = 0.09f + random.nextFloat() * 0.14f,
                age = 0f,
                growthPhase = GrowthPhase.FORMING,
                growthProgress = 0f,
                createdAt = now,
            ),
        )
        plateCooldown = 2.8f
    }

    private fun enforceCaps(caps: StageCaps) {
        while (nodeById.size > caps.maxNodes) {
            val tip = nodeById.values.filter { it.type == NodeType.ACTIVE_TIP && it.age > 5f }
                .minByOrNull { it.energy } ?: break
            edges.removeAll { it.fromId == tip.id || it.toId == tip.id }
            nodeById.remove(tip.id)
        }
    }
}

data class GrowthDebugStats(
    val nodeCount: Int,
    val activeTipCount: Int,
    val edgeCount: Int,
    val plateCount: Int,
    val growthRate: Float,
    val branchExtensionRate: Float,
    val maxNodesCap: Int,
    val avgGrowthProgress: Float,
    val growingEdgesCount: Int,
    val formingNodesCount: Int,
    val matureVsGrowingRatio: Float,
)
