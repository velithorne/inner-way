import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Location from 'expo-location';

const ANOMALY_LOG_KEY = '@resonance_map_anomaly_log';
const MAX_LOG_ENTRIES = 500;

export interface AnomalyEntry {
  id: string;
  timestamp: string;           // ISO 8601
  coordinates: {
    lat: number | null;
    lng: number | null;
  };
  magnitude: number;
  delta: number;
  x: number;
  y: number;
  z: number;
  heading: number;
  // Phase 3 server sync hook
  synced?: boolean;
}

let locationPermissionGranted: boolean | null = null;

async function requestLocationIfNeeded(): Promise<void> {
  if (locationPermissionGranted !== null) return;
  try {
    const { status } = await Location.requestForegroundPermissionsAsync();
    locationPermissionGranted = status === 'granted';
  } catch {
    locationPermissionGranted = false;
  }
}

async function getCurrentCoordinates(): Promise<{ lat: number | null; lng: number | null }> {
  if (!locationPermissionGranted) {
    return { lat: null, lng: null };
  }
  try {
    const location = await Location.getCurrentPositionAsync({
      accuracy: Location.Accuracy.Balanced,
    });
    return {
      lat: location.coords.latitude,
      lng: location.coords.longitude,
    };
  } catch {
    return { lat: null, lng: null };
  }
}

export async function loadAnomalyLog(): Promise<AnomalyEntry[]> {
  try {
    const raw = await AsyncStorage.getItem(ANOMALY_LOG_KEY);
    if (raw) return JSON.parse(raw);
  } catch {}
  return [];
}

async function saveAnomalyLog(entries: AnomalyEntry[]): Promise<void> {
  await AsyncStorage.setItem(ANOMALY_LOG_KEY, JSON.stringify(entries));
}

export async function logAnomaly(params: {
  magnitude: number;
  delta: number;
  x: number;
  y: number;
  z: number;
  heading: number;
}): Promise<AnomalyEntry> {
  // Request location permission on first anomaly
  await requestLocationIfNeeded();
  const coordinates = await getCurrentCoordinates();

  const entry: AnomalyEntry = {
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    timestamp: new Date().toISOString(),
    coordinates,
    magnitude: params.magnitude,
    delta: params.delta,
    x: params.x,
    y: params.y,
    z: params.z,
    heading: params.heading,
    synced: false,
    // Phase 3: server sync will set synced = true once uploaded
  };

  const existing = await loadAnomalyLog();
  const updated = [entry, ...existing].slice(0, MAX_LOG_ENTRIES);
  await saveAnomalyLog(updated);

  return entry;
}

export async function clearAnomalyLog(): Promise<void> {
  await AsyncStorage.removeItem(ANOMALY_LOG_KEY);
}

// Phase 3 integration hook — server sync stub
export async function syncAnomalyLog(): Promise<void> {
  // TODO Phase 3: POST unsynced entries to server, mark as synced
  // const unsyncedEntries = (await loadAnomalyLog()).filter(e => !e.synced);
  // await fetch(PHASE3_SYNC_ENDPOINT, { method: 'POST', body: JSON.stringify(unsyncedEntries) });
}
