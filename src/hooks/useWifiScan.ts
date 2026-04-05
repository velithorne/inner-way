import { useCallback, useEffect, useRef, useState } from 'react';
import { Platform } from 'react-native';
import WifiManager from 'react-native-wifi-reborn';
import type { WifiNetwork } from '../types/wifi';

function mapEntry(e: {
  SSID: string;
  BSSID: string;
  level: number;
  frequency: number;
}): WifiNetwork {
  return {
    ssid: e.SSID || '(hidden)',
    bssid: e.BSSID || '',
    rssi: e.level,
    frequency: e.frequency ?? 0,
  };
}

export function useWifiScan(
  enabled: boolean,
  intervalMs: number,
  onScanComplete?: (networks: WifiNetwork[]) => void,
) {
  const [networks, setNetworks] = useState<WifiNetwork[]>([]);
  const [scanning, setScanning] = useState(false);
  const onCompleteRef = useRef(onScanComplete);
  onCompleteRef.current = onScanComplete;

  const runScan = useCallback(async () => {
    if (Platform.OS !== 'android') {
      setNetworks([]);
      onCompleteRef.current?.([]);
      return;
    }
    setScanning(true);
    try {
      const list = await WifiManager.reScanAndLoadWifiList();
      const next = list.map(mapEntry);
      setNetworks(next);
      onCompleteRef.current?.(next);
    } catch {
      try {
        const list = await WifiManager.loadWifiList();
        const next = list.map(mapEntry);
        setNetworks(next);
        onCompleteRef.current?.(next);
      } catch {
        setNetworks([]);
        onCompleteRef.current?.([]);
      }
    } finally {
      setScanning(false);
    }
  }, []);

  useEffect(() => {
    if (!enabled) return;
    void runScan();
    const id = setInterval(() => void runScan(), intervalMs);
    return () => clearInterval(id);
  }, [enabled, intervalMs, runScan]);

  return { networks, scanning, rescan: runScan };
}
