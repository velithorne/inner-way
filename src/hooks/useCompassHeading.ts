import * as Location from 'expo-location';
import { Magnetometer } from 'expo-sensors';
import { useEffect, useState } from 'react';

function headingFromMagnetometer(x: number, y: number): number {
  const rad = Math.atan2(-x, y);
  let deg = (rad * (180 / Math.PI) + 360) % 360;
  return deg;
}

/**
 * Degrees clockwise from north (0–360). Used with rotateZ(-heading) so north stays up.
 */
export function useCompassHeading(enabled: boolean) {
  const [headingDeg, setHeadingDeg] = useState(0);

  useEffect(() => {
    if (!enabled) return;

    let headingSub: Location.LocationSubscription | null = null;
    let magSub: { remove: () => void } | null = null;

    (async () => {
      try {
        const sub = await Location.watchHeadingAsync((head) => {
          const t = head.trueHeading;
          const m = head.magHeading;
          const v = t >= 0 ? t : m >= 0 ? m : 0;
          setHeadingDeg(v);
        });
        headingSub = sub;
      } catch {
        const avail = await Magnetometer.isAvailableAsync();
        if (!avail) return;
        Magnetometer.setUpdateInterval(100);
        magSub = Magnetometer.addListener((meas) => {
          setHeadingDeg(headingFromMagnetometer(meas.x, meas.y));
        });
      }
    })();

    return () => {
      headingSub?.remove();
      magSub?.remove();
    };
  }, [enabled]);

  return headingDeg;
}
