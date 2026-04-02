import type { PulseEvent, EmotionKey, RegionBias } from '../types';
import { EMOTIONS } from './emotions';
import { DEMO_CITIES, REGIONS } from './demoLocations';

let _idCounter = 0;
function nextId(): string {
  return `pulse-${Date.now()}-${_idCounter++}`;
}

function randomBetween(min: number, max: number): number {
  return Math.random() * (max - min) + min;
}

function randomEmotion(): EmotionKey {
  return EMOTIONS[Math.floor(Math.random() * EMOTIONS.length)].key;
}

function randomGlobalCoord(): { lat: number; lon: number } {
  // Bias toward inhabited latitudes (-60 to 75)
  const lat = randomBetween(-60, 75);
  const lon = randomBetween(-180, 180);
  return { lat, lon };
}

function randomUrbanCoord(): { lat: number; lon: number } {
  // Pick a random demo city and add noise
  const city = DEMO_CITIES[Math.floor(Math.random() * DEMO_CITIES.length)];
  return {
    lat: city.lat + randomBetween(-8, 8),
    lon: city.lon + randomBetween(-8, 8),
  };
}

function randomRegionCoord(region: keyof typeof REGIONS): { lat: number; lon: number } {
  const r = REGIONS[region];
  return {
    lat: randomBetween(r.lat[0], r.lat[1]),
    lon: randomBetween(r.lon[0], r.lon[1]),
  };
}

export function createMockPulse(bias: RegionBias = 'global'): PulseEvent {
  let coord: { lat: number; lon: number };

  switch (bias) {
    case 'urban':
      coord = randomUrbanCoord();
      break;
    case 'asia':
      coord = randomRegionCoord('asia');
      break;
    case 'europe':
      coord = randomRegionCoord('europe');
      break;
    case 'americas':
      coord = randomRegionCoord('americas');
      break;
    default:
      coord = randomGlobalCoord();
  }

  return {
    id: nextId(),
    lat: coord.lat,
    lon: coord.lon,
    emotion: randomEmotion(),
    startTime: Date.now(),
    duration: randomBetween(3500, 7000),
    intensity: randomBetween(0.5, 1.0),
  };
}

export function createCityPulse(cityName: string, emotion?: EmotionKey): PulseEvent | null {
  const city = DEMO_CITIES.find((c) => c.name === cityName);
  if (!city) return null;
  return {
    id: nextId(),
    lat: city.lat,
    lon: city.lon,
    emotion: emotion ?? randomEmotion(),
    startTime: Date.now(),
    duration: randomBetween(4000, 8000),
    intensity: 1.0,
  };
}

export function createBurst(count: number, bias: RegionBias = 'global'): PulseEvent[] {
  return Array.from({ length: count }, () => createMockPulse(bias));
}

export function createUserPulse(emotion: EmotionKey): PulseEvent {
  const coord = randomUrbanCoord();
  return {
    id: nextId(),
    lat: coord.lat,
    lon: coord.lon,
    emotion,
    startTime: Date.now(),
    duration: 8000,
    intensity: 1.0,
  };
}
