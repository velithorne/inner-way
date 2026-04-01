import React, { useEffect, useRef, useState } from 'react';
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
import { FIELD_WEAK_MAX, FIELD_NORMAL_MAX } from '../../constants/thresholds';

if (Platform.OS === 'android') {
  UIManager.setLayoutAnimationEnabledExperimental?.(true);
}

function getMagColor(magnitude: number, isAnomaly: boolean): string {
  if (isAnomaly) return Colors.gold;
  if (magnitude < 20)  return Colors.cyan;
  if (magnitude < 50)  return '#00BB99';
  if (magnitude < 65)  return Colors.white;
  return Colors.gold;
}

function headingDir(deg: number): string {
  const dirs = ['N','NE','E','SE','S','SW','W','NW'];
  return dirs[Math.round(deg / 45) % 8];
}

function StatusDot({ isAnomaly, isBaselineReady }: { isAnomaly: boolean; isBaselineReady: boolean }) {
  const opacity = useSharedValue(1);
  useEffect(() => {
    if (isAnomaly) {
      opacity.value = withRepeat(
        withSequence(withTiming(0.15, { duration: 350 }), withTiming(1, { duration: 350 })),
        -1, false
      );
    } else {
      opacity.value = withTiming(1, { duration: 200 });
    }
  }, [isAnomaly]);
  const style = useAnimatedStyle(() => ({ opacity: opacity.value }));
  const color = isAnomaly ? Colors.gold : isBaselineReady ? Colors.cyan : Colors.greyLight;
  return (
    <Animated.View style={[dot.pip, { backgroundColor: color }, style]} />
  );
}

const dot = StyleSheet.create({
  pip: { width: 6, height: 6, borderRadius: 3, marginRight: 5 },
});

interface Props {
  anomalyCount: number;
}

export default function SensorHUD({ anomalyCount }: Props) {
  const reading       = useFieldStore((s) => s.reading);
  const isAnomaly     = useFieldStore((s) => s.isAnomaly);
  const isBaselineReady = useFieldStore((s) => s.isBaselineReady);
  const rollingAvg    = useFieldStore((s) => s.rollingAverage);
  const anomalyDelta  = useFieldStore((s) => s.anomalyDelta);

  const [expanded, setExpanded] = useState(false);
  const [toastVisible, setToastVisible] = useState(false);
  const toastOpacity = useSharedValue(0);
  const borderColor  = useSharedValue(Colors.grey);
  const prevAnomalyRef = useRef(false);

  const { magnitude, heading, x, y, z } = reading;
  const magColor = getMagColor(magnitude, isAnomaly);

  // Anomaly → flash border gold, show toast
  useEffect(() => {
    if (isAnomaly && !prevAnomalyRef.current) {
      borderColor.value = withSequence(
        withTiming(Colors.gold, { duration: 200 }),
        withTiming(Colors.gold, { duration: 1800 }),
        withTiming(Colors.greyDark, { duration: 400 })
      );
      toastOpacity.value = withSequence(
        withTiming(1, { duration: 200 }),
        withTiming(1, { duration: 2600 }),
        withTiming(0, { duration: 400 })
      );
      setToastVisible(true);
      setTimeout(() => setToastVisible(false), 3200);
    }
    prevAnomalyRef.current = isAnomaly;
  }, [isAnomaly, anomalyCount]);

  const hudBorderStyle = useAnimatedStyle(() => ({
    borderBottomColor: isAnomaly ? Colors.gold : 'rgba(0,255,229,0.15)',
  }));

  const toastStyle = useAnimatedStyle(() => ({ opacity: toastOpacity.value }));

  const statusLabel = isAnomaly
    ? 'ANOMALY'
    : isBaselineReady ? 'NOMINAL' : 'CALIBRATING';
  const statusColor = isAnomaly
    ? Colors.gold
    : isBaselineReady ? Colors.cyan : Colors.greyLight;

  const toggleExpand = () => {
    LayoutAnimation.configureNext(LayoutAnimation.Presets.easeInEaseOut);
    setExpanded((v) => !v);
  };

  const deltaSign = anomalyDelta >= 0 ? '+' : '';

  return (
    <View style={styles.wrapper} pointerEvents="box-none">
      <Animated.View style={[styles.hud, hudBorderStyle]}>
        {/* ROW 1: identity + status */}
        <View style={styles.row1}>
          <Text style={styles.logo}>◈ RESONANCE MAP</Text>
          <Text style={styles.utcClock}>{new Date().toUTCString().replace('GMT','UTC')}</Text>
          <View style={styles.statusBadge}>
            <StatusDot isAnomaly={isAnomaly} isBaselineReady={isBaselineReady} />
            <Text style={[styles.statusText, { color: statusColor }]}>{statusLabel}</Text>
          </View>
        </View>

        {/* ROW 2: live data strip */}
        <View style={styles.row2}>
          {/* Magnitude — hero value */}
          <View style={styles.dataBlock}>
            <Text style={styles.dataLabel}>MAG</Text>
            <Text style={[styles.magValue, { color: magColor }]}>
              {magnitude.toFixed(1)}
              <Text style={styles.unit}> µT</Text>
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

          {/* XYZ tap-to-expand */}
          <TouchableOpacity style={styles.dataBlock} onPress={toggleExpand} activeOpacity={0.7}>
            <Text style={styles.dataLabel}>X/Y/Z</Text>
            <Text style={[styles.dataValue, { color: Colors.greyLight }]}>
              {expanded ? '▲' : '▼'}
            </Text>
          </TouchableOpacity>
        </View>

        {/* Expandable drawer */}
        {expanded && (
          <View style={styles.drawer}>
            <View style={styles.xyzRow}>
              {[['X', x], ['Y', y], ['Z', z]].map(([lbl, val]) => (
                <View key={lbl as string} style={styles.xyzItem}>
                  <Text style={styles.xyzLabel}>{lbl}</Text>
                  <Text style={styles.xyzVal}>{(val as number).toFixed(2)}</Text>
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
  row1: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 6,
  },
  logo: {
    fontFamily: Fonts.header,
    fontSize: 13,
    color: Colors.cyan,
    letterSpacing: 2,
    flex: 1,
  },
  utcClock: {
    fontFamily: Fonts.mono,
    fontSize: 11,
    color: 'rgba(255,255,255,0.45)',
    flex: 1.2,
    textAlign: 'center',
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
    justifyContent: 'flex-end',
  },
  statusText: {
    fontFamily: Fonts.mono,
    fontSize: 10,
    letterSpacing: 1.5,
  },

  row2: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  dataBlock: {
    flex: 1,
    alignItems: 'center',
  },
  dataLabel: {
    fontFamily: Fonts.mono,
    fontSize: 8,
    color: Colors.greyLight,
    letterSpacing: 1,
    marginBottom: 2,
  },
  magValue: {
    fontFamily: Fonts.header,
    fontSize: 22,
    lineHeight: 26,
  },
  dataValue: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.sm,
    color: Colors.cyan,
  },
  unit: {
    fontSize: 9,
    color: Colors.greyLight,
    fontFamily: Fonts.mono,
  },
  divV: {
    width: 0.5,
    height: 28,
    backgroundColor: Colors.grey,
    opacity: 0.35,
  },

  drawer: {
    marginTop: Spacing.sm,
    borderTopWidth: 0.5,
    borderTopColor: Colors.grey,
    paddingTop: Spacing.sm,
  },
  xyzRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: Spacing.sm,
  },
  xyzItem: {
    flexDirection: 'row',
    alignItems: 'baseline',
    gap: 3,
  },
  xyzLabel: {
    fontFamily: Fonts.mono,
    fontSize: 9,
    color: Colors.greyLight,
    letterSpacing: 1,
  },
  xyzVal: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.sm,
    color: Colors.green,
  },
  waveformWrap: {
    transform: [{ scaleY: 0.6 }],
    transformOrigin: 'top',
    marginTop: -10,
  },

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
  toastText: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.gold,
    letterSpacing: 1.5,
  },
});
