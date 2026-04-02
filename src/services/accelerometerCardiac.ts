import { Accelerometer } from 'expo-sensors';
import { butterworth4thOrderBandpass } from '../dsp/filters';

const ACC_HZ = 100;
const BUFFER_SEC = 10;
const BUFFER_LEN = ACC_HZ * BUFFER_SEC;
const LOW_HZ = 0.5;
const HIGH_HZ = 4;
const GRAVITY_LP_HZ = 0.1;

export type AccelerometerCardiacOutput = {
  bpm: number;
  confidence: number;
  amplitude: number;
  /** Dominant frequency in cardiac band (Hz), for coherence */
  freqHz: number;
};

function magnitude(x: number, y: number, z: number): number {
  return Math.sqrt(x * x + y * y + z * z);
}

/** Normalized autocorrelation peak in lag range [minLag, maxLag] (inclusive). */
function autocorrPeakFreq(
  x: Float32Array,
  sampleRate: number,
  minHz: number,
  maxHz: number
): { freqHz: number; prominence: number; amplitude: number } {
  const n = x.length;
  if (n < 64) return { freqHz: 0, prominence: 0, amplitude: 0 };

  let maxAmp = 0;
  for (let i = 0; i < n; i++) {
    const a = Math.abs(x[i]);
    if (a > maxAmp) maxAmp = a;
  }

  const minLag = Math.max(2, Math.floor(sampleRate / maxHz));
  const maxLag = Math.min(n - 1, Math.ceil(sampleRate / minHz));
  if (minLag >= maxLag) return { freqHz: 0, prominence: 0, amplitude: maxAmp };

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
    let sum = 0;
    const count = n - lag;
    for (let i = 0; i < count; i++) {
      sum += (x[i] - mean) * (x[i + lag] - mean);
    }
    const r = sum / (count * variance);
    rAtLag.push(r);
    if (r > bestR) {
      bestR = r;
      bestLag = lag;
    }
  }

  const freqHz = bestLag > 0 ? sampleRate / bestLag : 0;
  let medianR = 0;
  if (rAtLag.length) {
    const sorted = [...rAtLag].sort((a, b) => a - b);
    medianR = sorted[Math.floor(sorted.length / 2)];
  }
  const prominence = Math.max(0, bestR - medianR);

  return { freqHz, prominence, amplitude: maxAmp };
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

  let latest: AccelerometerCardiacOutput = {
    bpm: 0,
    confidence: 0,
    amplitude: 0,
    freqHz: 0,
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

    const buf = Float32Array.from(samples);
    const bp = butterworth4thOrderBandpass(buf, LOW_HZ, HIGH_HZ, ACC_HZ);
    const { freqHz, prominence, amplitude } = autocorrPeakFreq(bp, ACC_HZ, 0.5, 3.0);

    const cardiacCandidate = freqHz >= 0.5 && freqHz <= 3.0 && amplitude > 0.002;
    const bpm = cardiacCandidate ? 60 * freqHz : 0;
    const confidence = cardiacCandidate
      ? Math.min(100, prominence * 50 + Math.min(40, amplitude * 8000))
      : Math.min(30, prominence * 20);

    latest = {
      bpm,
      confidence,
      amplitude,
      freqHz: cardiacCandidate ? freqHz : 0,
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
