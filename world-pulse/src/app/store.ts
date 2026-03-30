import { create } from 'zustand';
import type { PulseEvent, EmotionKey, MockEngineSettings } from '../types';

interface WorldPulseState {
  pulses: PulseEvent[];
  selectedEmotion: EmotionKey | null;
  mockEngineSettings: MockEngineSettings;
  showDebug: boolean;
  glowAccumulation: number;

  addPulse: (pulse: PulseEvent) => void;
  removePulse: (id: string) => void;
  setSelectedEmotion: (emotion: EmotionKey | null) => void;
  updateMockSettings: (settings: Partial<MockEngineSettings>) => void;
  toggleDebug: () => void;
  setGlowAccumulation: (value: number) => void;
  cleanExpiredPulses: () => void;
}

export const useWorldPulseStore = create<WorldPulseState>((set, get) => ({
  pulses: [],
  selectedEmotion: null,
  showDebug: false,
  glowAccumulation: 0,

  mockEngineSettings: {
    enabled: true,
    intervalMs: 1800,
    burstProbability: 0.12,
    burstSize: 4,
    regionBias: 'global',
  },

  addPulse: (pulse) =>
    set((state) => ({
      pulses: [...state.pulses, pulse],
    })),

  removePulse: (id) =>
    set((state) => ({
      pulses: state.pulses.filter((p) => p.id !== id),
    })),

  setSelectedEmotion: (emotion) => set({ selectedEmotion: emotion }),

  updateMockSettings: (settings) =>
    set((state) => ({
      mockEngineSettings: { ...state.mockEngineSettings, ...settings },
    })),

  toggleDebug: () => set((state) => ({ showDebug: !state.showDebug })),

  setGlowAccumulation: (value) => set({ glowAccumulation: value }),

  cleanExpiredPulses: () => {
    const now = Date.now();
    const { pulses } = get();
    const active = pulses.filter((p) => now - p.startTime < p.duration + 500);
    if (active.length !== pulses.length) {
      set({ pulses: active });
    }
  },
}));
