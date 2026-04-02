import { create } from 'zustand';

export type ScanMode = 'PROXIMITY' | 'CONTACT' | 'SWEEP';

export type BpsLabel =
  | 'NO SIGNAL'
  | 'TRACE'
  | 'POSSIBLE'
  | 'PROBABLE'
  | 'DETECTED'
  | 'CONFIRMED';

function labelFromBps(bps: number): BpsLabel {
  if (bps <= 20) return 'NO SIGNAL';
  if (bps <= 40) return 'TRACE';
  if (bps <= 60) return 'POSSIBLE';
  if (bps <= 75) return 'PROBABLE';
  if (bps <= 90) return 'DETECTED';
  return 'CONFIRMED';
}

/**
 * BPS can stay low when only the accelerometer contributes (~40 pts max) and mag is ~0.
 * If we already show a plausible BPM from the acc pipeline, do not label "NO SIGNAL" — that contradicts the readout.
 */
function deriveBpsLabel(
  bps: number,
  accBpm: number,
  accConfidence: number
): BpsLabel {
  const accShowsCardiac =
    accBpm >= 36 &&
    accBpm <= 200 &&
    accConfidence >= 22;
  if (bps <= 20 && accShowsCardiac) {
    return 'TRACE';
  }
  return labelFromBps(bps);
}

export type BioState = {
  bps: number;
  bpsLabel: BpsLabel;
  accBpm: number;
  accConfidence: number;
  magFreqHz: number;
  magSnr: number;
  coherenceDetected: boolean;
  isCalibrated: boolean;
  scanMode: ScanMode;
  rfScoreComponent: number;
  stabilityBonusActive: boolean;
  /** Last fusion tick timestamp */
  lastUpdateMs: number;
  setFusion: (partial: Partial<Omit<BioState, 'setFusion' | 'reset'>>) => void;
  reset: () => void;
};

const initial: Omit<BioState, 'setFusion' | 'reset'> = {
  bps: 0,
  bpsLabel: 'NO SIGNAL',
  accBpm: 0,
  accConfidence: 0,
  magFreqHz: 0,
  magSnr: 0,
  coherenceDetected: false,
  isCalibrated: false,
  scanMode: 'CONTACT',
  rfScoreComponent: 0,
  stabilityBonusActive: false,
  lastUpdateMs: 0,
};

export const useBioStore = create<BioState>((set) => ({
  ...initial,
  setFusion: (partial) =>
    set((s) => {
      const bps = partial.bps ?? s.bps;
      const accBpm = partial.accBpm ?? s.accBpm;
      const accConfidence = partial.accConfidence ?? s.accConfidence;
      return {
        ...s,
        ...partial,
        bpsLabel: deriveBpsLabel(bps, accBpm, accConfidence),
      };
    }),
  reset: () => set(initial),
}));
