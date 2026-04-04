import { useCallback, useMemo, useRef, useState } from 'react';
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import * as THREE from 'three';
import { Camera, useCameraDevice, useCameraPermission } from 'react-native-vision-camera';
import { findChannelConflicts } from '../services/channelConflicts';
import { rssiToDistance } from '../services/rssiToDistance';
import { networkColour } from '../services/colourFromBssid';
import { useSignalStore } from '../store/useSignalStore';
import { useWifiStore } from '../store/useWifiStore';
import { estimateToWorldPosition } from '../world/space';
import { WaveScene } from '../world/WaveScene';

function signalBarWidth(rssi: number): `${number}%` {
  const t = Math.max(0, Math.min(1, (rssi + 100) / 70));
  return `${Math.round(t * 100)}%`;
}

export function SignalScreen() {
  const device = useCameraDevice('back');
  const { hasPermission, requestPermission } = useCameraPermission();
  const { networks, networkCount, routerEstimates } = useWifiStore();
  const { highlightBssid, setHighlightBssid } = useSignalStore();
  const [showFps, setShowFps] = useState(false);
  const [fps, setFps] = useState<number | null>(null);
  const tapCount = useRef(0);
  const tapTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const estimatesMap = useMemo(() => {
    const m = new Map<string, (typeof routerEstimates)[string]>();
    for (const n of networks) {
      const e = routerEstimates[n.bssid];
      if (e) m.set(n.bssid, e);
    }
    return m;
  }, [networks, routerEstimates]);

  const conflicts = useMemo(() => findChannelConflicts(networks), [networks]);

  const conflictMidpoints = useMemo(() => {
    const out: THREE.Vector3[] = [];
    for (const c of conflicts.slice(0, 6)) {
      const ea = estimatesMap.get(c.a.bssid);
      const eb = estimatesMap.get(c.b.bssid);
      if (!ea || !eb) continue;
      const pA = estimateToWorldPosition(ea, c.a.frequency);
      const pB = estimateToWorldPosition(eb, c.b.frequency);
      out.push(pA.clone().add(pB).multiplyScalar(0.5));
    }
    return out;
  }, [conflicts, estimatesMap]);

  const onFps = useCallback((v: number) => {
    setFps(v);
  }, []);

  const onTripleTapHeader = () => {
    tapCount.current += 1;
    if (tapTimer.current) clearTimeout(tapTimer.current);
    tapTimer.current = setTimeout(() => {
      tapCount.current = 0;
    }, 450);
    if (tapCount.current >= 3) {
      setShowFps((s) => !s);
      tapCount.current = 0;
    }
  };

  const visibleNets = useMemo(
    () => [...networks].sort((a, b) => b.rssi - a.rssi).slice(0, 12),
    [networks]
  );

  if (!hasPermission) {
    return (
      <View style={styles.center}>
        <Text style={styles.msg}>Camera access is required for the AR wave field.</Text>
        <Pressable style={styles.btn} onPress={() => void requestPermission()}>
          <Text style={styles.btnText}>Grant camera</Text>
        </Pressable>
      </View>
    );
  }

  if (device == null) {
    return (
      <View style={styles.center}>
        <Text style={styles.msg}>No camera device found.</Text>
      </View>
    );
  }

  return (
    <View style={styles.root}>
      <Camera style={StyleSheet.absoluteFill} device={device} isActive />
      <WaveScene
        networks={networks}
        estimates={estimatesMap}
        highlightBssid={highlightBssid}
        conflictMidpoints={conflictMidpoints}
        onFps={onFps}
      />
      <Pressable style={styles.hud} onPress={onTripleTapHeader}>
        <Text style={styles.hudTitle}>SIGNAL</Text>
        <Text style={styles.hudSub}>{networkCount} networks · spherical wavefronts</Text>
        {showFps ? (
          <Text style={styles.fps}>FPS {fps != null ? fps.toFixed(0) : '…'}</Text>
        ) : (
          <Text style={styles.hint}>Triple-tap for FPS</Text>
        )}
      </Pressable>

      {conflicts.length > 0 ? (
        <View style={styles.conflictBadge}>
          <Text style={styles.conflictText}>⚡ CHANNEL CONFLICT DETECTED</Text>
          <Text style={styles.conflictDetail} numberOfLines={2}>
            {conflicts[0].a.ssid || conflicts[0].a.bssid} & {conflicts[0].b.ssid || conflicts[0].b.bssid} · Ch{' '}
            {conflicts[0].channel}
          </Text>
        </View>
      ) : null}

      <View style={styles.legend}>
        <Text style={styles.legendTitle}>Networks</Text>
        <ScrollView style={styles.legendScroll} nestedScrollEnabled>
          {visibleNets.map((n) => {
            const c = networkColour(n.bssid, n.frequency);
            const hex = `#${[c.r, c.g, c.b].map((x) => Math.round(x * 255).toString(16).padStart(2, '0')).join('')}`;
            const est = routerEstimates[n.bssid];
            const dist = est ? rssiToDistance(n.rssi, n.frequency).toFixed(0) : '—';
            return (
              <Pressable
                key={n.bssid}
                style={[styles.legendRow, highlightBssid === n.bssid && styles.legendRowHi]}
                onPress={() =>
                  setHighlightBssid(highlightBssid === n.bssid ? null : n.bssid)
                }
              >
                <View style={[styles.dot, { backgroundColor: hex }]} />
                <Text style={styles.legendSsid} numberOfLines={1}>
                  {n.ssid || '(hidden)'}
                </Text>
                <View style={styles.barTrack}>
                  <View style={[styles.barFill, { width: signalBarWidth(n.rssi) }]} />
                </View>
                <Text style={styles.legendMeta}>{n.rssi} · ~{dist}m</Text>
              </Pressable>
            );
          })}
        </ScrollView>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#000' },
  center: {
    flex: 1,
    backgroundColor: '#0a0e14',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  msg: {
    color: '#b0bec5',
    fontSize: 15,
    textAlign: 'center',
    marginBottom: 16,
  },
  btn: {
    backgroundColor: '#00838f',
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 10,
  },
  btnText: { color: '#fff', fontWeight: '700' },
  hud: {
    position: 'absolute',
    top: 48,
    left: 16,
    right: 120,
    padding: 12,
    backgroundColor: 'rgba(10, 14, 20, 0.72)',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: 'rgba(0, 255, 229, 0.25)',
  },
  hudTitle: {
    color: '#00ffe5',
    fontSize: 18,
    fontWeight: '800',
    letterSpacing: 2,
  },
  hudSub: {
    color: '#90a4ae',
    fontSize: 12,
    marginTop: 4,
  },
  fps: {
    color: '#00ffe5',
    fontSize: 13,
    fontWeight: '600',
    marginTop: 6,
  },
  hint: {
    color: '#546e7a',
    fontSize: 11,
    marginTop: 6,
  },
  conflictBadge: {
    position: 'absolute',
    top: 160,
    left: 16,
    right: 16,
    backgroundColor: 'rgba(80, 40, 0, 0.85)',
    padding: 10,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#ff9800',
  },
  conflictText: { color: '#ffcc80', fontWeight: '700', fontSize: 12 },
  conflictDetail: { color: '#ffe0b2', fontSize: 11, marginTop: 4 },
  legend: {
    position: 'absolute',
    bottom: 88,
    right: 8,
    width: 200,
    maxHeight: 220,
    backgroundColor: 'rgba(10, 14, 20, 0.88)',
    borderRadius: 10,
    padding: 8,
    borderWidth: 1,
    borderColor: '#1e2a36',
  },
  legendTitle: {
    color: '#00ffe5',
    fontSize: 11,
    fontWeight: '700',
    marginBottom: 6,
  },
  legendScroll: { maxHeight: 180 },
  legendRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 6,
    gap: 6,
  },
  legendRowHi: { opacity: 1, backgroundColor: 'rgba(0,255,229,0.12)', borderRadius: 4 },
  dot: { width: 8, height: 8, borderRadius: 4 },
  legendSsid: { flex: 1, color: '#eceff1', fontSize: 10 },
  barTrack: {
    width: 40,
    height: 4,
    backgroundColor: '#1c252e',
    borderRadius: 2,
    overflow: 'hidden',
  },
  barFill: { height: '100%', backgroundColor: '#00bcd4' },
  legendMeta: { color: '#78909c', fontSize: 9, width: 52, textAlign: 'right' },
});
