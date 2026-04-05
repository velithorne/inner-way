import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import * as Location from 'expo-location';
import {
  addFieldPoint,
  createSession,
  getSessionMeta,
  getSessionPoints,
  listSessions,
  type FieldPointRow,
  type FieldSessionRow,
} from '../services/fieldRecorder';
import { useWifiStore } from '../store/useWifiStore';
import { haversineM } from '../utils/geo';
import { FieldMapScene } from '../world/FieldMapScene';
import {
  buildVoxels,
  computeSessionSummary,
  findDeadZones,
  pointsToLocal,
  uniqueSsidsFromPoints,
} from '../world/fieldMapHeatmap';

type RawPoint = {
  lat: number;
  lng: number;
  timestamp: number;
  networks: import('../types/wifi').WifiNetwork[];
  low_precision: boolean;
};

export function FieldMapScreen() {
  useWifiStore();
  const [accuracy, setAccuracy] = useState<number | null>(null);
  const [recording, setRecording] = useState(false);
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [sessionName, setSessionName] = useState('Walk');
  const [elapsed, setElapsed] = useState(0);
  const [pointCount, setPointCount] = useState(0);
  const [sessions, setSessions] = useState<FieldSessionRow[]>([]);
  const [loading, setLoading] = useState(true);

  const [indoorMode, setIndoorMode] = useState(false);
  const [view3d, setView3d] = useState(false);
  const [ssidFilter, setSsidFilter] = useState<string | null>(null);

  /** Live points while recording */
  const [liveRawPoints, setLiveRawPoints] = useState<RawPoint[]>([]);
  /** Replay: loaded session */
  const [replaySessionId, setReplaySessionId] = useState<number | null>(null);
  const [replayMeta, setReplayMeta] = useState<{ name: string; created_at: number } | null>(null);
  const [replayPoints, setReplayPoints] = useState<FieldPointRow[]>([]);
  const [recordStartedAt, setRecordStartedAt] = useState<number | null>(null);

  /** Dedupe rapid fires from watch + intervals sampling the same spot */
  const lastFieldSampleRef = useRef<{ lat: number; lng: number; t: number } | null>(null);
  const sessionIdRef = useRef<number | null>(null);
  const recordingRef = useRef(false);
  const indoorModeRef = useRef(indoorMode);
  indoorModeRef.current = indoorMode;
  sessionIdRef.current = sessionId;
  recordingRef.current = recording;

  const refreshSessions = useCallback(async () => {
    const rows = await listSessions();
    setSessions(rows);
  }, []);

  useEffect(() => {
    void refreshSessions().finally(() => setLoading(false));
  }, [refreshSessions]);

  useEffect(() => {
    if (!recording) return;
    const t0 = Date.now();
    const id = setInterval(() => setElapsed(Math.floor((Date.now() - t0) / 1000)), 1000);
    return () => clearInterval(id);
  }, [recording]);

  const tryCaptureFieldPoint = useCallback(
    async (
      coords: { latitude: number; longitude: number; accuracy: number | null | undefined },
      source: string
    ) => {
      const sid = sessionIdRef.current;
      if (sid == null) {
        console.log('[FieldMap] tryCapture skipped — no sessionId', { source });
        return;
      }

      const acc = coords.accuracy;
      const maxAcc = indoorModeRef.current ? 15 : 5;
      if (acc != null && acc > maxAcc) {
        console.log('[FieldMap] Point REJECTED because:', `${acc.toFixed(1)} m > ${maxAcc} m (${source})`);
        return;
      }

      const now = Date.now();
      const last = lastFieldSampleRef.current;
      if (last) {
        const dt = now - last.t;
        const dist = haversineM(
          { lat: last.lat, lng: last.lng },
          { lat: coords.latitude, lng: coords.longitude }
        );
        if (dt < 4000 && dist < 2) {
          console.log('[FieldMap] duplicate sample skipped', { source, dtMs: dt, distM: dist.toFixed(1) });
          return;
        }
      }

      const nets = useWifiStore.getState().networks;
      console.log('[FieldMap] recordPoint called', {
        lat: coords.latitude,
        lng: coords.longitude,
        accuracy: acc,
        networkCount: nets.length,
        isRecording: true,
        source,
      });

      const lowPrecision = indoorModeRef.current && (acc == null || acc > 5);

      await addFieldPoint(
        sid,
        {
          lat: coords.latitude,
          lng: coords.longitude,
          timestamp: now,
          networks: nets,
        },
        { lowPrecision }
      );

      lastFieldSampleRef.current = {
        lat: coords.latitude,
        lng: coords.longitude,
        t: now,
      };

      setPointCount((c) => c + 1);
      setLiveRawPoints((prev) => [
        ...prev,
        {
          lat: coords.latitude,
          lng: coords.longitude,
          timestamp: now,
          networks: [...nets],
          low_precision: lowPrecision,
        },
      ]);
    },
    []
  );

  /** One Location.watch for HUD + captures while recording (avoids duplicate subscriptions). */
  useEffect(() => {
    let sub: Location.LocationSubscription | null = null;
    (async () => {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') return;
      sub = await Location.watchPositionAsync(
        {
          accuracy: Location.Accuracy.High,
          timeInterval: 5000,
          distanceInterval: 1,
        },
        (loc) => {
          setAccuracy(loc.coords.accuracy ?? null);
          if (recordingRef.current && sessionIdRef.current != null) {
            void tryCaptureFieldPoint(loc.coords, 'watchPosition');
          }
        }
      );
    })();
    return () => void sub?.remove();
  }, [tryCaptureFieldPoint]);

  /** Time-based fallbacks: 3s + 8s getCurrentPosition (live accuracy per sample). */
  useEffect(() => {
    if (!recording || sessionId == null) {
      lastFieldSampleRef.current = null;
      return;
    }

    void (async () => {
      const loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High });
      await tryCaptureFieldPoint(loc.coords, 'start-immediate');
    })();

    const id3 = setInterval(() => {
      void (async () => {
        const loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High });
        await tryCaptureFieldPoint(loc.coords, 'interval-3s');
      })();
    }, 3000);

    const id8 = setInterval(() => {
      void (async () => {
        const loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.High });
        await tryCaptureFieldPoint(loc.coords, 'interval-8s');
      })();
    }, 8000);

    return () => {
      clearInterval(id3);
      clearInterval(id8);
    };
  }, [recording, sessionId, tryCaptureFieldPoint]);

  const activeRawPoints = recording ? liveRawPoints : replayPoints.map(rowToRaw);

  const { localPoints, voxels, deadZones, summary } = useMemo(() => {
    const local = pointsToLocal(activeRawPoints, ssidFilter);
    const vox = buildVoxels(local);
    const dead = findDeadZones(vox);
    const sum = computeSessionSummary(local, vox, dead);
    return { localPoints: local, voxels: vox, deadZones: dead, summary: sum };
  }, [activeRawPoints, ssidFilter]);

  const ssidOptions = useMemo(() => uniqueSsidsFromPoints(activeRawPoints), [activeRawPoints]);

  const showScene = recording || replaySessionId != null;

  const start = async () => {
    setReplaySessionId(null);
    setReplayPoints([]);
    setReplayMeta(null);
    setLiveRawPoints([]);
    setSsidFilter(null);
    const id = await createSession(sessionName.trim() || 'Session');
    const t = Date.now();
    setRecordStartedAt(t);
    setSessionId(id);
    setPointCount(0);
    setElapsed(0);
    setRecording(true);
  };

  const stop = async () => {
    setRecording(false);
    setSessionId(null);
    setLiveRawPoints([]);
    setRecordStartedAt(null);
    await refreshSessions();
  };

  const openSession = async (s: FieldSessionRow) => {
    setRecording(false);
    setSessionId(null);
    setLiveRawPoints([]);
    setReplaySessionId(s.id);
    setRecordStartedAt(null);
    setSsidFilter(null);
    const meta = await getSessionMeta(s.id);
    setReplayMeta(meta ? { name: meta.name, created_at: meta.created_at } : null);
    const pts = await getSessionPoints(s.id);
    setReplayPoints(pts);
  };

  const closeReplay = () => {
    setReplaySessionId(null);
    setReplayPoints([]);
    setReplayMeta(null);
    setRecordStartedAt(null);
  };

  const accHint =
    accuracy == null
      ? '—'
      : indoorMode
        ? accuracy > 15
          ? ` (need ≤ 15 m — wait for better fix)`
          : accuracy > 5
            ? ' (indoor — reduced precision)'
            : ''
        : accuracy > 5
          ? ' (wait for < 5 m to record)'
          : '';

  return (
    <View style={styles.root}>
      <ScrollView style={styles.scroll} contentContainerStyle={styles.scrollContent}>
        <Text style={styles.title}>Field Map</Text>
        <Text style={styles.sub}>Record GPS + WiFi — voxel heatmap of signal strength</Text>

        <View style={styles.modeRow}>
          <Pressable
            style={[styles.modeBtn, !indoorMode && styles.modeBtnOn]}
            onPress={() => setIndoorMode(false)}
          >
            <Text style={[styles.modeTxt, !indoorMode && styles.modeTxtOn]}>OUTDOOR</Text>
          </Pressable>
          <Pressable
            style={[styles.modeBtn, indoorMode && styles.modeBtnOn]}
            onPress={() => setIndoorMode(true)}
          >
            <Text style={[styles.modeTxt, indoorMode && styles.modeTxtOn]}>INDOOR</Text>
          </Pressable>
        </View>
        {indoorMode ? (
          <Text style={styles.warn}>Indoor GPS — reduced accuracy; points may be imprecise.</Text>
        ) : null}

        <TextInput
          style={styles.input}
          placeholder="Session name"
          placeholderTextColor="#546e7a"
          value={sessionName}
          onChangeText={setSessionName}
          editable={!recording}
        />

        <Text style={styles.meta}>
          GPS accuracy: {accuracy != null ? `${accuracy.toFixed(1)} m` : '—'}
          {accHint}
        </Text>

        {recording ? (
          <View style={styles.recording}>
            <Text style={styles.recDot}>● REC</Text>
            <Text style={styles.recTime}>
              {Math.floor(elapsed / 60)}:{String(elapsed % 60).padStart(2, '0')}
            </Text>
            <Text style={styles.points}>{pointCount} points recorded</Text>
          </View>
        ) : null}

        <Pressable
          style={[styles.btn, recording && styles.btnStop]}
          onPress={recording ? stop : start}
          disabled={loading}
        >
          {loading ? (
            <ActivityIndicator color="#fff" />
          ) : (
            <Text style={styles.btnText}>{recording ? 'STOP + SAVE' : 'START MAPPING'}</Text>
          )}
        </Pressable>

        {showScene ? (
          <>
            <View style={styles.filterRow}>
              <Pressable
                style={[styles.filterChip, ssidFilter == null && styles.filterChipOn]}
                onPress={() => setSsidFilter(null)}
                disabled={recording}
              >
                <Text style={styles.filterChipTxt}>ALL</Text>
              </Pressable>
              {ssidOptions.map((ssid) => (
                <Pressable
                  key={ssid}
                  style={[styles.filterChip, ssidFilter === ssid && styles.filterChipOn]}
                  onPress={() => setSsidFilter(ssid === ssidFilter ? null : ssid)}
                  disabled={recording}
                >
                  <Text style={styles.filterChipTxt} numberOfLines={1}>
                    {ssid.length > 14 ? `${ssid.slice(0, 12)}…` : ssid}
                  </Text>
                </Pressable>
              ))}
            </View>

            <View style={styles.viewToggle}>
              <Pressable
                style={[styles.viewBtn, !view3d && styles.viewBtnOn]}
                onPress={() => setView3d(false)}
              >
                <Text style={styles.viewBtnTxt}>PLAN</Text>
              </Pressable>
              <Pressable
                style={[styles.viewBtn, view3d && styles.viewBtnOn]}
                onPress={() => setView3d(true)}
              >
                <Text style={styles.viewBtnTxt}>3D</Text>
              </Pressable>
            </View>

            <View style={styles.sceneLabels}>
              <Text style={styles.startLabel}>START</Text>
              {replaySessionId != null ? (
                <Pressable onPress={closeReplay}>
                  <Text style={styles.closeReplay}>Close replay</Text>
                </Pressable>
              ) : null}
            </View>

            <FieldMapScene
              voxels={voxels}
              deadZones={deadZones}
              view3d={view3d}
              liveMode={recording}
            />
            {deadZones.length > 0 ? (
              <Text style={styles.deadLegend}>
                Red outline = DEAD ZONE (weak pocket surrounded by stronger cells)
              </Text>
            ) : null}

            <View style={styles.summary}>
              <Text style={styles.summaryLine}>
                {replayMeta?.name ?? sessionName} ·{' '}
                {new Date(
                  replayMeta?.created_at ?? recordStartedAt ?? Date.now()
                ).toLocaleDateString()}
              </Text>
              <Text style={styles.summaryLine}>
                {summary.pointCount} points · ~{summary.pathMetres.toFixed(0)} m covered
              </Text>
              <Text style={styles.summaryLine}>
                Signal range: {summary.minDbm.toFixed(0)} to {summary.maxDbm.toFixed(0)} dBm
                {ssidFilter ? ` (${ssidFilter})` : ''}
              </Text>
              {summary.strongest ? (
                <Text style={styles.summaryLine}>
                  Strongest: {summary.strongest.ssid} at Δx {summary.strongest.offsetXM.toFixed(0)} m,
                  Δz {summary.strongest.offsetZM.toFixed(0)} m
                </Text>
              ) : (
                <Text style={styles.summaryLine}>Strongest: —</Text>
              )}
              {summary.weakestVoxel ? (
                <Text style={styles.summaryLine}>
                  Weakest zone: {summary.weakestVoxel.energyDbm.toFixed(0)} dBm at Δx{' '}
                  {summary.weakestVoxel.offsetXM.toFixed(0)} m, Δz {summary.weakestVoxel.offsetZM.toFixed(0)}{' '}
                  m
                </Text>
              ) : null}
              <Text style={styles.summaryLine}>Dead zones (heuristic): {summary.deadZoneCount}</Text>
            </View>
          </>
        ) : null}

        <Text style={styles.section}>Saved sessions</Text>
        {sessions.map((s) => (
          <Pressable key={s.id} style={styles.row} onPress={() => void openSession(s)}>
            <Text style={styles.rowName}>{s.name}</Text>
            <Text style={styles.rowMeta}>
              {new Date(s.created_at).toLocaleString()} · {s.point_count} pts · {s.network_count} nets
            </Text>
          </Pressable>
        ))}
        {sessions.length === 0 ? <Text style={styles.empty}>No sessions yet.</Text> : null}
      </ScrollView>
    </View>
  );
}

function rowToRaw(r: FieldPointRow): RawPoint {
  return {
    lat: r.lat,
    lng: r.lng,
    timestamp: r.timestamp,
    networks: r.networks,
    low_precision: r.low_precision,
  };
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#0a0e14' },
  scroll: { flex: 1 },
  scrollContent: { padding: 16, paddingTop: 48, paddingBottom: 32 },
  title: { color: '#00ffe5', fontSize: 22, fontWeight: '800' },
  sub: { color: '#78909c', fontSize: 13, marginTop: 8, marginBottom: 12 },
  modeRow: { flexDirection: 'row', gap: 8, marginBottom: 8 },
  modeBtn: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: '#121a22',
    borderWidth: 1,
    borderColor: '#1e2a36',
  },
  modeBtnOn: { borderColor: '#00ffe5', backgroundColor: '#0d2528' },
  modeTxt: { color: '#78909c', fontWeight: '700', fontSize: 12 },
  modeTxtOn: { color: '#00ffe5' },
  warn: { color: '#ffb74d', fontSize: 12, marginBottom: 10 },
  input: {
    backgroundColor: '#121a22',
    borderRadius: 10,
    padding: 12,
    color: '#eceff1',
    marginBottom: 12,
    borderWidth: 1,
    borderColor: '#1e2a36',
  },
  meta: { color: '#90a4ae', fontSize: 12, marginBottom: 12 },
  recording: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    marginBottom: 12,
  },
  recDot: { color: '#f44336', fontWeight: '800' },
  recTime: { color: '#eceff1', fontVariant: ['tabular-nums'] },
  points: { color: '#b0bec5' },
  btn: {
    backgroundColor: '#00838f',
    paddingVertical: 16,
    borderRadius: 12,
    alignItems: 'center',
    marginBottom: 16,
  },
  btnStop: { backgroundColor: '#c62828' },
  btnText: { color: '#fff', fontWeight: '800', fontSize: 16 },
  filterRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 6, marginBottom: 8 },
  filterChip: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 8,
    backgroundColor: '#121a22',
    borderWidth: 1,
    borderColor: '#2a3a4a',
    maxWidth: 140,
  },
  filterChipOn: { borderColor: '#00ffe5' },
  filterChipTxt: { color: '#b0bec5', fontSize: 11, fontWeight: '600' },
  viewToggle: { flexDirection: 'row', gap: 8, marginBottom: 6 },
  viewBtn: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 8,
    backgroundColor: '#121a22',
    borderWidth: 1,
    borderColor: '#1e2a36',
  },
  viewBtnOn: { borderColor: '#00ffe5' },
  viewBtnTxt: { color: '#eceff1', fontWeight: '700', fontSize: 12 },
  sceneLabels: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  startLabel: { color: '#00ffe5', fontSize: 11, fontWeight: '700' },
  closeReplay: { color: '#78909c', fontSize: 12 },
  deadLegend: { color: '#ff6655', fontSize: 11, marginTop: 6, marginBottom: 4 },
  summary: { marginTop: 12, gap: 4 },
  summaryLine: { color: '#b0bec5', fontSize: 12, lineHeight: 18 },
  section: { color: '#90a4ae', fontWeight: '700', marginTop: 8, marginBottom: 8 },
  list: { flex: 1 },
  row: {
    backgroundColor: '#121a22',
    padding: 12,
    borderRadius: 10,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: '#1e2a36',
  },
  rowName: { color: '#eceff1', fontWeight: '600' },
  rowMeta: { color: '#78909c', fontSize: 11, marginTop: 4 },
  empty: { color: '#546e7a', fontSize: 13 },
});
