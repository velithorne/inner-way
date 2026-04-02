import { useEffect } from 'react';
import { useWorldPulseStore } from '../app/store';

/**
 * Tracks how many active pulses exist and derives a global glow accumulation value.
 * This drives the subtle brightening of the Earth when pulse density is high.
 */
export function useGlowAccumulation() {
  const { pulses, setGlowAccumulation } = useWorldPulseStore();

  useEffect(() => {
    const target = Math.min(pulses.length / 20, 1.0);
    setGlowAccumulation(target);
  }, [pulses.length, setGlowAccumulation]);
}
