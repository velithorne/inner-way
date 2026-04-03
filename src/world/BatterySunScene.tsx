import { StyleSheet, Text, View } from 'react-native';
import Svg, { Circle, Defs, RadialGradient, Stop } from 'react-native-svg';

import { useWorldStore } from '../store/useWorldStore';

/**
 * Phase 2 preview: battery charge as a glowing circle ("sun") — SVG, no extra native deps.
 * Driven by live `batteryLevel` from the native poller (0–100).
 */
export function BatterySunScene() {
  const batteryLevel = useWorldStore((s) => s.batteryLevel);
  const telemetryOk = useWorldStore((s) => s.telemetryState === 'ok');
  const pct = Math.max(0, Math.min(100, batteryLevel)) / 100;
  const r = 72 + pct * 28;
  const glow = 0.45 + pct * 0.55;

  return (
    <View style={styles.wrap}>
      <Text style={styles.label}>Phase 2 preview — Battery sun</Text>
      {!telemetryOk ? (
        <Text style={styles.hint}>Waiting for telemetry… size/colour track charge when data arrives.</Text>
      ) : null}
      <View style={styles.svgBox}>
        <Svg width="100%" height="100%" viewBox="0 0 200 200">
          <Defs>
            <RadialGradient id="sunGrad" cx="50%" cy="45%" rx="50%" ry="50%">
              <Stop offset="0%" stopColor="#fff8e1" stopOpacity={0.95 * glow} />
              <Stop offset="35%" stopColor="#ffb300" stopOpacity={0.9} />
              <Stop offset="100%" stopColor="#ff6f00" stopOpacity={0.35} />
            </RadialGradient>
          </Defs>
          <Circle cx="100" cy="100" r={r} fill="url(#sunGrad)" opacity={0.85 + pct * 0.15} />
        </Svg>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    marginTop: 24,
    alignSelf: 'stretch',
  },
  label: {
    color: '#00bcd4',
    fontSize: 13,
    fontWeight: '600',
    marginBottom: 8,
  },
  hint: {
    color: '#78909c',
    fontSize: 11,
    marginBottom: 8,
  },
  svgBox: {
    height: 220,
    borderRadius: 12,
    overflow: 'hidden',
    backgroundColor: '#05070a',
  },
});
