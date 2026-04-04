import { useCallback, useEffect, useState } from 'react';
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
  listSessions,
  type FieldSessionRow,
} from '../services/fieldRecorder';
import { useWifiStore } from '../store/useWifiStore';

export function FieldMapScreen() {
  const { networks } = useWifiStore();
  const [accuracy, setAccuracy] = useState<number | null>(null);
  const [recording, setRecording] = useState(false);
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [sessionName, setSessionName] = useState('Walk');
  const [elapsed, setElapsed] = useState(0);
  const [pointCount, setPointCount] = useState(0);
  const [sessions, setSessions] = useState<FieldSessionRow[]>([]);
  const [loading, setLoading] = useState(true);

  const refreshSessions = useCallback(async () => {
    const rows = await listSessions();
    setSessions(rows);
  }, []);

  useEffect(() => {
    void refreshSessions().finally(() => setLoading(false));
  }, [refreshSessions]);

  useEffect(() => {
    let sub: Location.LocationSubscription | null = null;
    (async () => {
      const { status } = await Location.requestForegroundPermissionsAsync();
      if (status !== 'granted') return;
      sub = await Location.watchPositionAsync(
        { accuracy: Location.Accuracy.High, timeInterval: 2000, distanceInterval: 1.5 },
        (loc) => setAccuracy(loc.coords.accuracy ?? null)
      );
    })();
    return () => void sub?.remove();
  }, []);

  useEffect(() => {
    if (!recording) return;
    const t0 = Date.now();
    const id = setInterval(() => setElapsed(Math.floor((Date.now() - t0) / 1000)), 1000);
    return () => clearInterval(id);
  }, [recording]);

  useEffect(() => {
    if (!recording || sessionId == null) return;
    const id = setInterval(async () => {
      if (accuracy != null && accuracy > 5) return;
      const loc = await Location.getCurrentPositionAsync({
        accuracy: Location.Accuracy.High,
      });
      const p = loc.coords;
      await addFieldPoint(sessionId, {
        lat: p.latitude,
        lng: p.longitude,
        timestamp: Date.now(),
        networks: [...networks],
      });
      setPointCount((c) => c + 1);
    }, 3000);
    return () => clearInterval(id);
  }, [recording, sessionId, networks, accuracy]);

  const start = async () => {
    const id = await createSession(sessionName.trim() || 'Session');
    setSessionId(id);
    setPointCount(0);
    setElapsed(0);
    setRecording(true);
  };

  const stop = async () => {
    setRecording(false);
    setSessionId(null);
    await refreshSessions();
  };

  return (
    <View style={styles.root}>
      <Text style={styles.title}>Field Map</Text>
      <Text style={styles.sub}>Record GPS + WiFi for voxel heatmaps (Phase 4 visual)</Text>

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
        {accuracy != null && accuracy > 5 ? ' (wait for < 5 m to record)' : ''}
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

      <Text style={styles.section}>Saved sessions</Text>
      <ScrollView style={styles.list}>
        {sessions.map((s) => (
          <View key={s.id} style={styles.row}>
            <Text style={styles.rowName}>{s.name}</Text>
            <Text style={styles.rowMeta}>
              {new Date(s.created_at).toLocaleString()} · {s.point_count} pts · {s.network_count}{' '}
              nets
            </Text>
          </View>
        ))}
        {sessions.length === 0 ? <Text style={styles.empty}>No sessions yet.</Text> : null}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#0a0e14', padding: 16, paddingTop: 48 },
  title: { color: '#00ffe5', fontSize: 22, fontWeight: '800' },
  sub: { color: '#78909c', fontSize: 13, marginTop: 8, marginBottom: 16 },
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
    marginBottom: 20,
  },
  btnStop: { backgroundColor: '#c62828' },
  btnText: { color: '#fff', fontWeight: '800', fontSize: 16 },
  section: { color: '#90a4ae', fontWeight: '700', marginBottom: 8 },
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
