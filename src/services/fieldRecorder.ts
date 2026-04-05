import * as SQLite from 'expo-sqlite';
import type { WifiNetwork } from '../types/wifi';

export type FieldPoint = {
  lat: number;
  lng: number;
  timestamp: number;
  networks: WifiNetwork[];
};

export type FieldSessionRow = {
  id: number;
  name: string;
  created_at: number;
  point_count: number;
  network_count: number;
};

export type FieldPointRow = {
  id: number;
  lat: number;
  lng: number;
  timestamp: number;
  networks: WifiNetwork[];
  low_precision: boolean;
};

let dbPromise: Promise<SQLite.SQLiteDatabase> | null = null;

async function getDb(): Promise<SQLite.SQLiteDatabase> {
  if (!dbPromise) {
    dbPromise = (async () => {
      const db = await SQLite.openDatabaseAsync('phantom_field.db');
      await db.execAsync(`
        CREATE TABLE IF NOT EXISTS field_sessions (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          name TEXT NOT NULL,
          created_at INTEGER NOT NULL
        );
        CREATE TABLE IF NOT EXISTS field_points (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          session_id INTEGER NOT NULL,
          lat REAL NOT NULL,
          lng REAL NOT NULL,
          timestamp INTEGER NOT NULL,
          networks_json TEXT NOT NULL,
          low_precision INTEGER NOT NULL DEFAULT 0,
          FOREIGN KEY(session_id) REFERENCES field_sessions(id)
        );
      `);
      try {
        await db.execAsync(
          'ALTER TABLE field_points ADD COLUMN low_precision INTEGER NOT NULL DEFAULT 0'
        );
      } catch {
        /* column already exists */
      }
      return db;
    })();
  }
  return dbPromise;
}

export async function createSession(name: string): Promise<number> {
  const db = await getDb();
  const now = Date.now();
  const r = await db.runAsync(
    'INSERT INTO field_sessions (name, created_at) VALUES (?, ?)',
    name,
    now
  );
  return r.lastInsertRowId;
}

export async function addFieldPoint(
  sessionId: number,
  point: FieldPoint,
  options?: { lowPrecision?: boolean }
): Promise<void> {
  const db = await getDb();
  const netsJson = JSON.stringify(point.networks);
  const low = options?.lowPrecision ? 1 : 0;
  await db.runAsync(
    'INSERT INTO field_points (session_id, lat, lng, timestamp, networks_json, low_precision) VALUES (?, ?, ?, ?, ?, ?)',
    sessionId,
    point.lat,
    point.lng,
    point.timestamp,
    netsJson,
    low
  );
}

export async function getSessionPoints(sessionId: number): Promise<FieldPointRow[]> {
  const db = await getDb();
  const rows = await db.getAllAsync<{
    id: number;
    lat: number;
    lng: number;
    timestamp: number;
    networks_json: string;
    low_precision: number;
  }>(
    'SELECT id, lat, lng, timestamp, networks_json, low_precision FROM field_points WHERE session_id = ? ORDER BY timestamp ASC',
    sessionId
  );
  return rows.map((r) => ({
    id: r.id,
    lat: r.lat,
    lng: r.lng,
    timestamp: r.timestamp,
    networks: safeParseNetworks(r.networks_json),
    low_precision: r.low_precision !== 0,
  }));
}

function safeParseNetworks(json: string): WifiNetwork[] {
  try {
    const n = JSON.parse(json) as WifiNetwork[];
    return Array.isArray(n) ? n : [];
  } catch {
    return [];
  }
}

export async function getSessionMeta(
  sessionId: number
): Promise<{ id: number; name: string; created_at: number } | null> {
  const db = await getDb();
  const row = await db.getFirstAsync<{ id: number; name: string; created_at: number }>(
    'SELECT id, name, created_at FROM field_sessions WHERE id = ?',
    sessionId
  );
  return row ?? null;
}

export async function listSessions(): Promise<FieldSessionRow[]> {
  const db = await getDb();
  const sessions = await db.getAllAsync<{ id: number; name: string; created_at: number }>(
    'SELECT id, name, created_at FROM field_sessions ORDER BY created_at DESC'
  );
  const out: FieldSessionRow[] = [];
  for (const s of sessions) {
    const points = await db.getAllAsync<{ networks_json: string }>(
      'SELECT networks_json FROM field_points WHERE session_id = ?',
      s.id
    );
    const allBss = new Set<string>();
    for (const p of points) {
      try {
        const nets = JSON.parse(p.networks_json) as WifiNetwork[];
        nets.forEach((n) => allBss.add(n.bssid));
      } catch {
        /* ignore */
      }
    }
    out.push({
      ...s,
      point_count: points.length,
      network_count: allBss.size,
    });
  }
  return out;
}
