/**
 * Connection lines — user-drawn and auto-generated lines between anomaly nodes.
 * Persisted to AsyncStorage.
 */

import AsyncStorage from '@react-native-async-storage/async-storage';
import { bearing, checkLineAlignment, LatLng, LineAlignmentResult } from '../utils/geo';
import { haversineKm } from '../constants/sacredSites';

const LINES_KEY = '@resonance_map_connection_lines';

export type LineKind = 'node-node' | 'node-site' | 'auto-chrono' | 'auto-proximity';

export interface ConnectionLine {
  id: string;
  fromId: string;        // anomaly entry id or sacred site id
  toId: string;
  fromLat: number;
  fromLng: number;
  toLat: number;
  toLng: number;
  fromVerified: boolean;
  toVerified: boolean;
  kind: LineKind;
  distanceKm: number;
  bearingDeg: number;
  alignment: LineAlignmentResult;
  createdAt: string;
}

export async function loadLines(): Promise<ConnectionLine[]> {
  try {
    const raw = await AsyncStorage.getItem(LINES_KEY);
    if (raw) return JSON.parse(raw);
  } catch {}
  return [];
}

async function saveLines(lines: ConnectionLine[]): Promise<void> {
  await AsyncStorage.setItem(LINES_KEY, JSON.stringify(lines));
}

export async function addLine(params: {
  fromId: string; toId: string;
  fromLat: number; fromLng: number;
  toLat: number; toLng: number;
  fromVerified: boolean; toVerified: boolean;
  kind: LineKind;
}): Promise<ConnectionLine> {
  const existing = await loadLines();
  const existingBearings = existing.map((l) => l.bearingDeg);

  const a: LatLng = { latitude: params.fromLat, longitude: params.fromLng };
  const b: LatLng = { latitude: params.toLat, longitude: params.toLng };

  const line: ConnectionLine = {
    id: `line-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
    fromId: params.fromId,
    toId: params.toId,
    fromLat: params.fromLat,
    fromLng: params.fromLng,
    toLat: params.toLat,
    toLng: params.toLng,
    fromVerified: params.fromVerified,
    toVerified: params.toVerified,
    kind: params.kind,
    distanceKm: Math.round(haversineKm(params.fromLat, params.fromLng, params.toLat, params.toLng)),
    bearingDeg: Math.round(bearing(a, b)),
    alignment: checkLineAlignment(a, b, existingBearings),
    createdAt: new Date().toISOString(),
  };

  await saveLines([...existing, line]);
  return line;
}

export async function clearLines(): Promise<void> {
  await AsyncStorage.removeItem(LINES_KEY);
}

/** Build chronological connection lines from an ordered list of entries */
export async function buildChronologicalLines(
  entries: { id: string; lat: number; lng: number; verified: boolean }[]
): Promise<ConnectionLine[]> {
  await clearLines();
  const valid = entries.filter((e) => e.lat != null && e.lng != null);
  for (let i = 0; i < valid.length - 1; i++) {
    const a = valid[i], b = valid[i + 1];
    await addLine({
      fromId: a.id, toId: b.id,
      fromLat: a.lat, fromLng: a.lng,
      toLat: b.lat, toLng: b.lng,
      fromVerified: a.verified, toVerified: b.verified,
      kind: 'auto-chrono',
    });
  }
  return loadLines();
}

/** Build proximity-ordered (nearest to nearest) connection lines */
export async function buildProximityLines(
  entries: { id: string; lat: number; lng: number; verified: boolean }[]
): Promise<ConnectionLine[]> {
  await clearLines();
  const valid = entries.filter((e) => e.lat != null && e.lng != null);
  if (valid.length < 2) return [];

  const used = new Set<string>();
  const ordered: typeof valid = [valid[0]];
  used.add(valid[0].id);

  while (ordered.length < valid.length) {
    const last = ordered[ordered.length - 1];
    let nearest: typeof valid[0] | null = null;
    let nearestDist = Infinity;
    for (const e of valid) {
      if (used.has(e.id)) continue;
      const d = haversineKm(last.lat, last.lng, e.lat, e.lng);
      if (d < nearestDist) { nearestDist = d; nearest = e; }
    }
    if (!nearest) break;
    ordered.push(nearest);
    used.add(nearest.id);
  }

  for (let i = 0; i < ordered.length - 1; i++) {
    const a = ordered[i], b = ordered[i + 1];
    await addLine({
      fromId: a.id, toId: b.id,
      fromLat: a.lat, fromLng: a.lng,
      toLat: b.lat, toLng: b.lng,
      fromVerified: a.verified, toVerified: b.verified,
      kind: 'auto-proximity',
    });
  }
  return loadLines();
}
