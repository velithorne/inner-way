import { create } from 'zustand';

export interface SignalStore {
  highlightBssid: string | null;
  setHighlightBssid: (bssid: string | null) => void;
}

export const useSignalStore = create<SignalStore>((set) => ({
  highlightBssid: null,
  setHighlightBssid: (highlightBssid) => set({ highlightBssid }),
}));
