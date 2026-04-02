import NetInfo, { type NetInfoState } from '@react-native-community/netinfo';
import type { AccelerometerCardiacHandle } from './accelerometerCardiac';
import type { MagnetometerCardiacHandle } from './magnetometerCardiac';
import { useBioStore } from '../store/useBioStore';
import type { ScanMode } from '../store/useBioStore';

const FUSION_MS = 1000;
const COHERENCE_HZ = 0.1;
const STABILITY_WINDOW_MS = 30_000;
const STABILITY_TOL_HZ = 0.05;

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

    const accScore = Math.min(40, (accOut.confidence / 100) * 40);
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

    const now = Date.now();
    const domHz =
      accOut.freqHz > 0 ? accOut.freqHz : magOut.freqHz > 0 ? magOut.freqHz : 0;
    if (domHz > 0) {
      freqHistory.push({ t: now, hz: domHz });
    }
    while (freqHistory.length && now - freqHistory[0].t > STABILITY_WINDOW_MS) {
      freqHistory.shift();
    }
    let stabilityBonus = 0;
    if (freqHistory.length >= 25) {
      const mean =
        freqHistory.reduce((a, b) => a + b.hz, 0) / freqHistory.length;
      const stable = freqHistory.every((e) => Math.abs(e.hz - mean) <= STABILITY_TOL_HZ);
      if (stable) stabilityBonus = 10;
    }

    let sum =
      accScore + magScore + coherenceBonus + rfPoints + stabilityBonus;
    sum = Math.max(0, Math.min(100, sum));

    useBioStore.getState().setFusion({
      bps: sum,
      accBpm: accOut.bpm,
      accConfidence: accOut.confidence,
      magFreqHz: magOut.freqHz,
      magSnr: magOut.snr,
      coherenceDetected: coherence,
      rfScoreComponent: rfPoints,
      stabilityBonusActive: stabilityBonus > 0,
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
