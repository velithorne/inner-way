import { fetchDeviceSnapshot, isSystemDataAvailable } from '../native/systemData';
import type { DeviceSnapshot } from '../store/useWorldStore';
import { useHistoryStore } from '../store/useHistoryStore';
import { useWorldStore } from '../store/useWorldStore';

let timer: ReturnType<typeof setInterval> | null = null;

export function startPolling(): void {
  if (!isSystemDataAvailable()) return;
  if (timer != null) return;
  timer = setInterval(() => {
    void tick();
  }, 500);
}

export function stopPolling(): void {
  if (timer != null) {
    clearInterval(timer);
    timer = null;
  }
}

async function tick(): Promise<void> {
  try {
    const data = await fetchDeviceSnapshot();
    const snapshot: DeviceSnapshot = {
      timestamp: Date.now(),
      ...data,
    };
    useWorldStore.getState().setFromSnapshot(snapshot);
    useHistoryStore.getState().pushSnapshot(snapshot);
  } catch {
    // Native errors are surfaced in UI via missing snapshot updates
  }
}
