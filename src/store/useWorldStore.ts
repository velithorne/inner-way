import { create } from 'zustand';

import type {
  BatteryDetails,
  CpuCoreUsage,
  MemoryInfo,
  NetworkTraffic,
  RunningAppRow,
  StorageInfo,
} from '../native/systemData';

export type Anomaly = { id: string; message: string; severity: 'low' | 'medium' | 'high' };

export type DeviceSnapshot = {
  timestamp: number;
  cpu: CpuCoreUsage[];
  memory: MemoryInfo;
  apps: RunningAppRow[];
  network: NetworkTraffic;
  storage: StorageInfo;
  battery: BatteryDetails;
};

export type TelemetryState = 'idle' | 'loading' | 'ok' | 'error';

type WorldState = {
  snapshot: DeviceSnapshot | null;
  cpuLoad: number;
  ramPressure: number;
  networkActive: boolean;
  batteryLevel: number;
  topApps: RunningAppRow[];
  anomalies: Anomaly[];
  telemetryState: TelemetryState;
  telemetryMessage: string | null;
  /** 0–1 WiFi signal strength for sky clouds (NetInfo); null if unknown */
  wifiStrength: number | null;
  setFromSnapshot: (s: DeviceSnapshot) => void;
  setTelemetryLoading: () => void;
  setTelemetryError: (message: string) => void;
  setWifiStrength: (v: number | null) => void;
  reset: () => void;
};

function aggregateCpuLoad(cpu: CpuCoreUsage[]): number {
  if (!cpu.length) return 0;
  const sum = cpu.reduce((a, c) => a + c.usage, 0);
  return sum / cpu.length;
}

function ramPressurePct(memory: MemoryInfo): number {
  if (memory.totalRam <= 0) return 0;
  return (memory.usedRam / memory.totalRam) * 100;
}

export const useWorldStore = create<WorldState>((set) => ({
  snapshot: null,
  cpuLoad: 0,
  ramPressure: 0,
  networkActive: false,
  batteryLevel: 0,
  topApps: [],
  anomalies: [],
  telemetryState: 'idle',
  telemetryMessage: null,
  wifiStrength: null,
  setFromSnapshot: (s) => {
    const sorted = [...s.apps].sort((a, b) => b.memoryBytes - a.memoryBytes);
    const rx = s.network.rxBytesPerSecond;
    const tx = s.network.txBytesPerSecond;
    set({
      snapshot: s,
      cpuLoad: aggregateCpuLoad(s.cpu),
      ramPressure: ramPressurePct(s.memory),
      networkActive: rx + tx > 8 * 1024,
      batteryLevel: s.battery.level,
      topApps: sorted.slice(0, 10),
      anomalies: [],
      telemetryState: 'ok',
      telemetryMessage: null,
    });
  },
  setTelemetryLoading: () =>
    set({
      telemetryState: 'loading',
      telemetryMessage: null,
    }),
  setTelemetryError: (message) =>
    set({
      telemetryState: 'error',
      telemetryMessage: message,
    }),
  setWifiStrength: (v) => set({ wifiStrength: v }),
  reset: () =>
    set({
      snapshot: null,
      cpuLoad: 0,
      ramPressure: 0,
      networkActive: false,
      batteryLevel: 0,
      topApps: [],
      anomalies: [],
      telemetryState: 'idle',
      telemetryMessage: null,
      wifiStrength: null,
    }),
}));
