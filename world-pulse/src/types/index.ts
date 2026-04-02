export type EmotionKey =
  | 'heavy'
  | 'electric'
  | 'hollow'
  | 'tender'
  | 'anxious'
  | 'calm'
  | 'raw'
  | 'wonder'
  | 'numb'
  | 'alive';

export interface EmotionDefinition {
  key: EmotionKey;
  label: string;
  color: string;
  glowStrength: number;
  soundCategory?: string;
}

export interface GeoPoint {
  lat: number;
  lon: number;
}

export interface PulseEvent {
  id: string;
  lat: number;
  lon: number;
  emotion: EmotionKey;
  startTime: number;
  duration: number;
  intensity: number;
}

export interface DemoCity {
  name: string;
  lat: number;
  lon: number;
}

export interface MockEngineSettings {
  enabled: boolean;
  intervalMs: number;
  burstProbability: number;
  burstSize: number;
  regionBias: RegionBias;
}

export type RegionBias = 'global' | 'urban' | 'asia' | 'europe' | 'americas';
