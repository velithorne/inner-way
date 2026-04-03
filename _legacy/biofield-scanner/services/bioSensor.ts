import NetInfo, { type NetInfoState } from '@react-native-community/netinfo';
import type { AccelerometerCardiacHandle } from './accelerometerCardiac';
import type { MagnetometerCardiacHandle } from './magnetometerCardiac';
import { useBioStore } from '../store/useBioStore';
import type { ScanMode } from '../store/useBioStore';

const FUSION_MS = 1000;
const COHERENCE_HZ = 0.1;
const STABILITY_WINDOW_MS = 30_000;
const STABILITY_TOL_HZ = 0.05;

const SUSTAINED_MS = 30_000;
const RHYTHM_STABLE_MS = 60_000;
const BPM_STABILITY_TOL = 5;

type FusionHandle = {
  stop: () => void;
};

/**
 * Combines accelerometer + magnetometer services with optional WiFi RSSI baseline.
 * Runs fusion every 1s and updates useBioStore.
 */
export function startBioSensorFusion(
  acc: AccelerometerCardiacHandle,
  mag: MagnetometerCardiacHandle,
  options?: {
    baselineRssi?: number | null;
    scanMode?: ScanMode;
  }
): FusionHandle {
  let baselineRssi = options?.baselineRssi ?? null;
  const freqHistory: { t: number; hz: number }[] = [];
  const fusionStartMs = Date.now();

  /** BPM stability for "stable cardiac rhythm" bonus */
  let stableBpmRef: number | null = null;
  let stableBpmSinceMs: number | null = null;

  const unsubNet = NetInfo.addEventListener((state: NetInfoState) => {
    if (typeof state.details === 'object' && state.details !== null && 'strength' in state.details) {
      const s = state.details as { strength?: number };
      if (typeof s.strength === 'number' && baselineRssi === null) {
        baselineRssi = s.strength;
      }
    }
  });

  const interval = setInterval(async () => {
    const accOut = acc.getLatest();
    const magOut = mag.getLatest();
    const now = Date.now();
    const scanElapsedMs = now - fusionStartMs;

    const accScore =
      accOut.isControlPhase || accOut.bpm <= 0
        ? 0
        : Math.min(40, (accOut.confidence / 100) * 40);
    const magScore = magOut.detected
      ? Math.min(35, (Math.min(magOut.snr, 20) / 20) * 35)
      : 0;

    let coherence = false;
    if (
      accOut.freqHz > 0.4 &&
      magOut.freqHz > 0.4 &&
      Math.abs(accOut.freqHz - magOut.freqHz) < COHERENCE_HZ
    ) {
      coherence = true;
    }
    const coherenceBonus = coherence ? 15 : 0;

    let rfPoints = 0;
    try {
      const state = await NetInfo.fetch();
      if (
        typeof state.details === 'object' &&
        state.details !== null &&
        'strength' in state.details
      ) {
        const str = (state.details as { strength?: number }).strength;
        if (typeof str === 'number' && baselineRssi !== null) {
          const drop = baselineRssi - str;
          rfPoints = Math.min(15, Math.max(0, drop * 1.5));
        }
      }
    } catch {
      /* ignore */
    }

    const domHz =
      accOut.freqHz > 0 ? accOut.freqHz : magOut.freqHz > 0 ? magOut.freqHz : 0;
    if (domHz > 0) {
      freqHistory.push({ t: now, hz: domHz });
    }
    while (freqHistory.length && now - freqHistory[0].t > STABILITY_WINDOW_MS) {
      freqHistory.shift();
    }

    let sustainedSignalBonus = 0;
    if (
      !accOut.isControlPhase &&
      scanElapsedMs > SUSTAINED_MS &&
      accOut.bpm >= 45 &&
      accOut.bpm <= 180 &&
      accOut.confidence > 35
    ) {
      sustainedSignalBonus = 8;
    }

    let stableRhythmBonus = 0;
    const bpm = accOut.bpm;
    if (!accOut.isControlPhase && bpm >= 45 && bpm <= 180) {
      if (stableBpmRef === null) {
        stableBpmRef = bpm;
        stableBpmSinceMs = now;
      } else if (Math.abs(bpm - stableBpmRef) <= BPM_STABILITY_TOL) {
        if (stableBpmSinceMs !== null && now - stableBpmSinceMs > RHYTHM_STABLE_MS) {
          stableRhythmBonus = 12;
        }
      } else {
        stableBpmRef = bpm;
        stableBpmSinceMs = now;
      }
    } else {
      stableBpmRef = null;
      stableBpmSinceMs = null;
    }

    let sum =
      accScore +
      magScore +
      coherenceBonus +
      rfPoints +
      sustainedSignalBonus +
      stableRhythmBonus;
    sum = Math.max(0, Math.min(100, sum));

    useBioStore.getState().setFusion({
      bps: sum,
      accBpm: accOut.bpm,
      accConfidence: accOut.confidence,
      magFreqHz: magOut.freqHz,
      magSnr: magOut.snr,
      coherenceDetected: coherence,
      rfScoreComponent: rfPoints,
      stabilityBonusActive: stableRhythmBonus > 0,
      accSettling: accOut.isWarmup,
      accControlPhase: accOut.isControlPhase,
      accPossibleInterference: accOut.possibleInterference,
      sustainedSignalBonus,
      stableRhythmBonus,
      lastUpdateMs: now,
      scanMode: options?.scanMode ?? useBioStore.getState().scanMode,
    });
  }, FUSION_MS);

  return {
    stop: () => {
      clearInterval(interval);
      unsubNet();
    },
  };
}
