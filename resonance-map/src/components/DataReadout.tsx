import React, { useEffect, useRef } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withSequence,
  withTiming,
  Easing,
} from 'react-native-reanimated';
import { useFieldStore } from '../store/useFieldStore';
import { Colors, Fonts, FontSizes, Spacing, BorderWidth } from '../constants/theme';
import { FIELD_WEAK_MAX, FIELD_NORMAL_MAX } from '../constants/thresholds';

function getMagnitudeColor(magnitude: number, isAnomaly: boolean): string {
  if (isAnomaly) return Colors.gold;
  if (magnitude < FIELD_WEAK_MAX) return Colors.blueField;
  if (magnitude < FIELD_NORMAL_MAX) return Colors.cyan;
  return Colors.blueBright;
}

function formatValue(v: number, decimals = 1): string {
  return v.toFixed(decimals).padStart(7);
}

function formatDelta(delta: number): string {
  const sign = delta >= 0 ? '+' : '';
  return `${sign}${delta.toFixed(2)}`;
}

function formatHeading(heading: number): string {
  const deg = heading.toFixed(1).padStart(5);
  const dirs = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'];
  const idx = Math.round(heading / 45) % 8;
  return `${deg}°  ${dirs[idx]}`;
}

export default function DataReadout() {
  const { x, y, z, magnitude, heading } = useFieldStore((s) => s.reading);
  const rollingAverage = useFieldStore((s) => s.rollingAverage);
  const isAnomaly = useFieldStore((s) => s.isAnomaly);
  const anomalyDelta = useFieldStore((s) => s.anomalyDelta);
  const isSimulationMode = useFieldStore((s) => s.isSimulationMode);

  const magnitudeColor = getMagnitudeColor(magnitude, isAnomaly);

  // Blink animation for anomaly status
  const blinkOpacity = useSharedValue(1);

  useEffect(() => {
    if (isAnomaly) {
      blinkOpacity.value = withRepeat(
        withSequence(
          withTiming(0.2, { duration: 400, easing: Easing.inOut(Easing.ease) }),
          withTiming(1, { duration: 400, easing: Easing.inOut(Easing.ease) })
        ),
        -1,
        false
      );
    } else {
      blinkOpacity.value = withTiming(1, { duration: 200 });
    }
  }, [isAnomaly]);

  const blinkStyle = useAnimatedStyle(() => ({
    opacity: blinkOpacity.value,
  }));

  return (
    <View style={styles.container}>
      {isSimulationMode && (
        <View style={styles.simBanner}>
          <Text style={styles.simText}>◈ SIMULATION MODE — NO HARDWARE SENSOR ◈</Text>
        </View>
      )}

      {/* XYZ axis readings */}
      <View style={styles.axisRow}>
        <View style={styles.axisItem}>
          <Text style={styles.axisLabel}>X</Text>
          <Text style={styles.axisValue}>{formatValue(x)} <Text style={styles.unit}>µT</Text></Text>
        </View>
        <View style={styles.axisItem}>
          <Text style={styles.axisLabel}>Y</Text>
          <Text style={styles.axisValue}>{formatValue(y)} <Text style={styles.unit}>µT</Text></Text>
        </View>
        <View style={styles.axisItem}>
          <Text style={styles.axisLabel}>Z</Text>
          <Text style={styles.axisValue}>{formatValue(z)} <Text style={styles.unit}>µT</Text></Text>
        </View>
      </View>

      <View style={styles.divider} />

      {/* Magnitude — prominent */}
      <View style={styles.magnitudeRow}>
        <Text style={styles.magnitudeLabel}>MAGNITUDE</Text>
        <Text style={[styles.magnitudeValue, { color: magnitudeColor }]}>
          {magnitude.toFixed(2)} <Text style={styles.magnitudeUnit}>µT</Text>
        </Text>
      </View>

      <View style={styles.divider} />

      {/* Secondary stats */}
      <View style={styles.statsRow}>
        <View style={styles.statItem}>
          <Text style={styles.statLabel}>HEADING</Text>
          <Text style={styles.statValue}>{formatHeading(heading)}</Text>
        </View>
        <View style={styles.statItem}>
          <Text style={styles.statLabel}>BASELINE</Text>
          <Text style={styles.statValue}>{rollingAverage.toFixed(2)} <Text style={styles.unit}>µT</Text></Text>
        </View>
        <View style={styles.statItem}>
          <Text style={styles.statLabel}>DELTA</Text>
          <Text style={[styles.statValue, { color: Math.abs(anomalyDelta) > 1 ? Colors.gold : Colors.green }]}>
            {formatDelta(anomalyDelta)}
          </Text>
        </View>
      </View>

      <View style={styles.divider} />

      {/* Anomaly status */}
      <Animated.View style={[styles.anomalyRow, blinkStyle]}>
        <Text style={styles.anomalyLabel}>STATUS</Text>
        <Text style={[styles.anomalyValue, { color: isAnomaly ? Colors.gold : Colors.cyan }]}>
          {isAnomaly ? '⬡ ANOMALY DETECTED' : '◈ NOMINAL'}
        </Text>
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: Colors.backgroundPanel,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
    borderTopWidth: BorderWidth.thin,
    borderTopColor: Colors.grey,
  },
  simBanner: {
    backgroundColor: Colors.goldFaint,
    borderWidth: BorderWidth.thin,
    borderColor: Colors.gold,
    borderRadius: 2,
    paddingVertical: Spacing.xs,
    marginBottom: Spacing.sm,
    alignItems: 'center',
  },
  simText: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.gold,
    letterSpacing: 1,
  },
  axisRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: Spacing.xs,
  },
  axisItem: {
    flex: 1,
    alignItems: 'center',
  },
  axisLabel: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.greyLight,
    letterSpacing: 2,
    marginBottom: 2,
  },
  axisValue: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.sm,
    color: Colors.green,
  },
  unit: {
    fontSize: FontSizes.xs,
    color: Colors.greyLight,
  },
  divider: {
    height: BorderWidth.thin,
    backgroundColor: Colors.grey,
    opacity: 0.4,
    marginVertical: Spacing.xs,
  },
  magnitudeRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: Spacing.xs,
  },
  magnitudeLabel: {
    fontFamily: Fonts.header,
    fontSize: FontSizes.xs,
    color: Colors.greyLight,
    letterSpacing: 2,
  },
  magnitudeValue: {
    fontFamily: Fonts.header,
    fontSize: FontSizes.xl,
    letterSpacing: 1,
  },
  magnitudeUnit: {
    fontSize: FontSizes.sm,
    color: Colors.greyLight,
  },
  statsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: Spacing.xs,
  },
  statItem: {
    flex: 1,
    alignItems: 'center',
  },
  statLabel: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.greyLight,
    letterSpacing: 1.5,
    marginBottom: 2,
  },
  statValue: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.sm,
    color: Colors.cyan,
  },
  anomalyRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: Spacing.xs,
  },
  anomalyLabel: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.greyLight,
    letterSpacing: 2,
  },
  anomalyValue: {
    fontFamily: Fonts.header,
    fontSize: FontSizes.md,
    letterSpacing: 1.5,
  },
});
