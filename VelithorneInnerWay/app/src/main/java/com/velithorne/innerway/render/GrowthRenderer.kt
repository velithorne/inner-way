package com.velithorne.innerway.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.velithorne.innerway.mind.BodyExpressionModel
import com.velithorne.innerway.perception.EnvironmentalContext
import kotlin.math.hypot

private val Substrate = Color(0xFF05070C)
private val TraceDim = Color(0xFF1A2838)
private val SiliconCool = Color(0xFF3DB8A8)
private val SiliconHot = Color(0xFFC97B62)

/**
 * Renders the persistent growth graph: substrate, conductive branches, nodes, plates, pulse flow.
 * No dominant central orb — energy reads along edges.
 */
fun DrawScope.drawGrowthField(
    growth: GrowthState,
    environment: EnvironmentalContext,
    pulsePhase: Float,
    baseTint: Color,
    expression: BodyExpressionModel,
) {
    val wPx = size.width
    val hPx = size.height
    fun px(nx: Float, ny: Float) = Offset(nx * wPx, ny * hPx)

    // 1) Substrate
    drawRect(brush = Brush.verticalGradient(listOf(Substrate, Color(0xFF0A1018))), topLeft = Offset.Zero, size = size)

    val nodeMap = growth.nodes.associateBy { it.id }
    val stressMul = (1f - expression.contraction * 0.35f).coerceIn(0.55f, 1f)
    val pulseSharp = expression.pulseIntensity * (1f + expression.instability * 0.5f)
    val brightness = expression.brightness.coerceIn(0.12f, 1f)

    // 2) Root bed — very faint traces under seed
    growth.nodes.filter { it.type == NodeType.ROOT || it.type == NodeType.SEED_CORE }.forEach { n ->
        val c = baseTint.copy(alpha = 0.06f * brightness * stressMul)
        drawCircle(color = c, radius = 3f + n.age * 0.02f, center = px(n.x, n.y))
    }

    // 3–4) Conductive branches + pulse traveling along edges
    growth.edges.forEachIndexed { idx, e ->
        val a = nodeMap[e.fromId] ?: return@forEachIndexed
        val b = nodeMap[e.toId] ?: return@forEachIndexed
        val p0 = px(a.x, a.y)
        val p1 = px(b.x, b.y)
        val len = hypot(p1.x - p0.x, p1.y - p0.y).coerceAtLeast(1f)

        val baseW = (1.2f + e.thickness * 5f) * stressMul
        val dimAlpha = (0.12f + e.conductivity * 0.18f) * brightness

        drawLine(
            color = TraceDim.copy(alpha = dimAlpha),
            start = p0,
            end = p1,
            strokeWidth = baseW,
        )

        // Pulse packet position along edge (0..1)
        val travel = (pulsePhase + idx * 0.07f + e.age * 0.01f) % 1f
        val pxFlow = p0.x + (p1.x - p0.x) * travel
        val pyFlow = p0.y + (p1.y - p0.y) * travel
        val flowAlpha = (0.35f + environment.energyRatio * 0.4f) * brightness * (0.55f + pulseSharp * 0.45f)
        val heatMix = (expression.instability * 0.55f + environment.thermalRatio * 0.35f).coerceIn(0f, 1f)
        val flowColor = lerp(SiliconCool, SiliconHot, heatMix)
        drawCircle(
            color = flowColor.copy(alpha = flowAlpha.coerceIn(0.08f, 0.85f)),
            radius = 2.2f + e.thickness * 3f,
            center = Offset(pxFlow, pyFlow),
        )

        // Secondary echo (slower)
        val travel2 = (pulsePhase * 0.65f + idx * 0.11f) % 1f
        drawCircle(
            color = SiliconCool.copy(alpha = flowAlpha * 0.35f),
            radius = 1.4f,
            center = Offset(
                p0.x + (p1.x - p0.x) * travel2,
                p0.y + (p1.y - p0.y) * travel2,
            ),
        )
    }

    // 5) Crystalline plates (triangles)
    growth.plates.forEach { plate ->
        val anchors = plate.anchorNodeIds.mapNotNull { nodeMap[it] }
        if (anchors.size < 3) return@forEach
        val o0 = px(anchors[0].x, anchors[0].y)
        val o1 = px(anchors[1].x, anchors[1].y)
        val o2 = px(anchors[2].x, anchors[2].y)
        val p = Path().apply {
            moveTo(o0.x, o0.y)
            lineTo(o1.x, o1.y)
            lineTo(o2.x, o2.y)
            close()
        }
        drawPath(
            path = p,
            color = SiliconCool.copy(alpha = plate.opacity * 0.1f * brightness),
        )
        drawPath(
            path = p,
            color = baseTint.copy(alpha = plate.opacity * 0.55f * brightness),
            style = Stroke(width = 1f),
        )
    }

    // 6) Junction / tip / seed nodes (small — not orbs)
    growth.nodes.forEach { n ->
        val center = px(n.x, n.y)
        val col = when (n.type) {
            NodeType.SEED_CORE -> baseTint.copy(alpha = 0.55f * brightness)
            NodeType.ACTIVE_TIP -> SiliconCool.copy(alpha = (0.35f + n.energy * 0.4f) * brightness * if (n.active) 1f else 0.45f)
            NodeType.JUNCTION -> baseTint.copy(alpha = 0.28f * brightness)
            NodeType.ROOT -> TraceDim.copy(alpha = 0.35f * brightness)
            NodeType.PLATE_ANCHOR -> SiliconCool.copy(alpha = 0.32f * brightness)
        }
        val rad = when (n.type) {
            NodeType.SEED_CORE -> 4.2f
            NodeType.ACTIVE_TIP -> 2.4f + n.energy * 1.8f
            NodeType.JUNCTION -> 2.2f
            NodeType.ROOT -> 1.8f
            NodeType.PLATE_ANCHOR -> 2f
        } * stressMul
        drawCircle(color = col, radius = rad, center = center)
    }

    // 7) Edge vignette — substrate frame, not a central glow
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0x88050508)),
            center = Offset(wPx * 0.5f, hPx * 0.52f),
            radius = maxOf(wPx, hPx) * 0.85f,
        ),
        topLeft = Offset.Zero,
        size = size,
    )
}
