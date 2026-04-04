import { NativeModules, Platform } from 'react-native';
import WifiManager from 'react-native-wifi-reborn';
import { Color } from 'three';
import type { WifiNetwork } from '../types/wifi';
import { networkColour } from './colourFromBssid';
import { rssiToDistance } from './rssiToDistance';

const { WifiScanModule } = NativeModules as {
  WifiScanModule?: { scanNetworks: () => Promise<Record<string, unknown>[]> };
};

const SCAN_MS = 2000;

function channelFromFrequency(freq: number): number {
  if (freq >= 2412 && freq <= 2484) return Math.floor((freq - 2412) / 5) + 1;
  if (freq >= 5180) return Math.floor((freq - 5180) / 5) + 36;
  return 0;
}

function normalizeEntry(
  e: Record<string, unknown>,
  fallbackTs: number
): WifiNetwork | null {
  const bssid = String(e.bssid ?? e.BSSID ?? '');
  if (!bssid || bssid === '00:00:00:00:00:00') return null;
  const freq = Number(e.frequency ?? 0);
  return {
    ssid: String(e.ssid ?? e.SSID ?? ''),
    bssid,
    rssi: Number(e.rssi ?? e.level ?? -100),
    frequency: freq,
    channel: Number(e.channel ?? channelFromFrequency(freq)),
    capabilities: String(e.capabilities ?? ''),
    timestamp: Number(e.timestamp ?? fallbackTs),
  };
}

function dedupeStrongest(list: WifiNetwork[]): WifiNetwork[] {
  const byBssid = new Map<string, WifiNetwork>();
  for (const n of list) {
    const prev = byBssid.get(n.bssid);
    if (!prev || n.rssi > prev.rssi) byBssid.set(n.bssid, n);
  }
  return [...byBssid.values()].sort((a, b) => b.rssi - a.rssi);
}

async function scanOnceNative(): Promise<WifiNetwork[]> {
  const now = Date.now();
  if (!WifiScanModule?.scanNetworks) return [];
  const raw = await WifiScanModule.scanNetworks();
  const list = Array.isArray(raw) ? raw : [];
  const out: WifiNetwork[] = [];
  for (const row of list) {
    const n = normalizeEntry(row as Record<string, unknown>, now);
    if (n) out.push(n);
  }
  return dedupeStrongest(out);
}

async function scanOnceReborn(): Promise<WifiNetwork[]> {
  const now = Date.now();
  let entries: Array<{
    SSID: string;
    BSSID: string;
    frequency: number;
    level: number;
    capabilities: string;
    timestamp: number;
  }>;
  try {
    entries = await WifiManager.reScanAndLoadWifiList();
  } catch {
    try {
      entries = await WifiManager.loadWifiList();
    } catch {
      return [];
    }
  }
  const out: WifiNetwork[] = [];
  for (const e of entries) {
    const n = normalizeEntry(
      {
        ssid: e.SSID,
        bssid: e.BSSID,
        rssi: e.level,
        frequency: e.frequency,
        capabilities: e.capabilities,
        timestamp: e.timestamp || now,
      },
      now
    );
    if (n) out.push(n);
  }
  return dedupeStrongest(out);
}

export class WifiScanner {
  private intervalId: ReturnType<typeof setInterval> | null = null;
  private cache: WifiNetwork[] = [];

  start(onScan: (nets: WifiNetwork[]) => void) {
    const tick = async () => {
      try {
        const nets =
          Platform.OS === 'android' && WifiScanModule?.scanNetworks
            ? await scanOnceNative()
            : await scanOnceReborn();
        if (nets.length > 0) {
          this.cache = nets;
        }
        onScan([...this.cache]);
      } catch {
        onScan([...this.cache]);
      }
    };
    void tick();
    this.intervalId = setInterval(() => {
      void tick();
    }, SCAN_MS);
  }

  stop() {
    if (this.intervalId != null) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  getDistance(rssi: number, freq: number): number {
    return rssiToDistance(rssi, freq);
  }

  getColour(bssid: string, frequency: number): Color {
    return networkColour(bssid, frequency);
  }
}
