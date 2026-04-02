import { Magnetometer } from 'expo-sensors';
import { butterworth4thOrderBandpass, rollingMean } from '../dsp/filters';
import { fftWithSampleRate } from '../dsp/fft';

const TARGET_HZ = 200;
const BASELINE_SEC = 60;
const FFT_SEC = 10;
const LOW_HZ = 0.5;
const HIGH_HZ = 4;
const SNR_THRESHOLD = 3;

function magnitudeµT(x: number, y: number, z: number): number {
  return Math.sqrt(x * x + y * y + z * z);
}

export type MagnetometerCardiacOutput = {
  detected: boolean;
  freqHz: number;
  snr: number;
};

export type MagnetometerCardiacHandle = {
  getLatest: () => MagnetometerCardiacOutput;
  stop: () => void;
};

export function startMagnetometerCardiac(
  onTick?: (out: MagnetometerCardiacOutput) => void
): MagnetometerCardiacHandle {
  const magHistory: number[] = [];
  const maxBaseline = TARGET_HZ * BASELINE_SEC;
  const fftSamples = TARGET_HZ * FFT_SEC;

  let latest: MagnetometerCardiacOutput = {
    detected: false,
    freqHz: 0,
    snr: 0,
  };

  const intervalMs = Math.max(4, Math.round(1000 / TARGET_HZ));
  Magnetometer.setUpdateInterval(intervalMs);

  let tick = 0;
  const sub = Magnetometer.addListener(({ x, y, z }) => {
    const m = magnitudeµT(x, y, z);
    magHistory.push(m);
    while (magHistory.length > maxBaseline) magHistory.shift();

    tick += 1;
    if (magHistory.length < TARGET_HZ * 5) return;
    if (tick % 20 !== 0) return;

    const arr = Float32Array.from(magHistory);
    const win = Math.min(arr.length, maxBaseline);
    const baselineArr = rollingMean(arr, win);
    const residual = new Float32Array(arr.length);
    for (let i = 0; i < arr.length; i++) residual[i] = arr[i] - baselineArr[i];

    const tail = residual.slice(-fftSamples);
    const bp = butterworth4thOrderBandpass(tail, LOW_HZ, HIGH_HZ, TARGET_HZ);

    const { frequencies, magnitudes } = fftWithSampleRate(bp, TARGET_HZ);

    const noiseBins: number[] = [];
    for (let k = 1; k < magnitudes.length - 1; k++) {
      const f = frequencies[k];
      if (f < LOW_HZ || f > HIGH_HZ) noiseBins.push(magnitudes[k]);
    }
    noiseBins.sort((a, b) => a - b);
    const noiseFloor =
      noiseBins.length > 0
        ? noiseBins[Math.floor(noiseBins.length * 0.5)]
        : 1e-9;

    let bestK = -1;
    let peakMag = 0;
    for (let k = 1; k < magnitudes.length - 1; k++) {
      const f = frequencies[k];
      if (f < LOW_HZ || f > HIGH_HZ) continue;
      if (magnitudes[k] > peakMag) {
        peakMag = magnitudes[k];
        bestK = k;
      }
    }

    const freqHz = bestK >= 0 ? frequencies[bestK] : 0;
    const snr = noiseFloor > 0 ? peakMag / noiseFloor : 0;
    const detected = snr >= SNR_THRESHOLD && freqHz >= LOW_HZ && freqHz <= HIGH_HZ;

    latest = {
      detected,
      freqHz: detected ? freqHz : 0,
      snr,
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
