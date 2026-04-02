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
      return {
        ...s,
        ...partial,
        bpsLabel: labelFromBps(bps),
      };
    }),
  reset: () => set(initial),
}));
