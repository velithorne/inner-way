import { create } from 'zustand';
import type { WifiNetwork } from '../types/wifi';

export interface WifiStore {
  networks: WifiNetwork[];
  networkCount: number;
  strongestNetwork: WifiNetwork | null;
  lastScan: number;
  isScanning: boolean;
  setFromScan: (nets: WifiNetwork[], scannedAt: number) => void;
  setScanning: (v: boolean) => void;
}

export const useWifiStore = create<WifiStore>((set) => ({
  networks: [],
  networkCount: 0,
  strongestNetwork: null,
  lastScan: 0,
  isScanning: false,
  setFromScan: (nets, scannedAt) =>
    set({
      networks: nets,
      networkCount: nets.length,
      strongestNetwork: nets[0] ?? null,
      lastScan: scannedAt,
    }),
  setScanning: (isScanning) => set({ isScanning }),
}));
