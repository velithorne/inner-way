import { create } from 'zustand';
import {
  WAVEFORM_HISTORY_SAMPLES,
  ROLLING_AVERAGE_SAMPLES,
} from '../constants/thresholds';

export interface MagnetometerReading {
  x: number;
  y: number;
  z: number;
  magnitude: number;
  heading: number;
  timestamp: number;
}

export interface FieldState {
  // Live sensor data
  reading: MagnetometerReading;
  isSimulationMode: boolean;

  // Rolling history
  history: number[];            // magnitude history for waveform
  rollingAverage: number;       // 30-second rolling average

  // Anomaly state
  isAnomaly: boolean;
  anomalyDelta: number;         // deviation from baseline (signed)

  // Calibration
  isCalibrated: boolean;
  calibrationOffset: { x: number; y: number; z: number };
  isCalibrating: boolean;

  // Actions
  setReading: (reading: MagnetometerReading) => void;
  setSimulationMode: (sim: boolean) => void;
  pushHistory: (magnitude: number) => void;
  setAnomaly: (isAnomaly: boolean, delta: number) => void;
  setCalibrated: (offset: { x: number; y: number; z: number }) => void;
  setCalibrating: (val: boolean) => void;

  // Phase 2+ integration hooks
  // onSchumannDataReceived?: (data: unknown) => void;
  // onServerSyncRequested?: () => void;
}

const DEFAULT_READING: MagnetometerReading = {
  x: 0,
  y: 0,
  z: 0,
  magnitude: 0,
  heading: 0,
  timestamp: Date.now(),
};

export const useFieldStore = create<FieldState>((set) => ({
  reading: DEFAULT_READING,
  isSimulationMode: false,
  history: [],
  rollingAverage: 0,
  isAnomaly: false,
  anomalyDelta: 0,
  isCalibrated: false,
  calibrationOffset: { x: 0, y: 0, z: 0 },
  isCalibrating: false,

  setReading: (reading) => set({ reading }),

  setSimulationMode: (isSimulationMode) => set({ isSimulationMode }),

  pushHistory: (magnitude) =>
    set((state) => {
      const next = [...state.history, magnitude].slice(-WAVEFORM_HISTORY_SAMPLES);
      const rollingWindow = next.slice(-ROLLING_AVERAGE_SAMPLES);
      const rollingAverage =
        rollingWindow.length > 0
          ? rollingWindow.reduce((a, b) => a + b, 0) / rollingWindow.length
          : magnitude;
      return { history: next, rollingAverage };
    }),

  setAnomaly: (isAnomaly, anomalyDelta) => set({ isAnomaly, anomalyDelta }),

  setCalibrated: (calibrationOffset) =>
    set({ calibrationOffset, isCalibrated: true }),

  setCalibrating: (isCalibrating) => set({ isCalibrating }),
}));
