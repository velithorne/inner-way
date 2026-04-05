import firestore, {
  FirebaseFirestoreTypes,
} from '@react-native-firebase/firestore';
import AsyncStorage from '@react-native-async-storage/async-storage';
import NetInfo from '@react-native-community/netinfo';
import { getSessionId } from '../session/sessionId';
import type { WifiNetwork } from '../types/wifi';

const QUEUE_KEY = '@spectra_upload_queue';
const BATCH_MAX = 450;

type QueuedDoc = {
  lat: number;
  lng: number;
  accuracy: number;
  ssid: string;
  bssid: string;
  rssi: number;
  frequency: number;
  timestamp: number;
  sessionId: string;
};

function toFirestorePayload(
  doc: QueuedDoc,
): FirebaseFirestoreTypes.DocumentData {
  return {
    lat: doc.lat,
    lng: doc.lng,
    accuracy: doc.accuracy,
    ssid: doc.ssid,
    bssid: doc.bssid,
    rssi: doc.rssi,
    frequency: doc.frequency,
    timestamp: firestore.Timestamp.fromMillis(doc.timestamp),
    sessionId: doc.sessionId,
  };
}

async function loadQueue(): Promise<QueuedDoc[]> {
  try {
    const raw = await AsyncStorage.getItem(QUEUE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw) as QueuedDoc[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

async function saveQueue(items: QueuedDoc[]): Promise<void> {
  await AsyncStorage.setItem(QUEUE_KEY, JSON.stringify(items));
}

async function commitBatches(docs: QueuedDoc[]): Promise<void> {
  const coll = firestore().collection('signals');
  for (let i = 0; i < docs.length; i += BATCH_MAX) {
    const slice = docs.slice(i, i + BATCH_MAX);
    const batch = firestore().batch();
    for (const doc of slice) {
      batch.set(coll.doc(), toFirestorePayload(doc));
    }
    await batch.commit();
  }
}

export async function flushUploadQueue(): Promise<void> {
  const net = await NetInfo.fetch();
  if (!net.isConnected) return;

  const queue = await loadQueue();
  if (queue.length === 0) return;

  try {
    await commitBatches(queue);
    await saveQueue([]);
  } catch {
    // leave queue intact for next attempt
  }
}

/**
 * Batch-upload one document per network when accuracy < threshold.
 * Queues to AsyncStorage if offline or commit fails.
 */
export async function uploadScanIfAccurate(
  lat: number,
  lng: number,
  accuracy: number | null,
  networks: WifiNetwork[],
  accuracyThresholdM = 15,
): Promise<void> {
  if (accuracy == null || accuracy > accuracyThresholdM || networks.length === 0) {
    return;
  }

  const sessionId = getSessionId();
  const now = Date.now();
  const docs: QueuedDoc[] = networks.map((n) => ({
    lat,
    lng,
    accuracy,
    ssid: n.ssid,
    bssid: n.bssid,
    rssi: n.rssi,
    frequency: n.frequency,
    timestamp: now,
    sessionId,
  }));

  const net = await NetInfo.fetch();
  if (!net.isConnected) {
    const prev = await loadQueue();
    await saveQueue([...prev, ...docs]);
    return;
  }

  try {
    await commitBatches(docs);
  } catch {
    const prev = await loadQueue();
    await saveQueue([...prev, ...docs]);
  }
}

NetInfo.addEventListener((state) => {
  if (state.isConnected) {
    void flushUploadQueue();
  }
});
