import { create } from 'zustand';

import type { RoomGeometry } from '../types/decode';

export type SweepStatus = 'idle' | 'measuring' | 'locked';

type DecodeState = {
  roomGeometry: RoomGeometry | null;
  layerMag: boolean;
  layerRF: boolean;
  layerAcoustic: boolean;
  layerGravity: boolean;
  layerDataRain: boolean;
  anomalyActive: boolean;
  locationFingerprint: string | null;
  isScanning: boolean;
  sweepStatus: SweepStatus;
  setRoomGeometry: (g: RoomGeometry | null) => void;
  setLayer: (key: 'layerMag' | 'layerRF' | 'layerAcoustic' | 'layerGravity' | 'layerDataRain', v: boolean) => void;
  setSweepStatus: (s: SweepStatus) => void;
};

export const useDecodeStore = create<DecodeState>((set) => ({
  roomGeometry: null,
  layerMag: false,
  layerRF: false,
  layerAcoustic: true,
  layerGravity: false,
  layerDataRain: false,
  anomalyActive: false,
  locationFingerprint: null,
  isScanning: false,
  sweepStatus: 'idle',
  setRoomGeometry: (g) => set({ roomGeometry: g }),
  setLayer: (key, v) => set({ [key]: v } as Partial<DecodeState>),
  setSweepStatus: (s) => set({ sweepStatus: s }),
}));
