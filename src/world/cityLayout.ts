import * as THREE from 'three';
import type { RouterEstimate } from '../services/routerTriangulator';
import { rssiToDistance } from '../services/rssiToDistance';
import type { WifiNetwork } from '../types/wifi';
import { findDualBandPairs, isLikelySolarInverter } from '../services/deviceHeuristics';

export type CityBuilding = {
  bssid: string;
  ssid: string;
  rssi: number;
  frequency: number;
  channel: number;
  capabilities: string;
  position: THREE.Vector3;
  height: number;
  width: number;
  depth: number;
  kind: 'router' | 'solar';
  dualPartnerBssid?: string;
};

const MAX_BUILDINGS = 24;
const DISTANCE_SCALE = 1.5;
const MIN_SEP = 12;

function channelWidthHint(freq: number): number {
  return freq > 4000 ? 80 : 20;
}

function radialPosition(est: RouterEstimate | undefined, rssi: number, freq: number): THREE.Vector3 {
  const br = ((est?.bearing ?? 0) * Math.PI) / 180;
  const d = est?.distance ?? rssiToDistance(rssi, freq);
  const r = Math.max(20, d * DISTANCE_SCALE);
  return new THREE.Vector3(Math.sin(br) * r, 0, -Math.cos(br) * r);
}

function separateBuildings(buildings: CityBuilding[]): void {
  for (let pass = 0; pass < 4; pass++) {
    for (let i = 0; i < buildings.length; i++) {
      for (let j = i + 1; j < buildings.length; j++) {
        const bi = buildings[i];
        const bj = buildings[j];
        const dx = bj.position.x - bi.position.x;
        const dz = bj.position.z - bi.position.z;
        const dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 1e-3 || dist >= MIN_SEP) continue;
        const angle = Math.atan2(dz, dx);
        const push = (MIN_SEP - dist) / 2;
        bi.position.x -= Math.cos(angle) * push;
        bi.position.z -= Math.sin(angle) * push;
        bj.position.x += Math.cos(angle) * push;
        bj.position.z += Math.sin(angle) * push;
      }
    }
  }
  for (const b of buildings) {
    b.position.y = b.height / 2;
  }
}

export function layoutCityBuildings(
  networks: WifiNetwork[],
  estimates: Map<string, RouterEstimate>
): CityBuilding[] {
  const pairs = findDualBandPairs(networks);
  const paired = new Set<string>();
  const out: CityBuilding[] = [];

  for (const [a, b] of pairs) {
    paired.add(a.bssid);
    paired.add(b.bssid);
    const estA = estimates.get(a.bssid);
    const estB = estimates.get(b.bssid);
    const base = radialPosition(estA, a.rssi, a.frequency);
    const sep = 14;
    const ha = buildingHeight(a.rssi);
    const hb = buildingHeight(b.rssi);
    const wa = 8 + channelWidthHint(a.frequency) * 0.005;
    const wb = 8 + channelWidthHint(b.frequency) * 0.005;
    const perpDir = new THREE.Vector3(-base.z, 0, base.x);
    if (perpDir.lengthSq() < 1e-6) perpDir.set(1, 0, 0);
    perpDir.normalize().multiplyScalar(sep * 0.5);
    const off = perpDir;
    out.push({
      bssid: a.bssid,
      ssid: a.ssid,
      rssi: a.rssi,
      frequency: a.frequency,
      channel: a.channel,
      capabilities: a.capabilities,
      position: new THREE.Vector3(base.x - off.x, ha / 2, base.z - off.z),
      height: ha,
      width: wa,
      depth: 8,
      kind: 'router',
      dualPartnerBssid: b.bssid,
    });
    out.push({
      bssid: b.bssid,
      ssid: b.ssid,
      rssi: b.rssi,
      frequency: b.frequency,
      channel: b.channel,
      capabilities: b.capabilities,
      position: new THREE.Vector3(base.x + off.x, hb / 2, base.z + off.z),
      height: hb,
      width: wb,
      depth: 8,
      kind: 'router',
      dualPartnerBssid: a.bssid,
    });
  }

  const sorted = [...networks].sort((a, b) => b.rssi - a.rssi);
  for (const n of sorted) {
    if (paired.has(n.bssid)) continue;
    if (out.length >= MAX_BUILDINGS) break;
    const est = estimates.get(n.bssid);
    const p = radialPosition(est, n.rssi, n.frequency);
    const h = buildingHeight(n.rssi);
    const w = 8 + channelWidthHint(n.frequency) * 0.005;
    const solar = isLikelySolarInverter(n);
    out.push({
      bssid: n.bssid,
      ssid: n.ssid,
      rssi: n.rssi,
      frequency: n.frequency,
      channel: n.channel,
      capabilities: n.capabilities,
      position: new THREE.Vector3(p.x, h / 2, p.z),
      height: h,
      width: w,
      depth: 8,
      kind: solar ? 'solar' : 'router',
    });
  }

  separateBuildings(out);
  return out;
}

export function buildingHeight(rssi: number): number {
  return Math.max(4, ((rssi + 100) / 70) * 80);
}
