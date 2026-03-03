package com.falcor.civilization.engine.analysis

import com.falcor.civilization.domain.ObservationPoint
import kotlin.math.*

data class ExtractedFeatures(
    val slopeTempIr: Double,
    val slopeRpm: Double,
    val slopeEmAmp: Double,
    val deltaEmAmp: Double,
    val fftPeakRpm: Double,
    val crossCorrTempRpm: Double,
    val crossCorrRpmEm: Double,
    val medianVibRms: Double
)

object FeatureExtractor {
    fun extract(observations: List<ObservationPoint>): ExtractedFeatures {
        if (observations.size < 10) {
            return ExtractedFeatures(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        }
        val t = observations.map { it.t }
        val tempIr = observations.map { it.channels["temp_ir"] ?: 0.0 }
        val rpm = observations.map { it.channels["rpm"] ?: 0.0 }
        val emAmp = observations.map { it.channels["em_amp"] ?: 0.0 }
        val vibRms = observations.map { it.channels["vib_rms"] ?: 0.0 }

        val slopeTempIr = linearSlope(t, tempIr)
        val slopeRpm = linearSlope(t, rpm)
        val slopeEmAmp = linearSlope(t, emAmp)
        val deltaEmAmp = (emAmp.lastOrNull() ?: 0.0) - (emAmp.firstOrNull() ?: 0.0)
        val fftPeakRpm = simpleFftPeak(rpm)
        val crossCorrTempRpm = crossCorrelation(tempIr, rpm)
        val crossCorrRpmEm = crossCorrelation(rpm, emAmp)
        val medianVibRms = vibRms.sorted().let { it[it.size / 2] }

        return ExtractedFeatures(
            slopeTempIr = slopeTempIr,
            slopeRpm = slopeRpm,
            slopeEmAmp = slopeEmAmp,
            deltaEmAmp = deltaEmAmp,
            fftPeakRpm = fftPeakRpm,
            crossCorrTempRpm = crossCorrTempRpm,
            crossCorrRpmEm = crossCorrRpmEm,
            medianVibRms = medianVibRms
        )
    }

    private fun linearSlope(x: List<Double>, y: List<Double>): Double {
        val n = minOf(x.size, y.size)
        if (n < 2) return 0.0
        val meanX = x.take(n).average()
        val meanY = y.take(n).average()
        var num = 0.0
        var den = 0.0
        for (i in 0 until n) {
            val dx = x[i] - meanX
            num += dx * (y[i] - meanY)
            den += dx * dx
        }
        return if (abs(den) < 1e-10) 0.0 else num / den
    }

    private fun simpleFftPeak(signal: List<Double>): Double {
        val n = signal.size
        if (n < 4) return 0.0
        val half = n / 2
        var maxMag = 0.0
        for (k in 1 until half) {
            var re = 0.0
            var im = 0.0
            for (i in 0 until n) {
                val angle = -2 * PI * k * i / n
                re += signal[i] * cos(angle)
                im += signal[i] * sin(angle)
            }
            val mag = sqrt(re * re + im * im) / n
            if (mag > maxMag) maxMag = mag
        }
        return maxMag
    }

    private fun crossCorrelation(a: List<Double>, b: List<Double>): Double {
        val n = minOf(a.size, b.size)
        if (n < 2) return 0.0
        val meanA = a.take(n).average()
        val meanB = b.take(n).average()
        var num = 0.0
        var denA = 0.0
        var denB = 0.0
        for (i in 0 until n) {
            val da = a[i] - meanA
            val db = b[i] - meanB
            num += da * db
            denA += da * da
            denB += db * db
        }
        val den = sqrt(denA * denB)
        return if (den < 1e-10) 0.0 else num / den
    }
}
