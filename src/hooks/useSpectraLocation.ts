import * as Location from 'expo-location';
import { useEffect, useState } from 'react';

export type LocState = {
  lat: number | null;
  lng: number | null;
  accuracy: number | null;
};

export function useSpectraLocation(enabled: boolean) {
  const [loc, setLoc] = useState<LocState>({
    lat: null,
    lng: null,
    accuracy: null,
  });

  useEffect(() => {
    if (!enabled) return;

    let sub: Location.LocationSubscription | null = null;

    (async () => {
      sub = await Location.watchPositionAsync(
        {
          accuracy: Location.Accuracy.Highest,
          timeInterval: 1000,
          distanceInterval: 1,
        },
        (pos) => {
          setLoc({
            lat: pos.coords.latitude,
            lng: pos.coords.longitude,
            accuracy: pos.coords.accuracy ?? null,
          });
        },
      );
    })();

    return () => {
      sub?.remove();
    };
  }, [enabled]);

  return loc;
}
