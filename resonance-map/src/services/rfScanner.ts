/**
 * RF Scanner — WiFi network scan service.
 * Provides real RSSI values for AR field visualisation.
 *
 * Android: uses react-native-wifi-reborn reScanAndLoadWifiList()
 * iOS: falls back to @react-native-community/netinfo signal strength
 *      (only connected network available due to OS restrictions)
 */

import { Platform } from 'react-native';
import NetInfo from '@react-native-community/netinfo';

// Lazy import so Android doesn't crash on iOS
let WifiManager: any = null;
if (Platform.OS === 'android') {
  try {
    WifiManager = require('react-native-wifi-reborn').default;
  } catch {}
}

export interface RFNetwork {
  id: string;          // BSSID or 'cellular'
  ssid: string;
  rssi: number;        // dBm, -30 (strong) to -90 (weak)
  frequency: number;   // MHz, 0 for cellular
  intensity: number;   // 0.0–1.0 normalised
  pseudoAngle: number; // 0–360 consistent direction from BSSID hash
}

export interface RFScanState {
  networks: RFNetwork[];
  isScanning: boolean;
  isIOS: boolean;
  lastScanMs: number;
}

const SCAN_INTERVAL_MS = 3000;

// Normalise RSSI (-30 strong → -90 weak) to 0..1
function normaliseRSSI(rssi: number): number {
  const clamped = Math.max(-90, Math.min(-30, rssi));
  return (clamped + 90) / 60; // -90→0, -30→1
}

// Derive a consistent pseudo-angle from BSSID string so each network
// always appears in the same direction relative to camera heading
function bssidToAngle(bssid: string): number {
  let hash = 0;
  for (let i = 0; i < bssid.length; i++) {
    hash = ((hash << 5) - hash) + bssid.charCodeAt(i);
    hash |= 0;
  }
  return ((hash >>> 0) % 360);
}

let scanInterval: ReturnType<typeof setInterval> | null = null;
let currentState: RFScanState = {
  networks: [],
  isScanning: false,
  isIOS: Platform.OS === 'ios',
  lastScanMs: 0,
};
const listeners: Set<(state: RFScanState) => void> = new Set();

function notify() {
  listeners.forEach((fn) => fn({ ...currentState }));
}

async function doScan() {
  if (currentState.isScanning) return;
  currentState = { ...currentState, isScanning: true };

  try {
    if (Platform.OS === 'android' && WifiManager) {
      const list = await WifiManager.reScanAndLoadWifiList();
      const networks: RFNetwork[] = list.map((n: any) => ({
        id: n.BSSID ?? n.SSID,
        ssid: n.SSID ?? 'Unknown',
        rssi: n.level ?? -80,
        frequency: n.frequency ?? 0,
        intensity: normaliseRSSI(n.level ?? -80),
        pseudoAngle: bssidToAngle(n.BSSID ?? n.SSID ?? '0'),
      }));
      // Keep strongest 12 networks for performance
      networks.sort((a, b) => b.rssi - a.rssi);
      currentState = {
        ...currentState,
        networks: networks.slice(0, 12),
        isScanning: false,
        lastScanMs: Date.now(),
      };
    } else {
      // iOS / no WifiManager — use NetInfo signal strength
      const info = await NetInfo.fetch();
      const details = info.details as any;
      const rssi = details?.strength != null
        ? -90 + (details.strength / 100) * 60
        : -75;
      currentState = {
        ...currentState,
        networks: [{
          id: 'cellular',
          ssid: 'Cellular',
          rssi,
          frequency: 0,
          intensity: normaliseRSSI(rssi),
          pseudoAngle: 0,
        }],
        isScanning: false,
        isIOS: true,
        lastScanMs: Date.now(),
      };
    }
  } catch {
    currentState = { ...currentState, isScanning: false };
  }

  notify();
}

export function startRFScanner(): void {
  if (scanInterval) return;
  doScan();
  scanInterval = setInterval(doScan, SCAN_INTERVAL_MS);
}

export function stopRFScanner(): void {
  if (scanInterval) {
    clearInterval(scanInterval);
    scanInterval = null;
  }
}

export function subscribeRF(fn: (state: RFScanState) => void): () => void {
  listeners.add(fn);
  fn({ ...currentState }); // immediate current value
  return () => listeners.delete(fn);
}

export function getRFState(): RFScanState {
  return { ...currentState };
}
