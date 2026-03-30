import { useWorldPulseStore } from '../../app/store';
import { PulseMarker } from './PulseMarker';

interface PulseSystemProps {
  globeRadius?: number;
}

export function PulseSystem({ globeRadius = 1 }: PulseSystemProps) {
  const pulses = useWorldPulseStore((s) => s.pulses);

  return (
    <group>
      {pulses.map((pulse) => (
        <PulseMarker key={pulse.id} pulse={pulse} globeRadius={globeRadius} />
      ))}
    </group>
  );
}
