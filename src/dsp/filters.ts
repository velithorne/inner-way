/**
 * Digital filters for the Biofield Scanner signal pipeline.
 * Bandpass implemented as cascaded 2nd-order Butterworth sections (bilinear transform).
 */

function biquadProcess(
  b: [number, number, number],
  a: [number, number, number],
  input: Float32Array
): Float32Array {
  const out = new Float32Array(input.length);
  let x1 = 0;
  let x2 = 0;
  let y1 = 0;
  let y2 = 0;
  const b0 = b[0];
  const b1 = b[1];
  const b2 = b[2];
  const a0 = a[0];
  const a1 = a[1];
  const a2 = a[2];
  for (let i = 0; i < input.length; i++) {
    const x = input[i];
    const y = (b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2) / a0;
    x2 = x1;
    x1 = x;
    y2 = y1;
    y1 = y;
    out[i] = y;
  }
  return out;
}

function butterworthLowPass2nd(fc: number, sampleRate: number): Float32Array {
  const w0 = (2 * Math.PI * fc) / sampleRate;
  const cosw0 = Math.cos(w0);
  const sinw0 = Math.sin(w0);
  const Q = 1 / Math.sqrt(2);
  const alpha = sinw0 / (2 * Q);
  const b0 = (1 - cosw0) / 2;
  const b1 = 1 - cosw0;
  const b2 = (1 - cosw0) / 2;
  const a0 = 1 + alpha;
  const a1 = -2 * cosw0;
  const a2 = 1 - alpha;
  return new Float32Array([b0 / a0, b1 / a0, b2 / a0, 1, a1 / a0, a2 / a0]);
}

function butterworthHighPass2nd(fc: number, sampleRate: number): Float32Array {
  const w0 = (2 * Math.PI * fc) / sampleRate;
  const cosw0 = Math.cos(w0);
  const sinw0 = Math.sin(w0);
  const Q = 1 / Math.sqrt(2);
  const alpha = sinw0 / (2 * Q);
  const b0 = (1 + cosw0) / 2;
  const b1 = -(1 + cosw0);
  const b2 = (1 + cosw0) / 2;
  const a0 = 1 + alpha;
  const a1 = -2 * cosw0;
  const a2 = 1 - alpha;
  return new Float32Array([b0 / a0, b1 / a0, b2 / a0, 1, a1 / a0, a2 / a0]);
}

function applySos(coeffs: Float32Array, signal: Float32Array): Float32Array {
  const b: [number, number, number] = [coeffs[0], coeffs[1], coeffs[2]];
  const a: [number, number, number] = [coeffs[3], coeffs[4], coeffs[5]];
  return biquadProcess(b, a, signal);
}

/**
 * 4th-order bandpass: cascaded HP(lowHz) → LP(highHz) → HP → LP
 * Approximates a Butterworth bandpass for cardiac band isolation.
 */
export function butterworth4thOrderBandpass(
  signal: Float32Array,
  lowHz: number,
  highHz: number,
  sampleRate: number
): Float32Array {
  if (signal.length === 0) return signal;
  const hp1 = butterworthHighPass2nd(lowHz, sampleRate);
  const lp1 = butterworthLowPass2nd(highHz, sampleRate);
  let x = applySos(hp1, signal);
  x = applySos(lp1, x);
  x = applySos(hp1, x);
  x = applySos(lp1, x);
  return x;
}

export function rollingMean(buffer: Float32Array, windowSize: number): Float32Array {
  if (windowSize <= 0 || buffer.length === 0) return new Float32Array(buffer.length);
  const out = new Float32Array(buffer.length);
  let sum = 0;
  for (let i = 0; i < buffer.length; i++) {
    sum += buffer[i];
    if (i >= windowSize) sum -= buffer[i - windowSize];
    if (i >= windowSize - 1) {
      out[i] = sum / windowSize;
    } else {
      out[i] = sum / (i + 1);
    }
  }
  return out;
}

export function rollingStdDev(buffer: Float32Array, windowSize: number): Float32Array {
  if (windowSize <= 1 || buffer.length === 0) return new Float32Array(buffer.length);
  const out = new Float32Array(buffer.length);
  const win = Math.min(windowSize, buffer.length);
  for (let i = 0; i < buffer.length; i++) {
    const start = Math.max(0, i - win + 1);
    const n = i - start + 1;
    let mean = 0;
    for (let j = start; j <= i; j++) mean += buffer[j];
    mean /= n;
    let v = 0;
    for (let j = start; j <= i; j++) {
      const d = buffer[j] - mean;
      v += d * d;
    }
    out[i] = Math.sqrt(v / n);
  }
  return out;
}

export function baselineSubtract(signal: Float32Array, baseline: number): Float32Array {
  const out = new Float32Array(signal.length);
  for (let i = 0; i < signal.length; i++) out[i] = signal[i] - baseline;
  return out;
}

/** Single-pole lowpass for separating ~9.81 gravity from magnitude (cutoff ~0.1 Hz). */
export function onePoleLowPass(
  signal: Float32Array,
  sampleRate: number,
  cutoffHz: number
): Float32Array {
  const out = new Float32Array(signal.length);
  const rc = 1 / (2 * Math.PI * cutoffHz);
  const dt = 1 / sampleRate;
  const alpha = dt / (rc + dt);
  let y = signal[0];
  for (let i = 0; i < signal.length; i++) {
    y = alpha * signal[i] + (1 - alpha) * y;
    out[i] = y;
  }
  return out;
}
