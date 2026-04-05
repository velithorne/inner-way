package com.velithorne.innerway.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.velithorne.innerway.mind.BodyExpressionModel
import com.velithorne.innerway.mind.GrowthImprintModel
import com.velithorne.innerway.perception.EnvironmentalContext
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private val Substrate = Color(0xFF05070C)
private val TraceDim = Color(0xFF1A2838)
private val SiliconCool = Color(0xFF3DB8A8)
private val SiliconHot = Color(0xFFC97B62)

private const val NEW_HIGHLIGHT_MS = 2000L

/**
 * Renders growth with **time-based construction**: edges extend, nodes form, plates crystallize.
 */
fun DrawScope.drawGrowthField(
    growth: GrowthState,
    environment: EnvironmentalContext,
    pulsePhase: Float,
    baseTint: Color,
    expression: BodyExpressionModel,
    imprint: GrowthImprintModel = GrowthImprintModel(),
    nowMillis: Long = System.currentTimeMillis(),
) {
    val wPx = size.width
    val hPx = size.height
    fun px(nx: Float, ny: Float) = Offset(nx * wPx, ny * hPx)

    drawRect(brush = Brush.verticalGradient(listOf(Substrate, Color(0xFF0A1018))), topLeft = Offset.Zero, size = size)

    val nodeMap = growth.nodes.associateBy { it.id }
    val imprintContract = (expression.contraction * 0.35f + imprint.contractionMemory * 0.25f).coerceIn(0f, 0.65f)
    val stressMul = (1f - imprintContract).coerceIn(0.5f, 1f)
    val pulseSharp = expression.pulseIntensity * (1f + expression.instability * 0.5f)
    val brightness = (expression.brightness * (1f + imprint.calmReserve * 0.08f) *
        (1f + imprint.chargeTrust * 0.06f * if (environment.charging) 1f else 0f)).coerceIn(0.12f, 1f)

    fun highlightMix(createdAt: Long): Float {
        val age = (nowMillis - createdAt).coerceAtLeast(0L)
        return if (age < NEW_HIGHLIGHT_MS) {
            1f - age / NEW_HIGHLIGHT_MS.toFloat()
        } else {
            0f
        }
    }

    // Root bed — mature nodes only contribute faint bed
    growth.nodes.filter { it.type == NodeType.ROOT || it.type == NodeType.SEED_CORE }.forEach { n ->
        val vis = n.growthProgress.coerceIn(0f, 1f)
        val c = baseTint.copy(alpha = 0.06f * brightness * stressMul * vis)
        drawCircle(color = c, radius = (3f + n.age * 0.02f) * vis, center = px(n.x, n.y))
    }

    // Edges — draw only grown length; pulse travels along visible segment
    growth.edges.forEachIndexed { idx, e ->
        val a = nodeMap[e.fromId] ?: return@forEachIndexed
        val b = nodeMap[e.toId] ?: return@forEachIndexed
        val ax = (a.x + (randomAsym(idx, imprint) * imprint.asymmetryBias * 0.012f)).coerceIn(0f, 1f)
        val ay = (a.y + (randomAsym(idx + 3, imprint) * imprint.asymmetryBias * 0.012f)).coerceIn(0f, 1f)
        val bx = (b.x + (randomAsym(idx + 7, imprint) * imprint.asymmetryBias * 0.012f)).coerceIn(0f, 1f)
        val by = (b.y + (randomAsym(idx + 11, imprint) * imprint.asymmetryBias * 0.012f)).coerceIn(0f, 1f)
        val p0 = px(ax, ay)
        val p1 = px(bx, by)
        val t = e.growthProgress.coerceIn(0f, 1f)
        if (t <= 0.001f) return@forEachIndexed

        val end = Offset(
            p0.x + (p1.x - p0.x) * t,
            p0.y + (p1.y - p0.y) * t,
        )

        val targetW = (1.2f + e.thickness * 5f) * stressMul * (1f + imprint.stressLoad * 0.25f + imprint.contractionMemory * 0.15f)
        val lineW = targetW * t.coerceIn(0.15f, 1f)
        val dimAlpha = (0.12f + e.conductivity * 0.18f) * brightness * t * (0.92f + imprint.calmReserve * 0.12f)

        val hm = highlightMix(e.createdAt)
        val lineColor = lerp(TraceDim, SiliconHot, hm * 0.35f).copy(alpha = (dimAlpha + hm * 0.12f).coerceIn(0.04f, 0.55f))

        drawLine(
            color = lineColor,
            start = p0,
            end = end,
            strokeWidth = lineW.coerceAtLeast(0.8f),
        )

        val travel = (pulsePhase + idx * 0.07f + e.age * 0.01f) % 1f
        val along = travel * t
        val pxFlow = p0.x + (p1.x - p0.x) * along
        val pyFlow = p0.y + (p1.y - p0.y) * along
        val flowAlpha = (0.35f + environment.energyRatio * 0.4f) * brightness * (0.55f + pulseSharp * 0.45f) * t
        val heatMix = (expression.instability * 0.55f + environment.thermalRatio * 0.35f + hm * 0.25f).coerceIn(0f, 1f)
        val flowColor = lerp(SiliconCool, SiliconHot, heatMix)
        val pulseBoost = 1f + hm * 0.35f + (if (e.growthProgress < 1f) sin(pulsePhase * Math.PI.toFloat() * 4f) * 0.08f else 0f)
        drawCircle(
            color = flowColor.copy(alpha = (flowAlpha * pulseBoost).coerceIn(0.06f, 0.9f)),
            radius = (2.2f + e.thickness * 3f) * t.coerceIn(0.4f, 1f),
            center = Offset(pxFlow, pyFlow),
        )

        val travel2 = (pulsePhase * 0.65f + idx * 0.11f) % 1f
        val along2 = travel2 * t
        drawCircle(
            color = SiliconCool.copy(alpha = flowAlpha * 0.35f * t),
            radius = 1.4f,
            center = Offset(
                p0.x + (p1.x - p0.x) * along2,
                p0.y + (p1.y - p0.y) * along2,
            ),
        )
    }

    // Plates — crystallize (opacity + scale)
    growth.plates.forEach { plate ->
        val anchors = plate.anchorNodeIds.mapNotNull { nodeMap[it] }
        if (anchors.size < 3) return@forEach
        val prog = plate.growthProgress.coerceIn(0f, 1f)
        if (prog <= 0.001f) return@forEach
        val cx = (anchors[0].x + anchors[1].x + anchors[2].x) / 3f
        val cy = (anchors[0].y + anchors[1].y + anchors[2].y) / 3f
        val center = px(cx, cy)
        val scale = 0.8f + 0.2f * prog

        val o0 = Offset(
            center.x + (px(anchors[0].x, anchors[0].y).x - center.x) * scale,
            center.y + (px(anchors[0].x, anchors[0].y).y - center.y) * scale,
        )
        val o1 = Offset(
            center.x + (px(anchors[1].x, anchors[1].y).x - center.x) * scale,
            center.y + (px(anchors[1].x, anchors[1].y).y - center.y) * scale,
        )
        val o2 = Offset(
            center.x + (px(anchors[2].x, anchors[2].y).x - center.x) * scale,
            center.y + (px(anchors[2].x, anchors[2].y).y - center.y) * scale,
        )
        val p = Path().apply {
            moveTo(o0.x, o0.y)
            lineTo(o1.x, o1.y)
            lineTo(o2.x, o2.y)
            close()
        }
        val fillA = plate.opacity * 0.1f * brightness * prog * (0.85f + imprint.plateFormationBias * 0.25f)
        val strokeA = plate.opacity * 0.55f * brightness * prog * (0.9f + imprint.plateFormationBias * 0.2f)
        val hm = highlightMix(plate.createdAt)
        drawPath(
            path = p,
            color = lerp(SiliconCool, SiliconHot, hm * 0.4f).copy(alpha = fillA + hm * 0.06f),
        )
        drawPath(
            path = p,
            color = baseTint.copy(alpha = strokeA + hm * 0.15f),
            style = Stroke(width = 1f),
        )
    }

    // Nodes — radius and alpha follow formation progress
    growth.nodes.forEach { n ->
        val center = px(n.x, n.y)
        val form = n.growthProgress.coerceIn(0.001f, 1f)
        val hm = highlightMix(n.createdAt)
        val pulseForm = if (n.growthProgress < 1f) 1f + sin(pulsePhase * Math.PI.toFloat() * 3f) * 0.12f else 1f

        val baseRad = when (n.type) {
            NodeType.SEED_CORE -> 4.2f
            NodeType.ACTIVE_TIP -> 2.4f + n.energy * 1.8f
            NodeType.JUNCTION -> 2.2f
            NodeType.ROOT -> 1.8f
            NodeType.PLATE_ANCHOR -> 2f
        } * stressMul * form * pulseForm

        val baseAlpha = when (n.type) {
            NodeType.SEED_CORE -> 0.55f
            NodeType.ACTIVE_TIP -> (0.35f + n.energy * 0.4f) * if (n.active) 1f else 0.45f
            NodeType.JUNCTION -> 0.28f
            NodeType.ROOT -> 0.35f
            NodeType.PLATE_ANCHOR -> 0.32f
        } * brightness * form

        val col = when (n.type) {
            NodeType.SEED_CORE -> baseTint.copy(alpha = baseAlpha)
            NodeType.ACTIVE_TIP -> lerp(SiliconCool, SiliconHot, hm * 0.5f).copy(alpha = baseAlpha + hm * 0.15f)
            NodeType.JUNCTION -> baseTint.copy(alpha = baseAlpha)
            NodeType.ROOT -> TraceDim.copy(alpha = baseAlpha)
            NodeType.PLATE_ANCHOR -> SiliconCool.copy(alpha = baseAlpha)
        }
        drawCircle(color = col, radius = baseRad, center = center)
    }

    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0x88050508)),
            center = Offset(wPx * 0.5f, hPx * 0.52f),
            radius = max(wPx, hPx) * 0.85f,
        ),
        topLeft = Offset.Zero,
        size = size,
    )
}

/** Deterministic micro-asymmetry from imprint + index (no new Random in draw). */
private fun randomAsym(seed: Int, imprint: GrowthImprintModel): Float =
    kotlin.math.sin((seed * 12.9898 + imprint.asymmetryBias * 78.233).toDouble()).toFloat()
