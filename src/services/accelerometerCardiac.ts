import { Accelerometer } from 'expo-sensors';
import { butterworth4thOrderBandpass } from '../dsp/filters';

const ACC_HZ = 100;
const BUFFER_SEC = 10;
const BUFFER_LEN = ACC_HZ * BUFFER_SEC;
const LOW_HZ = 0.5;
const HIGH_HZ = 4;
const GRAVITY_LP_HZ = 0.1;
const WARMUP_SEC = 20;
const NOISE_WINDOW_SEC = 15;
const SUBHARMONIC_RATIO = 0.4;

export type AccelerometerCardiacOutput = {
  bpm: number;
  confidence: number;
  amplitude: number;
  /** Dominant frequency in cardiac band (Hz), for coherence */
  freqHz: number;
  /** First 20s after start: baseline stabilising; UI should show SETTLING */
  isWarmup: boolean;
};

function magnitude(x: number, y: number, z: number): number {
  return Math.sqrt(x * x + y * y + z * z);
}

function stdDev(samples: Float32Array): number {
  if (samples.length < 2) return 0;
  let mean = 0;
  for (let i = 0; i < samples.length; i++) mean += samples[i];
  mean /= samples.length;
  let v = 0;
  for (let i = 0; i < samples.length; i++) {
    const d = samples[i] - mean;
    v += d * d;
  }
  return Math.sqrt(v / samples.length);
}

function autocorrAtLag(
  x: Float32Array,
  mean: number,
  variance: number,
  lag: number
): number {
  const n = x.length;
  if (lag < 1 || lag >= n || variance < 1e-18) return 0;
  let sum = 0;
  const count = n - lag;
  for (let i = 0; i < count; i++) {
    sum += (x[i] - mean) * (x[i + lag] - mean);
  }
  return sum / (count * variance);
}

type PeakResult = {
  freqHz: number;
  prominence: number;
  amplitude: number;
  bestLag: number;
  bestR: number;
};

/** Autocorrelation peak with sub-harmonic correction (prefer L/2 when it is the real beat). */
function autocorrPeakFreq(
  x: Float32Array,
  sampleRate: number,
  minHz: number,
  maxHz: number
): PeakResult {
  const n = x.length;
  if (n < 64) {
    return { freqHz: 0, prominence: 0, amplitude: 0, bestLag: 0, bestR: 0 };
  }

  let maxAmp = 0;
  for (let i = 0; i < n; i++) {
    const a = Math.abs(x[i]);
    if (a > maxAmp) maxAmp = a;
  }

  const minLag = Math.max(2, Math.floor(sampleRate / maxHz));
  const maxLag = Math.min(n - 1, Math.ceil(sampleRate / minHz));
  if (minLag >= maxLag) {
    return { freqHz: 0, prominence: 0, amplitude: maxAmp, bestLag: 0, bestR: 0 };
  }

  let mean = 0;
  for (let i = 0; i < n; i++) mean += x[i];
  mean /= n;
  let varSum = 0;
  for (let i = 0; i < n; i++) {
    const d = x[i] - mean;
    varSum += d * d;
  }
  const variance = varSum / n || 1e-12;

  let bestLag = minLag;
  let bestR = -1;
  const rAtLag: number[] = [];
  for (let lag = minLag; lag <= maxLag; lag++) {
    const r = autocorrAtLag(x, mean, variance, lag);
    rAtLag.push(r);
    if (r > bestR) {
      bestR = r;
      bestLag = lag;
    }
  }

  let useLag = bestLag;
  let useR = bestR;
  const halfLag = Math.floor(bestLag / 2);
  if (halfLag >= minLag && halfLag <= maxLag) {
    const rHalf = autocorrAtLag(x, mean, variance, halfLag);
    if (rHalf > SUBHARMONIC_RATIO * bestR) {
      useLag = halfLag;
      useR = rHalf;
    }
  }

  let medianR = 0;
  if (rAtLag.length) {
    const sorted = [...rAtLag].sort((a, b) => a - b);
    medianR = sorted[Math.floor(sorted.length / 2)];
  }
  const prominence = Math.max(0, useR - medianR);
  const freqHz = useLag > 0 ? sampleRate / useLag : 0;

  return { freqHz, prominence, amplitude: maxAmp, bestLag: useLag, bestR: useR };
}

/**
 * Phase 2 confidence: prominence + SNR vs noise std from last 15s of bandpassed signal only.
 */
function confidenceSteady(
  prominence: number,
  bandpassLast15s: Float32Array,
  bestR: number
): number {
  const noiseFloor = stdDev(bandpassLast15s);
  const n = bandpassLast15s.length;
  let rms = 0;
  if (n > 0) {
    let s2 = 0;
    for (let i = 0; i < n; i++) s2 += bandpassLast15s[i] * bandpassLast15s[i];
    rms = Math.sqrt(s2 / n);
  }
  const snr = rms / (noiseFloor + 1e-9);
  const promTerm = Math.min(55, prominence * 70);
  const snrTerm = Math.min(40, 12 * Math.log1p(Math.max(0, snr - 0.5)));
  const rTerm = Math.min(25, Math.max(0, bestR) * 35);
  return Math.min(100, promTerm + snrTerm + rTerm);
}

export type AccelerometerCardiacHandle = {
  getLatest: () => AccelerometerCardiacOutput;
  stop: () => void;
};

/**
 * Ballistocardiography-style accelerometer pipeline: gravity removal, cardiac bandpass,
 * autocorrelation on a 10 s buffer.
 */
export function startAccelerometerCardiac(
  onTick?: (out: AccelerometerCardiacOutput) => void
): AccelerometerCardiacHandle {
  const samples: number[] = [];
  let gravLp = 9.81;
  const alphaGrav =
    (1 / ACC_HZ) / (1 / (2 * Math.PI * GRAVITY_LP_HZ) + 1 / ACC_HZ);

  const sessionStartMs = Date.now();

  let latest: AccelerometerCardiacOutput = {
    bpm: 0,
    confidence: 0,
    amplitude: 0,
    freqHz: 0,
    isWarmup: true,
  };

  Accelerometer.setUpdateInterval(1000 / ACC_HZ);

  let tick = 0;
  const sub = Accelerometer.addListener(({ x, y, z }) => {
    const m = magnitude(x, y, z);
    gravLp = alphaGrav * m + (1 - alphaGrav) * gravLp;
    const residual = m - gravLp;
    samples.push(residual);
    while (samples.length > BUFFER_LEN) samples.shift();

    tick += 1;
    if (samples.length < ACC_HZ * 2) return;
    if (tick % 10 !== 0) return;

    const elapsedSec = (Date.now() - sessionStartMs) / 1000;
    const isWarmup = elapsedSec < WARMUP_SEC;

    const buf = Float32Array.from(samples);
    const bp = butterworth4thOrderBandpass(buf, LOW_HZ, HIGH_HZ, ACC_HZ);
    const { freqHz, prominence, amplitude, bestR } = autocorrPeakFreq(
      bp,
      ACC_HZ,
      0.5,
      3.0
    );

    const cardiacCandidate = freqHz >= 0.5 && freqHz <= 3.0 && amplitude > 0.002;
    let bpm = cardiacCandidate ? 60 * freqHz : 0;

    if (cardiacCandidate && bpm > 0 && bpm < 45) {
      console.warn(
        `[Biofield] Low BPM detected (${bpm.toFixed(0)}) — possible sub-harmonic. Check if ${(bpm * 2).toFixed(0)} BPM is the true rate.`
      );
    }

    const n15 = Math.min(bp.length, ACC_HZ * NOISE_WINDOW_SEC);
    const bpLast15 = bp.slice(-n15);

    let confidence = 0;
    if (cardiacCandidate) {
      if (isWarmup) {
        const raw = confidenceSteady(prominence, bpLast15, bestR);
        confidence = Math.min(30, raw);
      } else {
        confidence = confidenceSteady(prominence, bpLast15, bestR);
      }
    } else {
      confidence = Math.min(30, prominence * 20);
      if (isWarmup) confidence = Math.min(30, confidence);
    }

    latest = {
      bpm,
      confidence,
      amplitude,
      freqHz: cardiacCandidate ? freqHz : 0,
      isWarmup,
    };
    onTick?.(latest);
  });

  return {
    getLatest: () => latest,
    stop: () => {
      sub.remove();
    },
  };
}
