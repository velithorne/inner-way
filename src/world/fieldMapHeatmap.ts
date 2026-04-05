import * as THREE from 'three';
import type { WifiNetwork } from '../types/wifi';
const VOXEL = 2;

/** Average RSSI in dBm from linear power mean. */
export function averageRssiDbm(networks: WifiNetwork[]): number {
  if (networks.length === 0) return -100;
  let sum = 0;
  for (const n of networks) {
    sum += Math.pow(10, n.rssi / 10);
  }
  const avg = sum / networks.length;
  return 10 * Math.log10(Math.max(1e-15, avg));
}

/** Strongest RSSI among networks matching SSID (case-sensitive trim). */
export function rssiForSsid(networks: WifiNetwork[], ssid: string): number | null {
  const want = ssid.trim();
  let best: number | null = null;
  for (const n of networks) {
    if (n.ssid === want) {
      if (best == null || n.rssi > best) best = n.rssi;
    }
  }
  return best;
}

export type LocalPoint = {
  x: number;
  z: number;
  timestamp: number;
  energyDbm: number;
  lowPrecision: boolean;
  networks: WifiNetwork[];
};

export function gpsToLocalMeters(
  origin: { lat: number; lng: number },
  lat: number,
  lng: number
): { x: number; z: number } {
  const cosLat = Math.cos((origin.lat * Math.PI) / 180);
  const x = (lng - origin.lng) * 111320 * cosLat;
  const z = (lat - origin.lat) * 110540;
  return { x, z };
}

export function pointsToLocal(
  raw: { lat: number; lng: number; timestamp: number; networks: WifiNetwork[]; low_precision: boolean }[],
  ssidFilter: string | null
): LocalPoint[] {
  if (raw.length === 0) return [];
  const lat0 = raw[0].lat;
  const lng0 = raw[0].lng;
  return raw.map((p) => {
    const { x, z } = gpsToLocalMeters({ lat: lat0, lng: lng0 }, p.lat, p.lng);
    let energy: number;
    if (ssidFilter == null) {
      energy = averageRssiDbm(p.networks);
    } else {
      const r = rssiForSsid(p.networks, ssidFilter);
      energy = r != null ? r : -100;
    }
    return {
      x,
      z,
      timestamp: p.timestamp,
      energyDbm: energy,
      lowPrecision: p.low_precision,
      networks: p.networks,
    };
  });
}

export function energyToColor(dbm: number): THREE.Color {
  const stops: [number, number][] = [
    [-90, 0x220044],
    [-80, 0x7700ff],
    [-70, 0x00ffe5],
    [-60, 0xaaffff],
    [-50, 0xffffff],
  ];
  if (dbm <= stops[0][0]) return new THREE.Color(stops[0][1]);
  if (dbm >= stops[stops.length - 1][0]) return new THREE.Color(stops[stops.length - 1][1]);
  for (let i = 0; i < stops.length - 1; i++) {
    const [e0, c0] = stops[i];
    const [e1, c1] = stops[i + 1];
    if (dbm >= e0 && dbm <= e1) {
      const t = (dbm - e0) / (e1 - e0);
      return new THREE.Color(c0).lerp(new THREE.Color(c1), t);
    }
  }
  return new THREE.Color(0x220044);
}

export type VoxelAgg = {
  key: string;
  ix: number;
  iz: number;
  /** World centre XZ */
  cx: number;
  cz: number;
  energyDbm: number;
  lowPrecisionFrac: number;
  count: number;
};

export function buildVoxels(localPoints: LocalPoint[], voxelSize = VOXEL): VoxelAgg[] {
  const map = new Map<
    string,
    { sumE: number; sumLp: number; count: number; ix: number; iz: number }
  >();
  for (const p of localPoints) {
    const ix = Math.floor(p.x / voxelSize);
    const iz = Math.floor(p.z / voxelSize);
    const key = `${ix},${iz}`;
    const prev = map.get(key);
    if (prev) {
      prev.sumE += p.energyDbm;
      prev.sumLp += p.lowPrecision ? 1 : 0;
      prev.count += 1;
    } else {
      map.set(key, {
        sumE: p.energyDbm,
        sumLp: p.lowPrecision ? 1 : 0,
        count: 1,
        ix,
        iz,
      });
    }
  }
  const out: VoxelAgg[] = [];
  for (const [key, v] of map) {
    const cx = (v.ix + 0.5) * voxelSize;
    const cz = (v.iz + 0.5) * voxelSize;
    out.push({
      key,
      ix: v.ix,
      iz: v.iz,
      cx,
      cz,
      energyDbm: v.sumE / v.count,
      lowPrecisionFrac: v.sumLp / v.count,
      count: v.count,
    });
  }
  return out;
}

export function voxelHeightForEnergy(energyDbm: number): number {
  return Math.max(0.4, ((energyDbm + 100) / 50) * 4.0);
}

export type DeadZone = { cx: number; cz: number; ix: number; iz: number };

/**
 * Voxels with energy < -82 dBm with at least 4 neighbors (8-neighbour grid)
 * that exist and have energy > -78 (stronger pocket inside coverage).
 */
export function findDeadZones(voxels: VoxelAgg[]): DeadZone[] {
  const byKey = new Map<string, VoxelAgg>();
  for (const v of voxels) byKey.set(v.key, v);

  const dead: DeadZone[] = [];
  for (const v of voxels) {
    if (v.energyDbm >= -82) continue;
    let stronger = 0;
    for (let dx = -1; dx <= 1; dx++) {
      for (let dz = -1; dz <= 1; dz++) {
        if (dx === 0 && dz === 0) continue;
        const nk = `${v.ix + dx},${v.iz + dz}`;
        const n = byKey.get(nk);
        if (n && n.energyDbm > -78) stronger++;
      }
    }
    if (stronger >= 4) {
      dead.push({ cx: v.cx, cz: v.cz, ix: v.ix, iz: v.iz });
    }
  }
  return dead;
}

export function sceneCentre(voxels: VoxelAgg[]): { cx: number; cz: number } {
  if (voxels.length === 0) return { cx: 0, cz: 0 };
  let sx = 0;
  let sz = 0;
  for (const v of voxels) {
    sx += v.cx;
    sz += v.cz;
  }
  return { cx: sx / voxels.length, cz: sz / voxels.length };
}

/** Path length using local XZ metres (consistent with voxel coords). */
export function pathLengthLocal(localPoints: LocalPoint[]): number {
  if (localPoints.length < 2) return 0;
  let d = 0;
  for (let i = 1; i < localPoints.length; i++) {
    const a = localPoints[i - 1];
    const b = localPoints[i];
    const dx = b.x - a.x;
    const dz = b.z - a.z;
    d += Math.sqrt(dx * dx + dz * dz);
  }
  return d;
}

export function uniqueSsidsFromPoints(
  points: { networks: WifiNetwork[] }[]
): string[] {
  const s = new Set<string>();
  for (const p of points) {
    for (const n of p.networks) {
      if (n.ssid && n.ssid.length > 0) s.add(n.ssid);
    }
  }
  return [...s].sort();
}

export function strongestNetworkAtPoint(points: LocalPoint[]): {
  ssid: string;
  rssi: number;
} | null {
  let best: { ssid: string; rssi: number } | null = null;
  for (const p of points) {
    for (const n of p.networks) {
      if (!n.ssid) continue;
      if (!best || n.rssi > best.rssi) best = { ssid: n.ssid, rssi: n.rssi };
    }
  }
  return best;
}

export type SessionSummary = {
  pointCount: number;
  pathMetres: number;
  minDbm: number;
  maxDbm: number;
  strongest: { ssid: string; rssi: number; offsetXM: number; offsetZM: number } | null;
  weakestVoxel: { energyDbm: number; offsetXM: number; offsetZM: number } | null;
  deadZoneCount: number;
};

export function computeSessionSummary(
  localPoints: LocalPoint[],
  voxels: VoxelAgg[],
  deadZones: DeadZone[]
): SessionSummary {
  const pathMetres = pathLengthLocal(localPoints);
  let minDbm = 0;
  let maxDbm = -100;
  if (localPoints.length > 0) {
    minDbm = localPoints[0].energyDbm;
    maxDbm = localPoints[0].energyDbm;
    for (const p of localPoints) {
      minDbm = Math.min(minDbm, p.energyDbm);
      maxDbm = Math.max(maxDbm, p.energyDbm);
    }
  }
  let strongest: SessionSummary['strongest'] = null;
  for (const p of localPoints) {
    for (const n of p.networks) {
      if (!n.ssid) continue;
      if (!strongest || n.rssi > strongest.rssi) {
        strongest = {
          ssid: n.ssid,
          rssi: n.rssi,
          offsetXM: p.x,
          offsetZM: p.z,
        };
      }
    }
  }
  let weakestVoxel: SessionSummary['weakestVoxel'] = null;
  for (const v of voxels) {
    if (!weakestVoxel || v.energyDbm < weakestVoxel.energyDbm) {
      weakestVoxel = {
        energyDbm: v.energyDbm,
        offsetXM: v.cx,
        offsetZM: v.cz,
      };
    }
  }
  return {
    pointCount: localPoints.length,
    pathMetres,
    minDbm,
    maxDbm,
    strongest,
    weakestVoxel,
    deadZoneCount: deadZones.length,
  };
}
