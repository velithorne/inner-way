import { rssiToDistance } from './rssiToDistance';

export interface RouterObservation {
  bssid: string;
  rssi: number;
  bearing: number;
  timestamp: number;
  frequencyMHz?: number;
  userPosition?: { lat: number; lng: number };
}

export interface RouterEstimate {
  bearing: number;
  distance: number;
  confidence: number;
}

const MAX_OBS = 30;

function bssidHash(bssid: string): number {
  const hex = bssid.replace(/:/g, '').toLowerCase();
  let h = 0;
  for (const c of hex) {
    h = (h * 31 + c.charCodeAt(0)) % 100000;
  }
  return h;
}

/** Stable compass bearing in radians from BSSID (fallback). */
export function bssidToBearingRad(bssid: string): number {
  return ((bssidHash(bssid) % 360) * Math.PI) / 180;
}

function confidenceFromCount(n: number): number {
  if (n < 5) return 20;
  if (n < 10) return 45;
  if (n < 20) return 65;
  return 80;
}

function circularMeanDeg(bearings: number[], weights: number[]): number {
  let sx = 0;
  let sy = 0;
  let wsum = 0;
  for (let i = 0; i < bearings.length; i++) {
    const w = weights[i] ?? 1;
    const r = (bearings[i] * Math.PI) / 180;
    sx += Math.sin(r) * w;
    sy += Math.cos(r) * w;
    wsum += w;
  }
  if (wsum < 1e-6) return 0;
  const ang = Math.atan2(sx / wsum, sy / wsum);
  let deg = (ang * 180) / Math.PI;
  if (deg < 0) deg += 360;
  return deg;
}

export class RouterTriangulator {
  private observations = new Map<string, RouterObservation[]>();

  addObservation(obs: RouterObservation): void {
    const list = this.observations.get(obs.bssid) ?? [];
    list.push(obs);
    while (list.length > MAX_OBS) list.shift();
    this.observations.set(obs.bssid, list);
  }

  estimatePosition(bssid: string, latestRssi: number, frequencyMHz: number): RouterEstimate {
    const obs = this.observations.get(bssid);
    if (!obs || obs.length < 3) {
      const br = bssidToBearingRad(bssid);
      return {
        bearing: (br * 180) / Math.PI,
        distance: rssiToDistance(latestRssi, frequencyMHz),
        confidence: 10,
      };
    }

    const sorted = [...obs].sort((a, b) => b.rssi - a.rssi);
    const topN = Math.max(1, Math.ceil(sorted.length * 0.25));
    const peak = sorted.slice(0, topN);
    const bearings = peak.map((o) => o.bearing);
    const weights = peak.map((o) => o.rssi + 100);
    const meanBearing = circularMeanDeg(bearings, weights);

    const medianRssi = peak[Math.floor(peak.length / 2)].rssi;
    const freq =
      peak.find((o) => o.frequencyMHz != null)?.frequencyMHz ?? frequencyMHz;
    const dist = rssiToDistance(medianRssi, freq);

    const spread =
      peak.length > 1
        ? Math.max(...peak.map((p) => p.rssi)) - Math.min(...peak.map((p) => p.rssi))
        : 0;
    let conf = confidenceFromCount(obs.length);
    if (spread > 15) conf = Math.max(15, conf - 15);

    return {
      bearing: meanBearing,
      distance: dist,
      confidence: Math.min(95, conf),
    };
  }

  clear(): void {
    this.observations.clear();
  }
}
