import { NativeModules, Platform } from 'react-native';

export type CpuCoreUsage = { core: number; usage: number };

export type MemoryInfo = {
  totalRam: number;
  availableRam: number;
  usedRam: number;
  lowMemory: boolean;
  threshold: number;
};

export type RunningAppRow = {
  packageName: string;
  appName: string;
  memoryBytes: number;
  pid: number;
  importance: string;
};

export type NetworkTraffic = {
  rxBytesTotal: number;
  txBytesTotal: number;
  rxBytesPerSecond: number;
  txBytesPerSecond: number;
};

export type StorageInfo = {
  totalBytes: number;
  usedBytes: number;
  freeBytes: number;
  appDataBytes: number;
  mediaBytes: number;
};

export type BatteryDetails = {
  level: number;
  isCharging: boolean;
  voltage: number;
  temperature: number;
  currentNow: number;
};

type SystemDataNative = {
  getCpuCoreUsage: () => Promise<CpuCoreUsage[]>;
  getMemoryInfo: () => Promise<MemoryInfo>;
  getRunningApps: () => Promise<RunningAppRow[]>;
  getNetworkTraffic: () => Promise<NetworkTraffic>;
  getStorageInfo: () => Promise<StorageInfo>;
  getBatteryDetails: () => Promise<BatteryDetails>;
};

const native: SystemDataNative | undefined =
  Platform.OS === 'android' ? (NativeModules.SystemData as SystemDataNative) : undefined;

export function isSystemDataAvailable(): boolean {
  return Platform.OS === 'android' && native != null;
}

export async function fetchDeviceSnapshot(): Promise<{
  cpu: CpuCoreUsage[];
  memory: MemoryInfo;
  apps: RunningAppRow[];
  network: NetworkTraffic;
  storage: StorageInfo;
  battery: BatteryDetails;
}> {
  if (!native) {
    throw new Error('SystemData native module is only available on Android');
  }
  const [cpu, memory, apps, network, storage, battery] = await Promise.all([
    native.getCpuCoreUsage(),
    native.getMemoryInfo(),
    native.getRunningApps(),
    native.getNetworkTraffic(),
    native.getStorageInfo(),
    native.getBatteryDetails(),
  ]);
  return { cpu, memory, apps, network, storage, battery };
}
