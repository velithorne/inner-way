import { fetchDeviceSnapshot, isSystemDataAvailable } from '../native/systemData';
import type { DeviceSnapshot } from '../store/useWorldStore';
import { useHistoryStore } from '../store/useHistoryStore';
import { useWorldStore } from '../store/useWorldStore';

let timer: ReturnType<typeof setInterval> | null = null;

function formatNativeError(e: unknown): string {
  if (e instanceof Error && e.message) return e.message;
  if (typeof e === 'object' && e !== null) {
    const o = e as { message?: string; userInfo?: { NSLocalizedDescription?: string } };
    if (o.message) return o.message;
    if (o.userInfo?.NSLocalizedDescription) return o.userInfo.NSLocalizedDescription;
  }
  return String(e);
}

export function startPolling(): void {
  if (!isSystemDataAvailable()) return;
  if (timer != null) return;
  useWorldStore.getState().setTelemetryLoading();
  void tick();
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
  } catch (e) {
    useWorldStore.getState().setTelemetryError(formatNativeError(e));
  }
}
