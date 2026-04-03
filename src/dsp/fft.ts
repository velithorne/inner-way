export type FrequencyPeak = {
  binIndex: number;
  frequencyHz: number;
  magnitude: number;
};

/**
 * In-place Cooley-Tukey radix-2 FFT. Length must be power of 2.
 */
export function fftComplex(re: Float32Array, im: Float32Array): void {
  const n = re.length;
  if ((n & (n - 1)) !== 0) throw new Error('FFT length must be a power of 2');

  let j = 0;
  for (let i = 1; i < n; i++) {
    let bit = n >> 1;
    while (j & bit) {
      j ^= bit;
      bit >>= 1;
    }
    j ^= bit;
    if (i < j) {
      [re[i], re[j]] = [re[j], re[i]];
      [im[i], im[j]] = [im[j], im[i]];
    }
  }

  for (let len = 2; len <= n; len <<= 1) {
    const half = len >> 1;
    const angle = (-2 * Math.PI) / len;
    for (let i = 0; i < n; i += len) {
      for (let k = 0; k < half; k++) {
        const theta = angle * k;
        const wr = Math.cos(theta);
        const wi = Math.sin(theta);
        const uRe = re[i + k];
        const uIm = im[i + k];
        const vRe = re[i + k + half];
        const vIm = im[i + k + half];
        const tRe = wr * vRe - wi * vIm;
        const tIm = wr * vIm + wi * vRe;
        re[i + k] = uRe + tRe;
        im[i + k] = uIm + tIm;
        re[i + k + half] = uRe - tRe;
        im[i + k + half] = uIm - tIm;
      }
    }
  }
}

export function fft(signal: Float32Array): { frequencies: Float32Array; magnitudes: Float32Array } {
  let n = 1;
  while (n < signal.length) n <<= 1;
  const re = new Float32Array(n);
  const im = new Float32Array(n);
  re.set(signal);
  fftComplex(re, im);
  const magnitudes = new Float32Array(n / 2);
  const frequencies = new Float32Array(n / 2);
  const sampleRateImplicit = 1;
  const df = sampleRateImplicit / n;
  for (let k = 0; k < n / 2; k++) {
    magnitudes[k] = Math.hypot(re[k], im[k]) * (k === 0 || k === n / 2 - 1 ? 1 : 2) / n;
    frequencies[k] = k * df;
  }
  return { frequencies, magnitudes };
}

/** FFT with explicit sample rate for frequency axis (uses zero-pad to next power of 2). */
export function fftWithSampleRate(
  signal: Float32Array,
  sampleRate: number
): { frequencies: Float32Array; magnitudes: Float32Array } {
  let n = 1;
  while (n < signal.length) n <<= 1;
  const re = new Float32Array(n);
  const im = new Float32Array(n);
  re.set(signal);
  fftComplex(re, im);
  const half = n / 2;
  const magnitudes = new Float32Array(half);
  const frequencies = new Float32Array(half);
  const scale = 2 / n;
  for (let k = 0; k < half; k++) {
    const mag = Math.hypot(re[k], im[k]) * scale * (k === 0 ? 0.5 : 1);
    magnitudes[k] = mag;
    frequencies[k] = (k * sampleRate) / n;
  }
  return { frequencies, magnitudes };
}

export function findPeaks(
  magnitudes: Float32Array,
  minProminence: number
): FrequencyPeak[] {
  const peaks: FrequencyPeak[] = [];
  for (let i = 1; i < magnitudes.length - 1; i++) {
    const m = magnitudes[i];
    if (m < magnitudes[i - 1] || m < magnitudes[i + 1]) continue;
    const leftMin = Math.min(magnitudes[i - 1], magnitudes[Math.max(0, i - 2)]);
    const rightMin = Math.min(magnitudes[i + 1], magnitudes[Math.min(magnitudes.length - 1, i + 2)]);
    const prominence = m - Math.max(leftMin, rightMin);
    if (prominence >= minProminence) {
      peaks.push({ binIndex: i, frequencyHz: 0, magnitude: m });
    }
  }
  return peaks;
}

export function peakFrequencyHz(peaks: FrequencyPeak[], sampleRate: number, fftSize: number): number {
  if (peaks.length === 0) return 0;
  let best = peaks[0];
  for (const p of peaks) {
    if (p.magnitude > best.magnitude) best = p;
  }
  return (best.binIndex * sampleRate) / fftSize;
}

/** Next power of two >= n */
export function nextPowerOfTwo(n: number): number {
  let p = 1;
  while (p < n) p <<= 1;
  return p;
}

/**
 * Inverse FFT in-place (un-normalized forward FFT → scaled inverse).
 * After call, `re` + i`im` is the time-domain signal (real output if input was Hermitian-symmetric).
 */
export function ifftComplex(re: Float32Array, im: Float32Array): void {
  const n = re.length;
  for (let i = 0; i < n; i++) im[i] = -im[i];
  fftComplex(re, im);
  const inv = 1 / n;
  for (let i = 0; i < n; i++) {
    re[i] *= inv;
    im[i] *= -inv;
  }
}

/**
 * Frequency-domain deconvolution: IR ≈ IFFT( FFT(recorded) / (FFT(sweep) + eps) ).
 * Arrays must be the same length (power of 2).
 */
export function deconvolveToImpulseResponse(
  sweep: Float32Array,
  recorded: Float32Array,
  eps = 1e-6
): { irTime: Float32Array; irImag: Float32Array } {
  const n = sweep.length;
  const reX = new Float32Array(n);
  const imX = new Float32Array(n);
  const reY = new Float32Array(n);
  const imY = new Float32Array(n);
  reX.set(sweep);
  reY.set(recorded);
  fftComplex(reX, imX);
  fftComplex(reY, imY);
  const reH = new Float32Array(n);
  const imH = new Float32Array(n);
  for (let k = 0; k < n; k++) {
    const denom = reX[k] * reX[k] + imX[k] * imX[k] + eps;
    const numRe = reY[k] * reX[k] + imY[k] * imX[k];
    const numIm = imY[k] * reX[k] - reY[k] * imX[k];
    reH[k] = numRe / denom;
    imH[k] = numIm / denom;
  }
  const reOut = new Float32Array(reH);
  const imOut = new Float32Array(imH);
  ifftComplex(reOut, imOut);
  return { irTime: reOut, irImag: imOut };
}
