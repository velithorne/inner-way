/**
 * SensorHUD — floating dark glass sensor panel docked to top of map.
 *
 * Performance notes:
 *  - Throttled to ~4Hz display update via a local interval that reads from
 *    Zustand via getState() (no React subscription = no 60Hz re-renders)
 *  - Anomaly events detected via a separate subscribe callback
 *  - UTC clock runs on its own 1-second interval
 *  - Reanimated styles do NOT read React state — they use shared values only
 */

import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Platform,
  LayoutAnimation,
  UIManager,
} from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withSequence,
  withTiming,
  Easing,
} from 'react-native-reanimated';
import { useFieldStore } from '../../store/useFieldStore';
import Waveform from '../Waveform';
import { Colors, Fonts, FontSizes, Spacing, BorderWidth } from '../../constants/theme';

if (Platform.OS === 'android') {
  UIManager.setLayoutAnimationEnabledExperimental?.(true);
}

const DISPLAY_HZ = 4;

function getMagColor(magnitude: number, isAnomaly: boolean): string {
  if (isAnomaly) return Colors.gold;
  if (magnitude < 20)  return Colors.cyan;
  if (magnitude < 50)  return '#00BB99';
  if (magnitude < 65)  return Colors.white;
  return Colors.gold;
}

function headingDir(deg: number): string {
  return ['N','NE','E','SE','S','SW','W','NW'][Math.round(deg / 45) % 8];
}

interface SensorSnapshot {
  magnitude: number;
  heading: number;
  x: number;
  y: number;
  z: number;
  rollingAvg: number;
  anomalyDelta: number;
  isAnomaly: boolean;
  isBaselineReady: boolean;
}

function snap(): SensorSnapshot {
  const s = useFieldStore.getState();
  return {
    magnitude:    s.reading.magnitude,
    heading:      s.reading.heading,
    x:            s.reading.x,
    y:            s.reading.y,
    z:            s.reading.z,
    rollingAvg:   s.rollingAverage,
    anomalyDelta: s.anomalyDelta,
    isAnomaly:    s.isAnomaly,
    isBaselineReady: s.isBaselineReady,
  };
}

interface Props { anomalyCount: number }

export default function SensorHUD({ anomalyCount }: Props) {
  const [data, setData] = useState<SensorSnapshot>(snap);
  const [utc, setUtc] = useState(() => new Date().toUTCString().replace('GMT', 'UTC'));
  const [expanded, setExpanded] = useState(false);
  const [toastVisible, setToastVisible] = useState(false);
  const prevAnomalyRef = useRef(false);

  // Reanimated shared values — driven only from these, never from React state
  const dotOpacity     = useSharedValue(1);
  const hudBorderAlpha = useSharedValue(0.15);   // 0=transparent 1=gold
  const toastOpacity   = useSharedValue(0);

  // 4Hz display throttle — reads store directly, no React subscription
  useEffect(() => {
    const id = setInterval(() => setData(snap()), 1000 / DISPLAY_HZ);
    return () => clearInterval(id);
  }, []);

  // 1-second UTC clock
  useEffect(() => {
    const id = setInterval(() => setUtc(new Date().toUTCString().replace('GMT', 'UTC')), 1000);
    return () => clearInterval(id);
  }, []);

  // Anomaly events via subscribe — stays off the render cycle
  useEffect(() => {
    return useFieldStore.subscribe((s) => {
      const isAnomaly = s.isAnomaly;
      if (isAnomaly && !prevAnomalyRef.current) {
        // Trigger animations purely via shared values
        hudBorderAlpha.value = withSequence(
          withTiming(1, { duration: 200 }),
          withTiming(1, { duration: 1800 }),
          withTiming(0, { duration: 400 })
        );
        toastOpacity.value = withSequence(
          withTiming(1, { duration: 200 }),
          withTiming(1, { duration: 2600 }),
          withTiming(0, { duration: 400 })
        );
        setToastVisible(true);
        setTimeout(() => setToastVisible(false), 3300);
      }
      if (isAnomaly) {
        dotOpacity.value = withRepeat(
          withSequence(withTiming(0.15, { duration: 350 }), withTiming(1, { duration: 350 })),
          -1, false
        );
      } else if (prevAnomalyRef.current) {
        dotOpacity.value = withTiming(1, { duration: 200 });
      }
      prevAnomalyRef.current = isAnomaly;
    });
  }, []);

  // Pure Reanimated styles — no React state reads
  const hudBorderStyle = useAnimatedStyle(() => ({
    borderBottomColor: `rgba(${hudBorderAlpha.value > 0.5 ? '255,183,0' : '0,255,229'},${hudBorderAlpha.value > 0.5 ? hudBorderAlpha.value : 0.15})`,
  }));
  const toastStyle = useAnimatedStyle(() => ({ opacity: toastOpacity.value }));
  const dotStyle   = useAnimatedStyle(() => ({ opacity: dotOpacity.value }));

  const { magnitude, heading, x, y, z, rollingAvg, anomalyDelta, isAnomaly, isBaselineReady } = data;
  const magColor = getMagColor(magnitude, isAnomaly);
  const deltaSign = anomalyDelta >= 0 ? '+' : '';
  const statusLabel = isAnomaly ? 'ANOMALY' : isBaselineReady ? 'NOMINAL' : 'CALIBRATING';
  const statusColor = isAnomaly ? Colors.gold : isBaselineReady ? Colors.cyan : Colors.greyLight;
  const dotColor = isAnomaly ? Colors.gold : isBaselineReady ? Colors.cyan : Colors.greyLight;

  const toggleExpand = useCallback(() => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setExpanded((v) => !v);
  }, []);

  return (
    <View style={styles.wrapper} pointerEvents="box-none">
      <Animated.View style={[styles.hud, hudBorderStyle]}>
        {/* ROW 1 */}
        <View style={styles.row1}>
          <Text style={styles.logo}>◈ RESONANCE MAP</Text>
          <Text style={styles.utcClock}>{utc}</Text>
          <View style={styles.statusBadge}>
            <Animated.View style={[styles.pip, { backgroundColor: dotColor }, dotStyle]} />
            <Text style={[styles.statusText, { color: statusColor }]}>{statusLabel}</Text>
          </View>
        </View>

        {/* ROW 2 */}
        <View style={styles.row2}>
          <View style={styles.dataBlock}>
            <Text style={styles.dataLabel}>MAG</Text>
            <Text style={[styles.magValue, { color: magColor }]}>
              {magnitude.toFixed(1)}<Text style={styles.unit}> µT</Text>
            </Text>
          </View>
          <View style={styles.divV} />
          <View style={styles.dataBlock}>
            <Text style={styles.dataLabel}>HDG</Text>
            <Text style={styles.dataValue}>
              {heading.toFixed(0)}°{'\u2009'}<Text style={styles.unit}>{headingDir(heading)}</Text>
            </Text>
          </View>
          <View style={styles.divV} />
          <View style={styles.dataBlock}>
            <Text style={styles.dataLabel}>BASE</Text>
            <Text style={styles.dataValue}>{rollingAvg.toFixed(1)}<Text style={styles.unit}> µT</Text></Text>
          </View>
          <View style={styles.divV} />
          <View style={styles.dataBlock}>
            <Text style={styles.dataLabel}>DELTA</Text>
            <Text style={[styles.dataValue, { color: isAnomaly ? Colors.gold : Colors.green }]}>
              {deltaSign}{anomalyDelta.toFixed(1)}
            </Text>
          </View>
          <View style={styles.divV} />
          <TouchableOpacity style={styles.dataBlock} onPress={toggleExpand} activeOpacity={0.7}>
            <Text style={styles.dataLabel}>X/Y/Z</Text>
            <Text style={[styles.dataValue, { color: Colors.greyLight }]}>{expanded ? '▲' : '▼'}</Text>
          </TouchableOpacity>
        </View>

        {/* Drawer */}
        {expanded && (
          <View style={styles.drawer}>
            <View style={styles.xyzRow}>
              {([['X', x], ['Y', y], ['Z', z]] as [string, number][]).map(([l, v]) => (
                <View key={l} style={styles.xyzItem}>
                  <Text style={styles.xyzLabel}>{l}</Text>
                  <Text style={styles.xyzVal}>{v.toFixed(2)}</Text>
                  <Text style={styles.unit}> µT</Text>
                </View>
              ))}
            </View>
            <View style={styles.waveformWrap}>
              <Waveform />
            </View>
          </View>
        )}
      </Animated.View>

      {/* Anomaly toast */}
      {toastVisible && (
        <Animated.View style={[styles.toast, toastStyle]} pointerEvents="none">
          <Text style={styles.toastText}>◈ ANOMALY LOGGED — #{anomalyCount}</Text>
        </Animated.View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    zIndex: 10,
  },
  hud: {
    backgroundColor: 'rgba(0,0,10,0.82)',
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0,255,229,0.15)',
    paddingTop: Platform.OS === 'android' ? 36 : 50,
    paddingBottom: Spacing.sm,
    paddingHorizontal: Spacing.md,
  },
  row1: { flexDirection: 'row', alignItems: 'center', marginBottom: 6 },
  logo: { fontFamily: Fonts.header, fontSize: 13, color: Colors.cyan, letterSpacing: 2, flex: 1 },
  utcClock: { fontFamily: Fonts.mono, fontSize: 11, color: 'rgba(255,255,255,0.45)', flex: 1.2, textAlign: 'center' },
  statusBadge: { flexDirection: 'row', alignItems: 'center', flex: 1, justifyContent: 'flex-end' },
  pip: { width: 6, height: 6, borderRadius: 3, marginRight: 5 },
  statusText: { fontFamily: Fonts.mono, fontSize: 10, letterSpacing: 1.5 },

  row2: { flexDirection: 'row', alignItems: 'center' },
  dataBlock: { flex: 1, alignItems: 'center' },
  dataLabel: { fontFamily: Fonts.mono, fontSize: 8, color: Colors.greyLight, letterSpacing: 1, marginBottom: 2 },
  magValue: { fontFamily: Fonts.header, fontSize: 22, lineHeight: 26 },
  dataValue: { fontFamily: Fonts.mono, fontSize: FontSizes.sm, color: Colors.cyan },
  unit: { fontSize: 9, color: Colors.greyLight, fontFamily: Fonts.mono },
  divV: { width: 0.5, height: 28, backgroundColor: Colors.grey, opacity: 0.35 },

  drawer: {
    marginTop: Spacing.sm,
    borderTopWidth: 0.5,
    borderTopColor: Colors.grey,
    paddingTop: Spacing.sm,
  },
  xyzRow: { flexDirection: 'row', justifyContent: 'space-around', marginBottom: Spacing.sm },
  xyzItem: { flexDirection: 'row', alignItems: 'baseline', gap: 3 },
  xyzLabel: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.greyLight, letterSpacing: 1 },
  xyzVal: { fontFamily: Fonts.mono, fontSize: FontSizes.sm, color: Colors.green },
  waveformWrap: { transform: [{ scaleY: 0.6 }], marginTop: -10 },

  toast: {
    marginTop: 4,
    marginHorizontal: Spacing.md,
    backgroundColor: 'rgba(0,0,10,0.88)',
    borderWidth: BorderWidth.thin,
    borderColor: Colors.gold,
    paddingVertical: 6,
    paddingHorizontal: Spacing.md,
    alignSelf: 'flex-start',
  },
  toastText: { fontFamily: Fonts.mono, fontSize: FontSizes.xs, color: Colors.gold, letterSpacing: 1.5 },
});
