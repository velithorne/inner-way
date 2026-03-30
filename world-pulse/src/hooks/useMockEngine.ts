import { useEffect, useRef } from 'react';
import { useWorldPulseStore } from '../app/store';
import { createMockPulse, createBurst } from '../data/mockPulses';

export function useMockEngine() {
  const { mockEngineSettings, addPulse, cleanExpiredPulses } = useWorldPulseStore();
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const cleanRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    if (timerRef.current) clearInterval(timerRef.current);

    if (!mockEngineSettings.enabled) return;

    timerRef.current = setInterval(() => {
      const isBurst = Math.random() < mockEngineSettings.burstProbability;

      if (isBurst) {
        const pulses = createBurst(mockEngineSettings.burstSize, mockEngineSettings.regionBias);
        pulses.forEach(addPulse);
      } else {
        addPulse(createMockPulse(mockEngineSettings.regionBias));
      }
    }, mockEngineSettings.intervalMs);

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [
    mockEngineSettings.enabled,
    mockEngineSettings.intervalMs,
    mockEngineSettings.burstProbability,
    mockEngineSettings.burstSize,
    mockEngineSettings.regionBias,
    addPulse,
  ]);

  useEffect(() => {
    cleanRef.current = setInterval(() => {
      cleanExpiredPulses();
    }, 2000);

    return () => {
      if (cleanRef.current) clearInterval(cleanRef.current);
    };
  }, [cleanExpiredPulses]);
}
