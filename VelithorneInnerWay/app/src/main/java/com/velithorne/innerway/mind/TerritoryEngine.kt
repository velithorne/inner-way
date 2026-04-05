package com.velithorne.innerway.mind

import android.content.Context
import androidx.core.content.edit
import com.velithorne.innerway.perception.EnvironmentalContext
import com.velithorne.innerway.render.TerritoryCell
import com.velithorne.innerway.render.TerritoryMap
import org.json.JSONArray
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Persistent habitat grid: where growth succeeded, where touch landed, where motion feels risky.
 * Smooth updates only — no hard jumps.
 */
class TerritoryEngine(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private var cols: Int = 0
    private var rows: Int = 0

    private var stability = FloatArray(0)
    private var disturbance = FloatArray(0)
    private var touchExposure = FloatArray(0)
    private var occupancy = FloatArray(0)
    private var growthAffinity = FloatArray(0)
    private var restAffinity = FloatArray(0)

    /** Rolling hint for debug: weighted mean direction toward high frontier affinity. */
    private var favoredDirectionRad: Float = 0f

    init {
        loadOrInit()
    }

    fun snapshot(): TerritoryMap {
        val cells = ArrayList<TerritoryCell>(cols * rows)
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val i = index(x, y)
                cells.add(
                    TerritoryCell(
                        xIndex = x,
                        yIndex = y,
                        stability = stability[i],
                        disturbance = disturbance[i],
                        touchExposure = touchExposure[i],
                        occupancy = occupancy[i],
                        growthAffinity = growthAffinity[i],
                        restAffinity = restAffinity[i],
                    ),
                )
            }
        }
        return TerritoryMap(cols, rows, cells)
    }

    fun debugStats(): TerritoryDebugStats {
        if (cols == 0 || rows == 0) {
            return TerritoryDebugStats(
                favoredDirectionRad = 0f,
                avgFrontierAffinity = 0f,
                avgSafeZoneAffinity = 0f,
                exploredCellCount = 0,
                matureTerritoryCells = 0,
                preferenceMode = TerritoryPreferenceMode.BALANCED,
                gridCols = cols,
                gridRows = rows,
            )
        }
        var frontierSum = 0f
        var safeSum = 0f
        var explored = 0
        var mature = 0
        var n = 0
        for (i in stability.indices) {
            val occ = occupancy[i]
            val ga = growthAffinity[i]
            val st = stability[i]
            val dist = disturbance[i]
            if (occ > 0.08f) explored++
            if (occ > 0.42f && ga > 0.38f) mature++
            val frontier = (1f - occ) * (0.35f + ga) * (1f - dist * 0.5f)
            val safe = st * (0.4f + occ) * (1f - dist * 0.4f)
            frontierSum += frontier
            safeSum += safe
            n++
        }
        val avgF = if (n > 0) frontierSum / n else 0f
        val avgS = if (n > 0) safeSum / n else 0f
        return TerritoryDebugStats(
            favoredDirectionRad = favoredDirectionRad,
            avgFrontierAffinity = avgF,
            avgSafeZoneAffinity = avgS,
            exploredCellCount = explored,
            matureTerritoryCells = mature,
            preferenceMode = lastMode,
            gridCols = cols,
            gridRows = rows,
        )
    }

    /**
     * Call when canvas layout is known; rebuilds grid if aspect bucket changes.
     */
    fun ensureGridForAspect(aspect: Float) {
        val (c, r) = gridDimensions(aspect)
        if (c == cols && r == rows && cols > 0) return
        resizeGrid(c, r)
    }

    /**
     * Global smoothing + environmental coupling (motion smears into “used” zones).
     */
    fun environmentalTick(
        dt: Float,
        aspect: Float,
        state: InternalState,
        imprint: GrowthImprintModel,
        environment: EnvironmentalContext,
        hints: SomaticHints,
    ) {
        ensureGridForAspect(aspect)
        val d = dt.coerceIn(0f, 0.6f)
        val motion = environment.motionEnergy.coerceIn(0f, 1f)
        val stillNorm = (hints.stillnessDurationSeconds / 120f).coerceIn(0f, 1f)

        lastMode = computePreferenceMode(state, imprint, environment, hints)

        for (i in stability.indices) {
            touchExposure[i] *= (1f - d * 0.45f).coerceAtLeast(0.5f)

            // Motion disturbs more where the body already lives (active use).
            val occBoost = 0.35f + occupancy[i] * 0.65f
            val targetDisturb = motion * occBoost * (0.55f + imprint.disturbanceBias * 0.35f)
            disturbance[i] = lerp(disturbance[i], targetDisturb, d * 0.55f)

            // Stillness heals local disturbance and raises stability in quiet zones.
            val calmPull = stillNorm * (0.25f + imprint.stillnessAffinity * 0.35f) * (1f - motion * 0.8f)
            disturbance[i] = (disturbance[i] - calmPull * d * 0.35f).coerceIn(0f, 1f)
            stability[i] = lerp(
                stability[i],
                (0.42f + restAffinity[i] * 0.28f + (1f - disturbance[i]) * 0.35f).coerceIn(0f, 1f),
                d * 0.12f,
            )

            // Charging + trust gently heals frontier affinity when safe.
            if (environment.charging && environment.thermalRatio < 0.55f) {
                val boost = d * 0.02f * imprint.chargeTrust * (1f - occupancy[i]).coerceIn(0f, 1f)
                growthAffinity[i] = (growthAffinity[i] + boost).coerceIn(0f, 1f)
            }
        }
        recomputeFavoredDirection()
        persist()
    }

    /** User touched / dragged — does not force growth; raises local exposure. */
    fun recordTouchNormalized(centerX: Float, centerY: Float, radiusCells: Float = 1.6f) {
        if (cols == 0 || rows == 0) return
        val cx = (centerX * cols).coerceIn(0f, cols - 1e-3f)
        val cy = (centerY * rows).coerceIn(0f, rows - 1e-3f)
        val ix = cx.toInt().coerceIn(0, cols - 1)
        val iy = cy.toInt().coerceIn(0, rows - 1)
        val r = radiusCells
        for (dy in -2..2) {
            for (dx in -2..2) {
                val x = ix + dx
                val y = iy + dy
                if (x !in 0 until cols || y !in 0 until rows) continue
                val dist = hypot((x + 0.5f - cx).toDouble(), (y + 0.5f - cy).toDouble()).toFloat()
                if (dist > r * 1.2f) continue
                val i = index(x, y)
                val add = (1f - dist / (r * 1.2f)).coerceIn(0f, 1f) * 0.22f
                touchExposure[i] = (touchExposure[i] + add).coerceIn(0f, 1f)
            }
        }
        persist()
    }

    /**
     * Weight for choosing a candidate extension direction (higher = more likely).
     */
    fun tipExtensionWeight(
        fromX: Float,
        fromY: Float,
        toX: Float,
        toY: Float,
        state: InternalState,
        imprint: GrowthImprintModel,
        charging: Boolean,
        hints: SomaticHints,
    ): Float {
        if (cols == 0) return 1f
        val s = sampleHabitat(toX, toY)
        val fromS = sampleHabitat(fromX, fromY)
        return habitatGrowthScore(s, fromS, state, imprint, charging, hints)
    }

    fun branchDirectionWeight(
        atX: Float,
        atY: Float,
        state: InternalState,
        imprint: GrowthImprintModel,
        charging: Boolean,
        hints: SomaticHints,
    ): Float {
        if (cols == 0) return 1f
        val s = sampleHabitat(atX, atY)
        val neutral = TerritorySample(0.5f, 0.25f, 0f, 0.2f, 0.35f, 0.3f)
        return habitatGrowthScore(s, neutral, state, imprint, charging, hints)
    }

    /** After a growth physics step — reinforces or punishes local habitat. */
    fun recordGrowthOutcome(
        toX: Float,
        toY: Float,
        accreted: Boolean,
        progressDelta: Float,
        plateAdded: Boolean,
        state: InternalState,
        imprint: GrowthImprintModel,
    ) {
        if (cols == 0) return
        val cx = (toX * cols).toInt().coerceIn(0, cols - 1)
        val cy = (toY * rows).toInt().coerceIn(0, rows - 1)
        val i = index(cx, cy)
        val d = progressDelta.coerceIn(0f, 0.2f)

        occupancy[i] = (occupancy[i] + d * 2.5f + if (accreted) 0.04f else 0.02f).coerceIn(0f, 1f)
        if (accreted || progressDelta > 0.01f) {
            growthAffinity[i] = (growthAffinity[i] + 0.035f * (1f - imprint.stressLoad * 0.4f)).coerceIn(0f, 1f)
            stability[i] = (stability[i] + 0.012f).coerceIn(0f, 1f)
        }
        if (plateAdded) {
            restAffinity[i] = (restAffinity[i] + 0.05f).coerceIn(0f, 1f)
            for (n in neighbors4(cx, cy)) {
                restAffinity[n] = (restAffinity[n] + 0.02f).coerceIn(0f, 1f)
            }
        }
        // Stalled / weak step — slight risk memory
        if (!accreted && progressDelta < 0.002f) {
            growthAffinity[i] = (growthAffinity[i] - 0.018f * (0.5f + imprint.stressLoad * 0.5f)).coerceIn(0f, 1f)
            disturbance[i] = (disturbance[i] + 0.015f).coerceIn(0f, 1f)
        }
        persist()
    }

    fun recordRetraction(x: Float, y: Float) {
        if (cols == 0) return
        val cx = (x * cols).toInt().coerceIn(0, cols - 1)
        val cy = (y * rows).toInt().coerceIn(0, rows - 1)
        val i = index(cx, cy)
        growthAffinity[i] = (growthAffinity[i] - 0.06f).coerceIn(0f, 1f)
        disturbance[i] = (disturbance[i] + 0.05f).coerceIn(0f, 1f)
        persist()
    }

    private data class TerritorySample(
        val stability: Float,
        val disturbance: Float,
        val touchExposure: Float,
        val occupancy: Float,
        val growthAffinity: Float,
        val restAffinity: Float,
    )

    private fun sampleHabitat(nx: Float, ny: Float): TerritorySample {
        if (cols < 2 || rows < 2) {
            return TerritorySample(0.5f, 0.25f, 0f, 0.2f, 0.35f, 0.3f)
        }
        val x = nx * cols
        val y = ny * rows
        val x0 = min(x.toInt(), cols - 2).coerceAtLeast(0)
        val y0 = min(y.toInt(), rows - 2).coerceAtLeast(0)
        val tx = (x - x0).coerceIn(0f, 1f)
        val ty = (y - y0).coerceIn(0f, 1f)
        fun b(arr: FloatArray): Float {
            val v00 = arr[index(x0, y0)]
            val v10 = arr[index(x0 + 1, y0)]
            val v01 = arr[index(x0, y0 + 1)]
            val v11 = arr[index(x0 + 1, y0 + 1)]
            val a0 = v00 * (1 - tx) + v10 * tx
            val a1 = v01 * (1 - tx) + v11 * tx
            return a0 * (1 - ty) + a1 * ty
        }
        return TerritorySample(
            stability = b(stability),
            disturbance = b(disturbance),
            touchExposure = b(touchExposure),
            occupancy = b(occupancy),
            growthAffinity = b(growthAffinity),
            restAffinity = b(restAffinity),
        )
    }

    private fun habitatGrowthScore(
        s: TerritorySample,
        fromS: TerritorySample,
        state: InternalState,
        imprint: GrowthImprintModel,
        charging: Boolean,
        hints: SomaticHints,
    ): Float {
        val lowOcc = (1f - s.occupancy).coerceIn(0f, 1f)
        val safeCore = s.stability * (0.45f + s.occupancy * 0.55f) * (1f - s.disturbance * 0.45f)
        val frontier = lowOcc * (0.55f + s.growthAffinity * 0.45f) * (1f - s.disturbance * 0.35f)
        val reconnect = (fromS.occupancy * s.occupancy) * (0.35f + s.growthAffinity * 0.4f)
        val touch = s.touchExposure
        val touchSeek = touch * (0.55f + imprint.disturbanceBias * 0.35f)
        val touchAvoid = (1f - touch * 0.85f).coerceIn(0.15f, 1f)

        var w = 0.5f
        when (state) {
            InternalState.CALM -> w = safeCore * 0.55f + frontier * 0.35f + reconnect * 0.15f
            InternalState.STRESSED -> w = safeCore * 0.75f + frontier * 0.12f + (1f - lowOcc) * 0.25f
            InternalState.RECOVERING -> w = reconnect * 0.45f + safeCore * 0.4f + frontier * 0.2f
            InternalState.CURIOUS, InternalState.ALERT -> w = frontier * 0.55f + touchSeek * 0.35f + safeCore * 0.15f
            InternalState.RESTING, InternalState.DORMANT -> w = safeCore * 0.45f + s.restAffinity * 0.45f + frontier * 0.08f
            InternalState.DEFENSIVE -> w = safeCore * 0.65f + touchAvoid * 0.35f + frontier * 0.1f
            InternalState.HUNGRY -> w = safeCore * 0.5f + frontier * 0.2f + reconnect * 0.15f
            else -> w = safeCore * 0.4f + frontier * 0.35f + reconnect * 0.15f
        }

        // Imprint modifiers
        w *= 1f - imprint.stressLoad * (lowOcc * 0.35f) * (if (state == InternalState.STRESSED) 0.5f else 0.2f)
        w *= 1f + imprint.calmReserve * (safeCore * 0.12f)
        w *= 1f + imprint.disturbanceBias * (touchSeek * 0.15f + frontier * 0.08f)
        w *= 1f + imprint.stillnessAffinity * (s.stability * 0.1f + (1f - s.disturbance) * 0.08f)
        if (charging && imprint.chargeTrust > 0.35f) {
            w *= 1f + imprint.chargeTrust * frontier * 0.12f
        }
        if (hints.stillnessDurationSeconds > 45f) {
            w *= 1f + imprint.stillnessAffinity * 0.08f
        }

        return (w * (0.35f + s.growthAffinity * 0.65f)).coerceIn(0.04f, 2.2f)
    }

    private fun computePreferenceMode(
        state: InternalState,
        imprint: GrowthImprintModel,
        environment: EnvironmentalContext,
        hints: SomaticHints,
    ): TerritoryPreferenceMode {
        if (state == InternalState.DEFENSIVE) return TerritoryPreferenceMode.TOUCH_AVOID
        if (state == InternalState.CURIOUS || state == InternalState.ALERT) return TerritoryPreferenceMode.TOUCH_CURIOUS
        if (state == InternalState.STRESSED || imprint.stressLoad > 0.62f) return TerritoryPreferenceMode.SHELTER_SEEK
        if (state == InternalState.RECOVERING) return TerritoryPreferenceMode.CAUTIOUS_RECONNECT
        if (state == InternalState.RESTING || state == InternalState.DORMANT) return TerritoryPreferenceMode.ROOT_AND_REST
        if (state == InternalState.CALM && imprint.calmReserve > 0.55f) return TerritoryPreferenceMode.BALANCED
        if (environment.motionEnergy > 0.2f) return TerritoryPreferenceMode.FRONTIER_PROBE
        return TerritoryPreferenceMode.SAFE_CORE
    }

    private fun recomputeFavoredDirection() {
        if (cols == 0 || rows == 0) return
        var wx = 0.0
        var wy = 0.0
        var wsum = 1e-4
        val cx = 0.5f
        val cy = 0.52f
        for (y in 0 until rows) {
            for (x in 0 until cols) {
                val i = index(x, y)
                val nx = (x + 0.5f) / cols
                val ny = (y + 0.5f) / rows
                val lowOcc = 1f - occupancy[i]
                val w = (0.15f + growthAffinity[i] * 0.5f + lowOcc * 0.35f) * (1f - disturbance[i] * 0.3f)
                wx += (nx - cx) * w
                wy += (ny - cy) * w
                wsum += w
            }
        }
        favoredDirectionRad = atan2((wy / wsum).toFloat(), (wx / wsum).toFloat())
    }

    private var lastMode: TerritoryPreferenceMode = TerritoryPreferenceMode.BALANCED

    private fun gridDimensions(aspect: Float): Pair<Int, Int> {
        val a = aspect.coerceIn(0.45f, 2.2f)
        val c = 12
        val r = (c / a).roundToInt().coerceIn(16, 24)
        return c to r
    }

    private fun resizeGrid(newCols: Int, newRows: Int) {
        if (newCols == cols && newRows == rows) return
        val oldC = cols
        val oldR = rows
        val oldStab = stability
        val oldDist = disturbance
        val oldTouch = touchExposure
        val oldOcc = occupancy
        val oldGa = growthAffinity
        val oldRest = restAffinity

        cols = newCols
        rows = newRows
        val n = cols * rows
        stability = FloatArray(n) { 0.45f }
        disturbance = FloatArray(n) { 0.12f }
        touchExposure = FloatArray(n) { 0f }
        occupancy = FloatArray(n) { 0f }
        growthAffinity = FloatArray(n) { 0.28f }
        restAffinity = FloatArray(n) { 0.22f }

        if (oldC > 0 && oldR > 0 && oldStab.isNotEmpty()) {
            for (y in 0 until rows) {
                for (x in 0 until cols) {
                    val nx = x / cols.toFloat() * oldC
                    val ny = y / rows.toFloat() * oldR
                    val ox = nx.toInt().coerceIn(0, oldC - 1)
                    val oy = ny.toInt().coerceIn(0, oldR - 1)
                    val oi = oy * oldC + ox
                    val di = index(x, y)
                    stability[di] = oldStab[oi]
                    disturbance[di] = oldDist[oi]
                    touchExposure[di] = oldTouch[oi]
                    occupancy[di] = oldOcc[oi]
                    growthAffinity[di] = oldGa[oi]
                    restAffinity[di] = oldRest[oi]
                }
            }
        }
        persist()
    }

    private fun index(x: Int, y: Int): Int = y * cols + x

    private fun neighbors4(cx: Int, cy: Int): List<Int> = buildList {
        if (cx > 0) add(index(cx - 1, cy))
        if (cx < cols - 1) add(index(cx + 1, cy))
        if (cy > 0) add(index(cx, cy - 1))
        if (cy < rows - 1) add(index(cx, cy + 1))
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun persist() {
        if (cols == 0) return
        prefs.edit {
            putInt(KEY_COLS, cols)
            putInt(KEY_ROWS, rows)
            putString(KEY_STAB, JSONArray().apply { stability.forEach { put(it.toDouble()) } }.toString())
            putString(KEY_DIST, JSONArray().apply { disturbance.forEach { put(it.toDouble()) } }.toString())
            putString(KEY_TOUCH, JSONArray().apply { touchExposure.forEach { put(it.toDouble()) } }.toString())
            putString(KEY_OCC, JSONArray().apply { occupancy.forEach { put(it.toDouble()) } }.toString())
            putString(KEY_GA, JSONArray().apply { growthAffinity.forEach { put(it.toDouble()) } }.toString())
            putString(KEY_REST, JSONArray().apply { restAffinity.forEach { put(it.toDouble()) } }.toString())
        }
    }

    private fun loadOrInit() {
        cols = prefs.getInt(KEY_COLS, 0)
        rows = prefs.getInt(KEY_ROWS, 0)
        if (cols <= 0 || rows <= 0) {
            resizeGrid(12, 20)
            return
        }
        val n = cols * rows
        fun loadArr(key: String, fallback: Float): FloatArray {
            val s = prefs.getString(key, null) ?: return FloatArray(n) { fallback }
            return try {
                val arr = JSONArray(s)
                FloatArray(n) { i -> arr.optDouble(i, fallback.toDouble()).toFloat() }
            } catch (_: Exception) {
                FloatArray(n) { fallback }
            }
        }
        stability = loadArr(KEY_STAB, 0.45f)
        disturbance = loadArr(KEY_DIST, 0.12f)
        touchExposure = loadArr(KEY_TOUCH, 0f)
        occupancy = loadArr(KEY_OCC, 0f)
        growthAffinity = loadArr(KEY_GA, 0.28f)
        restAffinity = loadArr(KEY_REST, 0.22f)
        if (stability.size != n) {
            resizeGrid(cols, rows)
        }
    }

    companion object {
        private const val PREFS = "velithorne_territory"
        private const val KEY_COLS = "cols"
        private const val KEY_ROWS = "rows"
        private const val KEY_STAB = "stab"
        private const val KEY_DIST = "dist"
        private const val KEY_TOUCH = "touch"
        private const val KEY_OCC = "occ"
        private const val KEY_GA = "ga"
        private const val KEY_REST = "rest"
    }
}
