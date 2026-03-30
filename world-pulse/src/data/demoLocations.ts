import type { DemoCity } from '../types';

export const DEMO_CITIES: DemoCity[] = [
  { name: 'Tokyo', lat: 35.6762, lon: 139.6503 },
  { name: 'New York', lat: 40.7128, lon: -74.006 },
  { name: 'London', lat: 51.5074, lon: -0.1278 },
  { name: 'Sydney', lat: -33.8688, lon: 151.2093 },
  { name: 'São Paulo', lat: -23.5505, lon: -46.6333 },
  { name: 'Mumbai', lat: 19.076, lon: 72.8777 },
  { name: 'Cairo', lat: 30.0444, lon: 31.2357 },
  { name: 'Lagos', lat: 6.5244, lon: 3.3792 },
  { name: 'Seoul', lat: 37.5665, lon: 126.978 },
  { name: 'Mexico City', lat: 19.4326, lon: -99.1332 },
  { name: 'Berlin', lat: 52.52, lon: 13.405 },
  { name: 'Jakarta', lat: -6.2088, lon: 106.8456 },
];

// Regional coordinate bounds for biased spawning
export const REGIONS = {
  asia: {
    lat: [-10, 55],
    lon: [60, 150],
  },
  europe: {
    lat: [35, 70],
    lon: [-10, 40],
  },
  americas: {
    lat: [-55, 55],
    lon: [-125, -35],
  },
  africa: {
    lat: [-35, 37],
    lon: [-18, 52],
  },
  oceania: {
    lat: [-50, -10],
    lon: [110, 180],
  },
};
