import { create } from 'zustand';

import type { DeviceSnapshot } from './useWorldStore';

/** Rolling buffer for Phase 1; full 24h @ 2Hz in SQLite is a later milestone. */
const MAX_SNAPSHOTS = 500;

type HistoryState = {
  snapshots: DeviceSnapshot[];
  pushSnapshot: (s: DeviceSnapshot) => void;
  clear: () => void;
};

export const useHistoryStore = create<HistoryState>((set, get) => ({
  snapshots: [],
  pushSnapshot: (s) => {
    const next = [...get().snapshots, s];
    if (next.length > MAX_SNAPSHOTS) {
      next.splice(0, next.length - MAX_SNAPSHOTS);
    }
    set({ snapshots: next });
  },
  clear: () => set({ snapshots: [] }),
}));
