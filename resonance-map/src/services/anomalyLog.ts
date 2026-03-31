import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Location from 'expo-location';
import { getProximityLinksForAnomaly, haversineKm } from '../constants/sacredSites';

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
  // Phase 3 server sync
  synced?: boolean;
  // Node verification
  verified?: boolean;
  verifiedAt?: string;
  verificationMagnitude?: number;
  // Proximity intelligence
  proximityLinks?: string[];   // sacred site IDs within 50km
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
  if (!locationPermissionGranted) return { lat: null, lng: null };
  try {
    const location = await Location.getCurrentPositionAsync({
      accuracy: Location.Accuracy.Balanced,
    });
    return { lat: location.coords.latitude, lng: location.coords.longitude };
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
  await requestLocationIfNeeded();
  const coordinates = await getCurrentCoordinates();

  // Calculate proximity links at log time
  const proximityLinks: string[] = [];
  if (coordinates.lat !== null && coordinates.lng !== null) {
    const links = getProximityLinksForAnomaly(coordinates.lat, coordinates.lng);
    links.forEach((l) => proximityLinks.push(l.site.id));
  }

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
    verified: false,
    proximityLinks,
    // Phase 3: server sync will set synced = true once uploaded
  };

  const existing = await loadAnomalyLog();
  const updated = [entry, ...existing].slice(0, MAX_LOG_ENTRIES);
  await saveAnomalyLog(updated);

  return entry;
}

const VERIFY_RADIUS_M = 200;

export type VerifyResult =
  | { success: true; entry: AnomalyEntry }
  | { success: false; reason: 'too_far'; distanceM: number; targetCoords: { lat: number; lng: number } }
  | { success: false; reason: 'no_gps' }
  | { success: false; reason: 'not_found' };

export async function verifyNode(
  entryId: string,
  currentMagnitude: number
): Promise<VerifyResult> {
  const entries = await loadAnomalyLog();
  const idx = entries.findIndex((e) => e.id === entryId);
  if (idx === -1) return { success: false, reason: 'not_found' };

  const entry = entries[idx];

  if (entry.coordinates.lat === null || entry.coordinates.lng === null) {
    return { success: false, reason: 'no_gps' };
  }

  await requestLocationIfNeeded();
  const current = await getCurrentCoordinates();
  if (current.lat === null || current.lng === null) {
    return { success: false, reason: 'no_gps' };
  }

  const distanceKm = haversineKm(
    current.lat, current.lng,
    entry.coordinates.lat, entry.coordinates.lng
  );
  const distanceM = distanceKm * 1000;

  if (distanceM > VERIFY_RADIUS_M) {
    return {
      success: false,
      reason: 'too_far',
      distanceM,
      targetCoords: { lat: entry.coordinates.lat, lng: entry.coordinates.lng },
    };
  }

  const verified: AnomalyEntry = {
    ...entry,
    verified: true,
    verifiedAt: new Date().toISOString(),
    verificationMagnitude: currentMagnitude,
  };
  entries[idx] = verified;
  await saveAnomalyLog(entries);

  return { success: true, entry: verified };
}

export async function updateEntry(updated: AnomalyEntry): Promise<void> {
  const entries = await loadAnomalyLog();
  const idx = entries.findIndex((e) => e.id === updated.id);
  if (idx !== -1) {
    entries[idx] = updated;
    await saveAnomalyLog(entries);
  }
}

export async function clearAnomalyLog(): Promise<void> {
  await AsyncStorage.removeItem(ANOMALY_LOG_KEY);
}

// Phase 4 server sync stub
export async function syncAnomalyLog(): Promise<void> {
  // TODO Phase 4: POST unsynced entries to server, mark synced = true
  // const unsyncedEntries = (await loadAnomalyLog()).filter(e => !e.synced);
  // await fetch(PHASE3_SYNC_ENDPOINT!, { method: 'POST', body: JSON.stringify(unsyncedEntries) });
}
