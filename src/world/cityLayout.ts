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

function channelWidthHint(freq: number): number {
  return freq > 4000 ? 80 : 20;
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
    const brA = ((estA?.bearing ?? 0) * Math.PI) / 180;
    const distA = (estA?.distance ?? rssiToDistance(a.rssi, a.frequency)) * 0.15;
    const baseX = Math.sin(brA) * distA;
    const baseZ = -Math.cos(brA) * distA;
    const sep = 5;
    const ha = buildingHeight(a.rssi);
    const hb = buildingHeight(b.rssi);
    const wa = 8 + channelWidthHint(a.frequency) * 0.005;
    const wb = 8 + channelWidthHint(b.frequency) * 0.005;
    out.push({
      bssid: a.bssid,
      ssid: a.ssid,
      rssi: a.rssi,
      frequency: a.frequency,
      channel: a.channel,
      capabilities: a.capabilities,
      position: new THREE.Vector3(baseX - sep * 0.5, ha / 2, baseZ),
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
      position: new THREE.Vector3(baseX + sep * 0.5, hb / 2, baseZ),
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
    const br = ((est?.bearing ?? 0) * Math.PI) / 180;
    const dist = (est?.distance ?? rssiToDistance(n.rssi, n.frequency)) * 0.15;
    const x = Math.sin(br) * dist;
    const z = -Math.cos(br) * dist;
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
      position: new THREE.Vector3(x, h / 2, z),
      height: h,
      width: w,
      depth: 8,
      kind: solar ? 'solar' : 'router',
    });
  }

  return out;
}

export function buildingHeight(rssi: number): number {
  return Math.max(4, ((rssi + 100) / 70) * 80);
}
