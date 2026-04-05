import {
  Blur,
  Canvas,
  Circle,
  Group,
  matchFont,
  Text,
} from '@shopify/react-native-skia';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Dimensions,
  Modal,
  Platform,
  Pressable,
  StyleSheet,
  Text as RNText,
  View,
} from 'react-native';
import Animated, {
  Easing,
  useAnimatedStyle,
  useSharedValue,
  withRepeat,
  withSequence,
  withTiming,
} from 'react-native-reanimated';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import {
  bandColor,
  COLORS,
  estimateDistanceMeters,
} from '../constants/theme';
import { useCompassHeading } from '../hooks/useCompassHeading';
import { useSpectraLocation } from '../hooks/useSpectraLocation';
import { useWifiScan } from '../hooks/useWifiScan';
import { requestSpectraPermissions } from '../services/permissions';
import { uploadScanIfAccurate } from '../services/signalUpload';
import type { WifiNetwork } from '../types/wifi';

const { width: SW, height: SH } = Dimensions.get('window');
const METERS_TO_PX = 3.2;
const MAX_DISPLAY_M = 55;
const RING_MS = [10, 30, 50];

type NodeLayout = {
  bssid: string;
  ssid: string;
  x: number;
  y: number;
  r: number;
  color: string;
  rssi: number;
  freq: number;
  distM: number;
};

function smoothStep(from: number, to: number, t: number) {
  return from + (to - from) * t;
}

export function ZoneScreen() {
  const insets = useSafeAreaInsets();
  const [permsOk, setPermsOk] = useState(false);

  useEffect(() => {
    void (async () => {
      const p = await requestSpectraPermissions();
      setPermsOk(p.fine);
    })();
  }, []);
  const heading = useCompassHeading(permsOk);
  const loc = useSpectraLocation(permsOk);

  const { networks, scanning } = useWifiScan(permsOk, 3000, (nets) => {
    if (
      loc.lat != null &&
      loc.lng != null &&
      loc.accuracy != null &&
      loc.accuracy <= 15
    ) {
      void uploadScanIfAccurate(loc.lat, loc.lng, loc.accuracy, nets);
    }
  });

  const [smooth, setSmooth] = useState<Record<string, Partial<WifiNetwork>>>(
    {},
  );
  useEffect(() => {
    setSmooth((prev) => {
      const next: Record<string, Partial<WifiNetwork>> = { ...prev };
      for (const n of networks) {
        const p = next[n.bssid];
        if (!p || p.rssi === undefined) {
          next[n.bssid] = { ...n };
        } else {
          next[n.bssid] = {
            ...n,
            rssi: smoothStep(p.rssi!, n.rssi, 0.35),
          };
        }
      }
      return next;
    });
  }, [networks]);

  const displayList = useMemo(() => {
    return networks.map((n) => {
      const s = smooth[n.bssid];
      return {
        ...n,
        rssi: s?.rssi ?? n.rssi,
      };
    });
  }, [networks, smooth]);

  const sorted = useMemo(() => {
    return [...displayList].sort((a, b) => a.bssid.localeCompare(b.bssid));
  }, [displayList]);

  const layouts = useMemo(() => {
    const N = Math.max(sorted.length, 1);
    const items: NodeLayout[] = sorted.map((n, i) => {
      const distM = Math.min(estimateDistanceMeters(n.rssi), MAX_DISPLAY_M);
      const rPx = Math.min(28, Math.max(6, 8 + (n.rssi + 100) * 0.35));
      const angle = (2 * Math.PI * i) / N;
      const distPx = distM * METERS_TO_PX;
      const x = SW / 2 + distPx * Math.sin(angle);
      const y = SH / 2 - distPx * Math.cos(angle);
      return {
        bssid: n.bssid,
        ssid: n.ssid,
        x,
        y,
        r: rPx,
        color: bandColor(n.frequency),
        rssi: n.rssi,
        freq: n.frequency,
        distM,
      };
    });
    return items;
  }, [sorted]);

  const [selected, setSelected] = useState<NodeLayout | null>(null);

  const hitTest = useCallback(
    (sx: number, sy: number) => {
      const cx = SW / 2;
      const cy = SH / 2;
      const hr = (heading * Math.PI) / 180;
      const dx = sx - cx;
      const dy = sy - cy;
      const lx = dx * Math.cos(hr) - dy * Math.sin(hr);
      const ly = dx * Math.sin(hr) + dy * Math.cos(hr);
      for (let i = layouts.length - 1; i >= 0; i--) {
        const L = layouts[i];
        const nx = L.x - cx;
        const ny = L.y - cy;
        const d = Math.hypot(lx - nx, ly - ny);
        if (d < L.r + 28) return L;
      }
      return null;
    },
    [heading, layouts],
  );

  const fontSmall = useMemo(
    () =>
      matchFont({
        fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace' }),
        fontSize: 10,
      }),
    [],
  );
  const fontLabel = useMemo(
    () =>
      matchFont({
        fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace' }),
        fontSize: 12,
      }),
    [],
  );

  const pulse = useSharedValue(1);
  useEffect(() => {
    pulse.value = withRepeat(
      withSequence(
        withTiming(0.35, { duration: 900, easing: Easing.inOut(Easing.ease) }),
        withTiming(1, { duration: 900, easing: Easing.inOut(Easing.ease) }),
      ),
      -1,
      true,
    );
  }, [pulse]);

  const scanPulseStyle = useAnimatedStyle(() => ({
    opacity: pulse.value,
  }));

  const headingRad = (-heading * Math.PI) / 180;

  return (
    <View style={styles.root}>
      <Canvas style={StyleSheet.absoluteFill}>
        <Group
          origin={{ x: SW / 2, y: SH / 2 }}
          transform={[{ rotate: headingRad }]}
        >
          {RING_MS.map((m) => (
            <Circle
              key={m}
              cx={SW / 2}
              cy={SH / 2}
              r={m * METERS_TO_PX}
              style="stroke"
              strokeWidth={1}
              color="rgba(0, 120, 60, 0.55)"
            />
          ))}
          {layouts.map((L) => (
            <Group key={L.bssid}>
              <Circle cx={L.x} cy={L.y} r={L.r + 12} color={L.color} opacity={0.22}>
                <Blur blur={14} mode="clamp" />
              </Circle>
              <Circle cx={L.x} cy={L.y} r={L.r + 4} color={L.color} opacity={0.45} />
              <Circle cx={L.x} cy={L.y} r={L.r} color={L.color} opacity={0.95} />
            </Group>
          ))}
          <Circle cx={SW / 2} cy={SH / 2} r={14} color="#FFFFFF" opacity={0.25}>
            <Blur blur={8} mode="clamp" />
          </Circle>
          <Circle cx={SW / 2} cy={SH / 2} r={6} color="#FFFFFF" />
          {fontLabel ? (
            <Text
              x={SW / 2 - 18}
              y={SH / 2 + 28}
              font={fontLabel}
              text="YOU"
              color={COLORS.white}
            />
          ) : null}
          {RING_MS.map((m) =>
            fontSmall ? (
              <Text
                key={`lbl-${m}`}
                x={SW / 2 + m * METERS_TO_PX - 12}
                y={SH / 2 + 4}
                font={fontSmall}
                text={`${m}m`}
                color="rgba(0, 200, 120, 0.75)"
              />
            ) : null,
          )}
          {layouts.map((L) =>
            fontSmall ? (
              <Text
                key={`t-${L.bssid}`}
                x={L.x - 72}
                y={L.y + L.r + 12}
                font={fontSmall}
                text={L.ssid.length > 22 ? `${L.ssid.slice(0, 22)}…` : L.ssid}
                color="rgba(0, 255, 200, 0.9)"
              />
            ) : null,
          )}
        </Group>
      </Canvas>

      <Pressable
        style={StyleSheet.absoluteFill}
        onPress={(e) => {
          const x = e.nativeEvent.locationX;
          const y = e.nativeEvent.locationY;
          setSelected(hitTest(x, y));
        }}
      />

      <View
        style={[styles.hudTop, { paddingTop: insets.top + 8 }]}
        pointerEvents="none"
      >
        <RNText style={styles.hudTitle}>SPECTRA</RNText>
        <RNText style={styles.hudRight}>
          {sorted.length} SIGNAL{sorted.length === 1 ? '' : 'S'}
        </RNText>
      </View>

      <View
        style={[styles.hudBottom, { paddingBottom: insets.bottom + 8 }]}
        pointerEvents="none"
      >
        <RNText style={styles.hudMeta}>
          GPS ± {loc.accuracy != null ? `${loc.accuracy.toFixed(0)}m` : '—'}
        </RNText>
        <Animated.Text style={[styles.hudScan, scanPulseStyle]}>
          {scanning ? 'SCANNING…' : 'SCAN…'}
        </Animated.Text>
      </View>

      <Modal
        visible={selected != null}
        transparent
        animationType="slide"
        onRequestClose={() => setSelected(null)}
      >
        <Pressable style={styles.sheetBackdrop} onPress={() => setSelected(null)}>
          <Pressable style={styles.sheet} onPress={(e) => e.stopPropagation()}>
            {selected ? (
              <>
                <RNText style={styles.sheetTitle}>{selected.ssid}</RNText>
                <RNText style={styles.sheetLine}>BSSID: {selected.bssid}</RNText>
                <RNText style={styles.sheetLine}>
                  Signal: {selected.rssi} dBm
                </RNText>
                <View style={styles.barTrack}>
                  <View
                    style={[
                      styles.barFill,
                      {
                        width: `${Math.min(
                          100,
                          Math.max(0, ((selected.rssi + 100) / 70) * 100),
                        )}%`,
                      },
                    ]}
                  />
                </View>
                <RNText style={styles.sheetLine}>
                  Est. distance: {selected.distM.toFixed(1)} m
                </RNText>
                <RNText style={styles.sheetLine}>
                  Freq: {selected.freq || '—'} MHz (
                  {selected.freq > 0
                    ? selected.freq < 3000
                      ? '2.4 GHz'
                      : '5 GHz'
                    : 'unknown'}
                  )
                </RNText>
                <Pressable style={styles.sheetBtn}>
                  <RNText style={styles.sheetBtnText}>MARK AS LOGGED</RNText>
                </Pressable>
              </>
            ) : null}
          </Pressable>
        </Pressable>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: COLORS.bg,
  },
  hudTop: {
    position: 'absolute',
    left: 16,
    right: 16,
    top: 0,
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  hudTitle: {
    color: COLORS.cyan,
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace' }),
    fontSize: 16,
    letterSpacing: 2,
  },
  hudRight: {
    color: COLORS.hud,
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace' }),
    fontSize: 13,
  },
  hudBottom: {
    position: 'absolute',
    left: 16,
    right: 16,
    bottom: 0,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
  },
  hudMeta: {
    color: 'rgba(0, 255, 200, 0.75)',
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace' }),
    fontSize: 11,
  },
  hudScan: {
    color: COLORS.magenta,
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace' }),
    fontSize: 12,
    letterSpacing: 1,
  },
  sheetBackdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.7)',
    justifyContent: 'flex-end',
  },
  sheet: {
    backgroundColor: '#050508',
    borderTopWidth: 1,
    borderColor: 'rgba(0,255,255,0.35)',
    padding: 20,
    paddingBottom: 32,
  },
  sheetTitle: {
    color: COLORS.cyan,
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace' }),
    fontSize: 16,
    marginBottom: 12,
  },
  sheetLine: {
    color: 'rgba(0, 255, 200, 0.85)',
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace' }),
    fontSize: 12,
    marginBottom: 8,
  },
  barTrack: {
    height: 8,
    backgroundColor: '#111',
    borderRadius: 4,
    marginBottom: 12,
    overflow: 'hidden',
  },
  barFill: {
    height: '100%',
    backgroundColor: COLORS.cyan,
  },
  sheetBtn: {
    marginTop: 16,
    borderWidth: 1,
    borderColor: COLORS.cyan,
    paddingVertical: 12,
    alignItems: 'center',
  },
  sheetBtnText: {
    color: COLORS.cyan,
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace' }),
    fontSize: 13,
    letterSpacing: 1,
  },
});
