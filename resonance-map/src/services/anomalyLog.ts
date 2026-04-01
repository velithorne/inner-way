import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Location from 'expo-location';
import { getProximityLinksForAnomaly, haversineKm } from '../constants/sacredSites';

const ANOMALY_LOG_KEY = '@resonance_map_anomaly_log';
const MAX_LOG_ENTRIES = 500;

export interface AnomalyEntry {
  id: string;
  timestamp: string;
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
  synced?: boolean;
  verified?: boolean;
  verifiedAt?: string;
  verificationMagnitude?: number;
  proximityLinks?: string[];
}

let locationPermissionGranted: boolean | null = null;

// Cache last known GPS so we never block on a slow fix
let lastKnownLat: number | null = null;
let lastKnownLng: number | null = null;

// Start a background GPS watcher that always keeps lastKnown fresh
let locationWatcher: Location.LocationSubscription | null = null;

export async function startLocationWatcher(): Promise<void> {
  if (locationWatcher) return;
  try {
    const { status } = await Location.requestForegroundPermissionsAsync();
    locationPermissionGranted = status === 'granted';
    if (!locationPermissionGranted) return;

    // Seed immediately with a fast low-accuracy fix
    const quick = await Location.getLastKnownPositionAsync();
    if (quick) {
      lastKnownLat = quick.coords.latitude;
      lastKnownLng = quick.coords.longitude;
    }

    // Keep updating in the background
    locationWatcher = await Location.watchPositionAsync(
      { accuracy: Location.Accuracy.Balanced, timeInterval: 10000, distanceInterval: 50 },
      (loc) => {
        lastKnownLat = loc.coords.latitude;
        lastKnownLng = loc.coords.longitude;
      }
    );
  } catch {}
}

export function stopLocationWatcher(): void {
  if (locationWatcher) {
    locationWatcher.remove();
    locationWatcher = null;
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
  try {
    await AsyncStorage.setItem(ANOMALY_LOG_KEY, JSON.stringify(entries));
  } catch (e) {
    console.warn('[AnomalyLog] Failed to save:', e);
  }
}

/**
 * Log an anomaly immediately — does NOT wait for GPS.
 * Uses the last known GPS position (updated by background watcher).
 * If no GPS fix exists, saves with null coordinates.
 */
export async function logAnomaly(params: {
  magnitude: number;
  delta: number;
  x: number;
  y: number;
  z: number;
  heading: number;
}): Promise<AnomalyEntry> {
  // Use cached GPS — never block on a live fix
  const coordinates = {
    lat: lastKnownLat,
    lng: lastKnownLng,
  };

  // Proximity links from cached position
  const proximityLinks: string[] = [];
  if (coordinates.lat !== null && coordinates.lng !== null) {
    try {
      const links = getProximityLinksForAnomaly(coordinates.lat, coordinates.lng);
      links.forEach((l) => proximityLinks.push(l.site.id));
    } catch {}
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

  const currentLat = lastKnownLat;
  const currentLng = lastKnownLng;
  if (currentLat === null || currentLng === null) {
    return { success: false, reason: 'no_gps' };
  }

  const distanceKm = haversineKm(currentLat, currentLng, entry.coordinates.lat, entry.coordinates.lng);
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

// Phase 4: server sync stub
export async function syncAnomalyLog(): Promise<void> {
  // TODO Phase 4: POST unsynced entries to server
}
