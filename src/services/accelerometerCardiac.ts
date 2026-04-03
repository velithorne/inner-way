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

/** RMS window for ambient vs cardiac gating (Fix 1) */
const RMS_WINDOW_SEC = 5;
const MIN_RMS_MPS2 = 0.008;

/** First 10s: learn table noise floor; no cardiac reporting (Fix 4) */
const CONTROL_PHASE_SEC = 10;
const NOISE_FLOOR_MULTIPLIER = 2.5;

/** Autocorr peak must exceed this multiple of mean r (Fix 2) */
const PROMINENCE_MEAN_RATIO = 3.5;

const INTERFERENCE_HZ = [0.83, 1.0, 1.17, 1.67];
const INTERFERENCE_TOL_HZ = 0.05;

export type AccelerometerCardiacOutput = {
  bpm: number;
  confidence: number;
  amplitude: number;
  freqHz: number;
  isWarmup: boolean;
  /** First 10s: calibrating table noise floor */
  isControlPhase: boolean;
  possibleInterference: boolean;
};

function magnitude(x: number, y: number, z: number): number {
  return Math.sqrt(x * x + y * y + z * z);
}

function rms(samples: Float32Array): number {
  if (samples.length === 0) return 0;
  let s2 = 0;
  for (let i = 0; i < samples.length; i++) {
    s2 += samples[i] * samples[i];
  }
  return Math.sqrt(s2 / samples.length);
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

function nearInterferenceHz(freqHz: number): boolean {
  for (const f of INTERFERENCE_HZ) {
    if (Math.abs(freqHz - f) <= INTERFERENCE_TOL_HZ) return true;
  }
  return false;
}

type PeakResult = {
  freqHz: number;
  prominence: number;
  amplitude: number;
  bestLag: number;
  bestR: number;
  meanR: number;
  peakAcceptable: boolean;
};

/** Autocorrelation peak with sub-harmonic correction; peak must be ≥ 3.5× mean r (Fix 2). */
function autocorrPeakFreq(
  x: Float32Array,
  sampleRate: number,
  minHz: number,
  maxHz: number
): PeakResult {
  const empty = (): PeakResult => ({
    freqHz: 0,
    prominence: 0,
    amplitude: 0,
    bestLag: 0,
    bestR: 0,
    meanR: 0,
    peakAcceptable: false,
  });

  const n = x.length;
  if (n < 64) return empty();

  let maxAmp = 0;
  for (let i = 0; i < n; i++) {
    const a = Math.abs(x[i]);
    if (a > maxAmp) maxAmp = a;
  }

  const minLag = Math.max(2, Math.floor(sampleRate / maxHz));
  const maxLag = Math.min(n - 1, Math.ceil(sampleRate / minHz));
  if (minLag >= maxLag) {
    return { ...empty(), amplitude: maxAmp };
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
  let sumR = 0;
  let countR = 0;
  for (let lag = minLag; lag <= maxLag; lag++) {
    const r = autocorrAtLag(x, mean, variance, lag);
    rAtLag.push(r);
    sumR += r;
    countR += 1;
    if (r > bestR) {
      bestR = r;
      bestLag = lag;
    }
  }
  const meanR = countR > 0 ? sumR / countR : 0;

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

  const peakAcceptable =
    meanR > 1e-9 && useR >= PROMINENCE_MEAN_RATIO * meanR;

  return {
    freqHz,
    prominence,
    amplitude: maxAmp,
    bestLag: useLag,
    bestR: useR,
    meanR,
    peakAcceptable,
  };
}

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

export function startAccelerometerCardiac(
  onTick?: (out: AccelerometerCardiacOutput) => void
): AccelerometerCardiacHandle {
  const samples: number[] = [];
  let gravLp = 9.81;
  const alphaGrav =
    (1 / ACC_HZ) / (1 / (2 * Math.PI * GRAVITY_LP_HZ) + 1 / ACC_HZ);

  const sessionStartMs = Date.now();
  let sessionNoiseFloor = 0;

  let latest: AccelerometerCardiacOutput = {
    bpm: 0,
    confidence: 0,
    amplitude: 0,
    freqHz: 0,
    isWarmup: true,
    isControlPhase: true,
    possibleInterference: false,
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
    const isControlPhase = elapsedSec < CONTROL_PHASE_SEC;
    const isWarmup = elapsedSec < WARMUP_SEC;

    const buf = Float32Array.from(samples);
    const bp = butterworth4thOrderBandpass(buf, LOW_HZ, HIGH_HZ, ACC_HZ);

    const n5 = Math.min(bp.length, ACC_HZ * RMS_WINDOW_SEC);
    const bpLast5 = bp.slice(-n5);
    const rms5s = rms(bpLast5);

    if (isControlPhase) {
      sessionNoiseFloor = Math.max(sessionNoiseFloor, rms5s);
      latest = {
        bpm: 0,
        confidence: 0,
        amplitude: 0,
        freqHz: 0,
        isWarmup,
        isControlPhase: true,
        possibleInterference: false,
      };
      onTick?.(latest);
      return;
    }

    const floorRef = Math.max(sessionNoiseFloor, 1e-6);
    if (rms5s < MIN_RMS_MPS2 || rms5s < NOISE_FLOOR_MULTIPLIER * floorRef) {
      latest = {
        bpm: 0,
        confidence: 0,
        amplitude: 0,
        freqHz: 0,
        isWarmup,
        isControlPhase: false,
        possibleInterference: false,
      };
      onTick?.(latest);
      return;
    }

    const {
      freqHz,
      prominence,
      amplitude,
      bestR,
      peakAcceptable,
    } = autocorrPeakFreq(bp, ACC_HZ, 0.5, 3.0);

    if (!peakAcceptable) {
      latest = {
        bpm: 0,
        confidence: 0,
        amplitude: 0,
        freqHz: 0,
        isWarmup,
        isControlPhase: false,
        possibleInterference: false,
      };
      onTick?.(latest);
      return;
    }

    const cardiacCandidate = freqHz >= 0.5 && freqHz <= 3.0 && amplitude > 0.002;
    let bpm = cardiacCandidate ? 60 * freqHz : 0;

    if (cardiacCandidate && bpm > 0 && bpm < 45) {
      console.warn(
        `[Biofield] Low BPM detected (${bpm.toFixed(0)}) — possible sub-harmonic. Check if ${(bpm * 2).toFixed(0)} BPM is the true rate.`
      );
    }

    const interference = cardiacCandidate && nearInterferenceHz(freqHz);

    const n15 = Math.min(bp.length, ACC_HZ * NOISE_WINDOW_SEC);
    const bpLast15 = bp.slice(-n15);

    let confidence = 0;
    if (cardiacCandidate) {
      let raw = confidenceSteady(prominence, bpLast15, bestR);
      if (interference) raw *= 0.4;
      if (isWarmup) {
        confidence = Math.min(30, raw);
      } else {
        confidence = raw;
      }
    } else {
      confidence = Math.min(30, prominence * 20);
      if (isWarmup) confidence = Math.min(30, confidence);
    }

    if (!cardiacCandidate) {
      bpm = 0;
    }

    latest = {
      bpm,
      confidence,
      amplitude: cardiacCandidate ? amplitude : 0,
      freqHz: cardiacCandidate ? freqHz : 0,
      isWarmup,
      isControlPhase: false,
      possibleInterference: interference && cardiacCandidate && bpm > 0,
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
