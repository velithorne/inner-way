import { Magnetometer } from 'expo-sensors';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Haptics from 'expo-haptics';
import { useFieldStore } from '../store/useFieldStore';
import {
  MAGNETOMETER_UPDATE_MS,
  ANOMALY_DEVIATION_PERCENT,
  CALIBRATION_DURATION_MS,
  SIMULATION_BASE_MAGNITUDE,
  SIMULATION_AMPLITUDE,
  SIMULATION_FREQUENCY,
} from '../constants/thresholds';

const CALIBRATION_STORAGE_KEY = '@resonance_map_calibration';

export interface CalibrationOffset {
  x: number;
  y: number;
  z: number;
}

let subscription: ReturnType<typeof Magnetometer.addListener> | null = null;
let simulationInterval: ReturnType<typeof setInterval> | null = null;
let simulationTick = 0;
let lastAnomalyTime = 0;
const ANOMALY_DEBOUNCE_MS = 2000;

// Minimum samples before anomaly detection activates (~5s at 60hz)
const BASELINE_MIN_SAMPLES = 300;
let sampleCount = 0;

export function calculateMagnitude(x: number, y: number, z: number): number {
  return Math.sqrt(x * x + y * y + z * z);
}

export function calculateHeading(x: number, y: number): number {
  let heading = Math.atan2(y, x) * (180 / Math.PI);
  if (heading < 0) heading += 360;
  return heading;
}

function applyCalibration(
  x: number,
  y: number,
  z: number,
  offset: CalibrationOffset
): { x: number; y: number; z: number } {
  return {
    x: x - offset.x,
    y: y - offset.y,
    z: z - offset.z,
  };
}

async function loadCalibration(): Promise<CalibrationOffset> {
  try {
    const stored = await AsyncStorage.getItem(CALIBRATION_STORAGE_KEY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch {}
  return { x: 0, y: 0, z: 0 };
}

export async function saveCalibration(offset: CalibrationOffset): Promise<void> {
  await AsyncStorage.setItem(CALIBRATION_STORAGE_KEY, JSON.stringify(offset));
  useFieldStore.getState().setCalibrated(offset);
}

export async function startCalibration(): Promise<CalibrationOffset> {
  useFieldStore.getState().setCalibrating(true);

  const samples: { x: number; y: number; z: number }[] = [];

  return new Promise((resolve) => {
    Magnetometer.setUpdateInterval(MAGNETOMETER_UPDATE_MS);

    const calSub = Magnetometer.addListener(({ x, y, z }) => {
      samples.push({ x, y, z });
    });

    setTimeout(() => {
      calSub.remove();
      useFieldStore.getState().setCalibrating(false);

      if (samples.length === 0) {
        resolve({ x: 0, y: 0, z: 0 });
        return;
      }

      // Hard-iron offset = midpoint of min/max per axis
      const offset: CalibrationOffset = {
        x: (Math.max(...samples.map((s) => s.x)) + Math.min(...samples.map((s) => s.x))) / 2,
        y: (Math.max(...samples.map((s) => s.y)) + Math.min(...samples.map((s) => s.y))) / 2,
        z: (Math.max(...samples.map((s) => s.z)) + Math.min(...samples.map((s) => s.z))) / 2,
      };

      saveCalibration(offset);
      resolve(offset);
    }, CALIBRATION_DURATION_MS);
  });
}

function processReading(rawX: number, rawY: number, rawZ: number) {
  const { calibrationOffset, rollingAverage, pushHistory, setReading, setAnomaly, setBaselineReady } =
    useFieldStore.getState();

  const { x, y, z } = applyCalibration(rawX, rawY, rawZ, calibrationOffset);
  const magnitude = calculateMagnitude(x, y, z);
  const heading = calculateHeading(x, y);

  setReading({ x, y, z, magnitude, heading, timestamp: Date.now() });
  pushHistory(magnitude);

  sampleCount += 1;

  // Gate anomaly detection until 300 samples (~5s) have been collected
  if (sampleCount < BASELINE_MIN_SAMPLES) {
    setAnomaly(false, 0);
    return;
  }

  // Mark baseline as ready once on the transition sample
  if (sampleCount === BASELINE_MIN_SAMPLES) {
    setBaselineReady(true);
  }

  // Anomaly detection — only after rolling average has enough data
  if (rollingAverage > 0) {
    const delta = magnitude - rollingAverage;
    const deviationRatio = Math.abs(delta) / rollingAverage;
    const isAnomaly = deviationRatio > ANOMALY_DEVIATION_PERCENT;

    const { isAnomaly: prevAnomaly } = useFieldStore.getState();

    if (isAnomaly && !prevAnomaly) {
      const now = Date.now();
      if (now - lastAnomalyTime > ANOMALY_DEBOUNCE_MS) {
        lastAnomalyTime = now;
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy).catch(() => {});
      }
    }

    setAnomaly(isAnomaly, delta);
  }
}

function startSimulation() {
  simulationInterval = setInterval(() => {
    simulationTick += 1;
    const t = simulationTick * (MAGNETOMETER_UPDATE_MS / 1000);
    const base = SIMULATION_BASE_MAGNITUDE;
    const x = base * Math.cos(2 * Math.PI * SIMULATION_FREQUENCY * t) + (Math.random() - 0.5) * 2;
    const y = base * Math.sin(2 * Math.PI * SIMULATION_FREQUENCY * t) + (Math.random() - 0.5) * 2;
    const z = SIMULATION_AMPLITUDE * Math.sin(2 * Math.PI * SIMULATION_FREQUENCY * 0.5 * t);

    // Occasionally spike for anomaly demo
    const spike = simulationTick % 300 < 10 ? 12 : 0;

    processReading(x + spike, y, z);
  }, MAGNETOMETER_UPDATE_MS);
}

export async function startMagnetometer(): Promise<void> {
  if (subscription) return;
  // Fresh start — reset baseline so warmup runs correctly
  sampleCount = 0;
  useFieldStore.getState().setBaselineReady(false);
  useFieldStore.getState().setAnomaly(false, 0);

  const offset = await loadCalibration();
  useFieldStore.getState().setCalibrated(offset);

  const available = await Magnetometer.isAvailableAsync();

  if (!available) {
    useFieldStore.getState().setSimulationMode(true);
    simulationTick = 0;
    startSimulation();
    return;
  }

  useFieldStore.getState().setSimulationMode(false);
  Magnetometer.setUpdateInterval(MAGNETOMETER_UPDATE_MS);

  subscription = Magnetometer.addListener(({ x, y, z }) => {
    processReading(x, y, z);
  });
}

export function stopMagnetometer(): void {
  if (subscription) {
    subscription.remove();
    subscription = null;
  }
  if (simulationInterval) {
    clearInterval(simulationInterval);
    simulationInterval = null;
  }
  // Do NOT reset sampleCount or baseline here — tab switches call stop/start
  // and resetting would restart the 5-second warmup every time.
  // Only startMagnetometer() resets when starting fresh (subscription was null).
}

/** Hard reset — call this only when the app fully backgrounds or user re-calibrates */
export function resetMagnetometerBaseline(): void {
  sampleCount = 0;
  useFieldStore.getState().setBaselineReady(false);
  useFieldStore.getState().setAnomaly(false, 0);
}
