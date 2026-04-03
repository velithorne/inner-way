/**
 * Phase 1 acoustic processing (pure functions; callable from JS thread).
 * Blueprint mandates a worker for production FFT load — RN Worker wiring can follow.
 */
import { deconvolveToImpulseResponse, fftWithSampleRate, findPeaks, nextPowerOfTwo } from '../dsp/fft';
import type { RoomGeometry } from '../types/decode';

const C_SOUND = 343;

function trimEnergy(x: Float32Array, maxLen: number): Float32Array {
  if (x.length <= maxLen) return x;
  return x.slice(0, maxLen);
}

function analyzeModalDimensions(
  ir: Float32Array,
  sampleRate: number
): { dims: [number, number, number]; freqs: number[]; isOpen: boolean } {
  const maxIrSamples = Math.min(ir.length, Math.floor(sampleRate * 0.4));
  const irSeg = ir.slice(0, maxIrSamples);
  const { frequencies, magnitudes } = fftWithSampleRate(irSeg, sampleRate);
  const minProm = Math.max(1e-6, Math.max(...magnitudes) * 0.08);
  const peaks = findPeaks(magnitudes, minProm)
    .map((p) => ({
      hz: frequencies[p.binIndex] ?? 0,
      mag: magnitudes[p.binIndex] ?? 0,
    }))
    .filter((p) => p.hz >= 18 && p.hz <= 400);

  peaks.sort((a, b) => b.mag - a.mag);

  const modalFreqs = peaks.slice(0, 8).map((p) => p.hz);
  const topThree = peaks.slice(0, 3).filter((p) => p.hz >= 25);

  if (topThree.length < 2 || peaks.length < 3) {
    return { dims: [4, 2.5, 4], freqs: modalFreqs, isOpen: true };
  }

  const lengths = topThree.map((p) => C_SOUND / (2 * Math.max(p.hz, 8)));
  lengths.sort((a, b) => b - a);
  const [L1, L2, L3] = [lengths[0]!, lengths[1]!, lengths[2]!];
  const clamp = (v: number) => Math.min(20, Math.max(1.5, v));
  return {
    dims: [clamp(L1), clamp(L2), clamp(L3)],
    freqs: modalFreqs,
    isOpen: false,
  };
}

function irConfidence(ir: Float32Array, sampleRate: number): number {
  const n = Math.min(ir.length, Math.floor(sampleRate * 0.05));
  let eEarly = 0;
  let eLate = 0;
  for (let i = 0; i < n; i++) eEarly += ir[i]! * ir[i]!;
  const start = Math.floor(sampleRate * 0.05);
  const end = Math.min(ir.length, start + n);
  for (let i = start; i < end; i++) eLate += ir[i]! * ir[i]!;
  const ratio = eLate > 1e-12 ? eEarly / (eLate + 1e-12) : 0;
  const c = 35 + 45 * Math.min(1, Math.log1p(ratio) / Math.log(10));
  return Math.min(95, Math.max(15, c));
}

export function processSweepAndRecorded(
  sweep: Float32Array,
  recorded: Float32Array,
  sampleRate: number
): RoomGeometry {
  const targetN = nextPowerOfTwo(Math.max(sweep.length, recorded.length));
  const sPad = new Float32Array(targetN);
  const rPad = new Float32Array(targetN);
  sPad.set(sweep);
  rPad.set(recorded);

  const { irTime } = deconvolveToImpulseResponse(sPad, rPad);
  const ir = trimEnergy(irTime, Math.floor(sampleRate * 0.5));

  const { dims, freqs, isOpen } = analyzeModalDimensions(ir, sampleRate);
  const conf = isOpen ? Math.min(55, irConfidence(ir, sampleRate)) : irConfidence(ir, sampleRate);

  console.log('DECODE_ACOUSTIC:', {
    width: dims[0],
    height: dims[1],
    depth: dims[2],
    confidence: Math.round(conf),
    isOpen,
  });

  return {
    width: dims[0],
    height: dims[1],
    depth: dims[2],
    confidence: conf,
    isOpen,
    resonanceFreqs: freqs,
    impulseResponse: ir,
  };
}
