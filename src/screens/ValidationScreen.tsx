import { useCallback, useEffect, useRef, useState } from 'react';
import { Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import * as Location from 'expo-location';
import { SpinningCubeScene } from '../components/SpinningCubeScene';
import { WifiScanner } from '../services/wifiScanner';
import { useWifiStore } from '../store/useWifiStore';
import type { WifiNetwork } from '../types/wifi';

function bandLabel(freq: number): string {
  if (freq > 4000) return '5 GHz';
  if (freq >= 2400) return '2.4 GHz';
  return '—';
}

function signalBarWidth(rssi: number): `${number}%` {
  const t = Math.max(0, Math.min(1, (rssi + 100) / 70));
  return `${Math.round(t * 100)}%`;
}

function hexFromRgb(r: number, g: number, b: number): string {
  const to = (n: number) => n.toString(16).padStart(2, '0');
  return `#${to(r)}${to(g)}${to(b)}`;
}

export function ValidationScreen() {
  const scannerRef = useRef(new WifiScanner());
  const { networks, networkCount, strongestNetwork, lastScan, setFromScan, setScanning } =
    useWifiStore();
  const [fps, setFps] = useState<number | null>(null);
  const [showFps, setShowFps] = useState(false);
  const [tapCount, setTapCount] = useState(0);
  const tapTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const onFps = useCallback((v: number) => {
    setFps(v);
  }, []);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (Platform.OS === 'android') {
        const { status } = await Location.requestForegroundPermissionsAsync();
        if (status !== 'granted' && !cancelled) {
          setScanning(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [setScanning]);

  useEffect(() => {
    setScanning(true);
    scannerRef.current.start((nets) => {
      setFromScan(nets, Date.now());
    });
    return () => {
      scannerRef.current.stop();
      setScanning(false);
    };
  }, [setFromScan, setScanning]);

  const onTripleTapRegion = () => {
    setTapCount((c) => {
      const next = c + 1;
      if (tapTimerRef.current) clearTimeout(tapTimerRef.current);
      tapTimerRef.current = setTimeout(() => setTapCount(0), 400);
      if (next >= 3) {
        setShowFps((s) => !s);
        return 0;
      }
      return next;
    });
  };

  const formatTime = (ts: number) => {
    if (!ts) return '—';
    return new Date(ts).toLocaleTimeString();
  };

  return (
    <View style={styles.root}>
      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        <Pressable onPress={onTripleTapRegion}>
          <Text style={styles.title}>PHANTOM — WiFi Scanner</Text>
        </Pressable>
        <Text style={styles.sub}>
          {networkCount} networks detected
          {strongestNetwork ? ` · strongest ${strongestNetwork.ssid || '(hidden)'}` : ''}
        </Text>
        <Text style={styles.meta}>Last scan: {formatTime(lastScan)}</Text>
        {showFps ? (
          <Text style={styles.fps}>
            FPS: {fps != null ? fps.toFixed(0) : '…'} (triple-tap to hide)
          </Text>
        ) : (
          <Text style={styles.hint}>Triple-tap anywhere to toggle FPS</Text>
        )}

        <SpinningCubeScene height={160} onFps={onFps} />

        <Text style={styles.section}>Networks (strongest first)</Text>
        {networks.map((n: WifiNetwork) => (
          <NetworkRow key={n.bssid} net={n} scanner={scannerRef.current} />
        ))}
        {networks.length === 0 ? (
          <Text style={styles.empty}>
            No networks yet. Grant location (Android), enable Wi‑Fi, and wait for a scan.
          </Text>
        ) : null}
      </ScrollView>
    </View>
  );
}

function NetworkRow({ net, scanner }: { net: WifiNetwork; scanner: WifiScanner }) {
  const c = scanner.getColour(net.bssid, net.frequency);
  const hex = hexFromRgb(Math.round(c.r * 255), Math.round(c.g * 255), Math.round(c.b * 255));
  const dist = scanner.getDistance(net.rssi, net.frequency);

  return (
    <View style={styles.card}>
      <View style={styles.cardHead}>
        <View style={[styles.dot, { backgroundColor: hex }]} />
        <Text style={styles.ssid} numberOfLines={1}>
          {net.ssid || '(hidden)'}
        </Text>
      </View>
      <Text style={styles.line}>BSSID: {net.bssid}</Text>
      <Text style={styles.line}>
        Signal: {net.rssi} dBm → ~{dist.toFixed(1)} m
      </Text>
      <Text style={styles.line}>
        Band: {bandLabel(net.frequency)} · Ch: {net.channel}
      </Text>
      <View style={styles.barTrack}>
        <View style={[styles.barFill, { width: signalBarWidth(net.rssi) }]} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: '#0a0e14',
  },
  scroll: {
    paddingTop: 48,
    paddingBottom: 40,
    paddingHorizontal: 16,
  },
  title: {
    color: '#eceff1',
    fontSize: 20,
    fontWeight: '700',
    marginBottom: 8,
  },
  sub: {
    color: '#b0bec5',
    fontSize: 14,
    marginBottom: 4,
  },
  meta: {
    color: '#78909c',
    fontSize: 12,
    marginBottom: 8,
  },
  fps: {
    color: '#00ffe5',
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 12,
  },
  hint: {
    color: '#546e7a',
    fontSize: 11,
    marginBottom: 12,
  },
  section: {
    color: '#90a4ae',
    fontSize: 13,
    fontWeight: '600',
    marginTop: 20,
    marginBottom: 10,
  },
  empty: {
    color: '#78909c',
    fontSize: 13,
    lineHeight: 20,
    marginTop: 8,
  },
  card: {
    backgroundColor: '#121a22',
    borderRadius: 10,
    padding: 12,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: '#1e2a36',
  },
  cardHead: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 6,
  },
  dot: {
    width: 12,
    height: 12,
    borderRadius: 6,
  },
  ssid: {
    flex: 1,
    color: '#eceff1',
    fontSize: 16,
    fontWeight: '600',
  },
  line: {
    color: '#b0bec5',
    fontSize: 12,
    marginBottom: 2,
    fontVariant: ['tabular-nums'],
  },
  barTrack: {
    height: 6,
    backgroundColor: '#1c252e',
    borderRadius: 3,
    marginTop: 8,
    overflow: 'hidden',
  },
  barFill: {
    height: '100%',
    backgroundColor: '#00bcd4',
    borderRadius: 3,
  },
});
