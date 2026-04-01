import React, { useEffect, useRef, useCallback, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Linking,
  Dimensions,
  Platform,
} from 'react-native';
import { useFocusEffect, useRoute } from '@react-navigation/native';
import { Camera, useCameraDevice, useCameraPermission } from 'react-native-vision-camera';
import * as Haptics from 'expo-haptics';
import * as Location from 'expo-location';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withSequence,
  withTiming,
  Easing,
} from 'react-native-reanimated';

import ARFieldCanvas from '../components/ARFieldCanvas';
import Waveform from '../components/Waveform';
import { useFieldStore } from '../store/useFieldStore';
// Magnetometer lifecycle managed globally in App.tsx
import { logAnomaly } from '../services/anomalyLog';
import { Colors, Fonts, FontSizes, Spacing, BorderWidth } from '../constants/theme';
import { FIELD_WEAK_MAX, FIELD_NORMAL_MAX } from '../constants/thresholds';
import { getNearbysSites, Nearbysite, haversineKm } from '../constants/sacredSites';
import { bearing } from '../utils/geo';

const { width: SCREEN_WIDTH } = Dimensions.get('window');

function getMagnitudeColor(magnitude: number, isAnomaly: boolean): string {
  if (isAnomaly) return Colors.gold;
  if (magnitude < FIELD_WEAK_MAX) return Colors.blueField;
  if (magnitude < FIELD_NORMAL_MAX) return Colors.cyan;
  return Colors.blueBright;
}

// ── Anomaly pulse dot ─────────────────────────────────────────────────────────
function AnomalyBadge({ visible }: { visible: boolean }) {
  const dotOpacity = useSharedValue(1);
  useEffect(() => {
    if (visible) {
      dotOpacity.value = withRepeat(
        withSequence(
          withTiming(0.2, { duration: 300 }),
          withTiming(1.0, { duration: 300 }),
        ),
        -1,
        false,
      );
    } else {
      dotOpacity.value = withTiming(1);
    }
  }, [visible]);
  const dotStyle = useAnimatedStyle(() => ({ opacity: dotOpacity.value }));

  if (!visible) return null;
  return (
    <Animated.View style={[badge.row, dotStyle]}>
      <View style={badge.dot} />
      <Text style={badge.text}>ANOMALY</Text>
    </Animated.View>
  );
}

const badge = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: 5, marginTop: 4 },
  dot: { width: 7, height: 7, borderRadius: 3.5, backgroundColor: Colors.gold },
  text: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.gold, letterSpacing: 1.5 },
});

// ── Site indicator arrow at screen edge ──────────────────────────────────────
function SiteIndicator({ entry, heading }: { entry: Nearbysite; heading: number }) {
  const relBearing = ((entry.bearingDeg - heading) + 360) % 360;
  const angle = (relBearing * Math.PI) / 180;
  const R = SCREEN_WIDTH / 2 - 48;
  const cx = SCREEN_WIDTH / 2;
  const cy = SCREEN_WIDTH * 0.7;
  const x = cx + R * Math.sin(angle);
  const y = cy - R * Math.cos(angle);

  // Clamp to screen edges
  const clampX = Math.max(24, Math.min(SCREEN_WIDTH - 24, x));
  const clampY = Math.max(80, Math.min(cy * 1.5, y));

  const strength = Math.round(entry.site.resonancePlaceholder * 100);

  return (
    <View style={[siteStyles.container, { left: clampX - 50, top: clampY - 20 }]}>
      <Text style={siteStyles.arrow}>
        {relBearing < 45 || relBearing > 315 ? '▲' :
         relBearing < 135 ? '▶' :
         relBearing < 225 ? '▼' : '◀'}
      </Text>
      <Text style={siteStyles.name}>{entry.site.name}</Text>
      <Text style={siteStyles.dist}>{Math.round(entry.distanceKm)} km</Text>
      {/* Phase 3: resonancePlaceholder replaced by live server value */}
      <View style={siteStyles.strengthBar}>
        <View style={[siteStyles.strengthFill, { width: `${strength}%` as any }]} />
      </View>
    </View>
  );
}

const siteStyles = StyleSheet.create({
  container: {
    position: 'absolute',
    alignItems: 'center',
    width: 100,
    backgroundColor: 'rgba(0,0,10,0.55)',
    borderWidth: 0.5,
    borderColor: Colors.gold,
    padding: 4,
  },
  arrow: { fontSize: 12, color: Colors.gold },
  name: { fontFamily: Fonts.mono, fontSize: 8, color: Colors.gold, letterSpacing: 0.5, textAlign: 'center' },
  dist: { fontFamily: Fonts.mono, fontSize: 8, color: Colors.greyLight, marginTop: 1 },
  strengthBar: { width: '100%', height: 2, backgroundColor: Colors.greyDark, marginTop: 3 },
  strengthFill: { height: 2, backgroundColor: Colors.gold, opacity: 0.7 },
});

// ── Permission denied view ────────────────────────────────────────────────────
function CameraPermissionDenied() {
  return (
    <View style={permStyles.container}>
      <Text style={permStyles.icon}>◈</Text>
      <Text style={permStyles.title}>CAMERA ACCESS REQUIRED</Text>
      <Text style={permStyles.body}>
        AR MODE REQUIRES CAMERA ACCESS{'\n'}TAP TO ENABLE IN SETTINGS
      </Text>
      <TouchableOpacity style={permStyles.btn} onPress={() => Linking.openSettings()}>
        <Text style={permStyles.btnText}>OPEN SETTINGS</Text>
      </TouchableOpacity>
    </View>
  );
}

const permStyles = StyleSheet.create({
  container: {
    flex: 1, backgroundColor: Colors.background,
    alignItems: 'center', justifyContent: 'center', gap: Spacing.md,
    paddingHorizontal: Spacing.xl,
  },
  icon: { fontSize: 40, color: Colors.grey },
  title: { fontFamily: Fonts.header, fontSize: FontSizes.md, color: Colors.cyan, letterSpacing: 3, textAlign: 'center' },
  body: { fontFamily: Fonts.mono, fontSize: FontSizes.sm, color: Colors.greyLight, textAlign: 'center', lineHeight: 22 },
  btn: { borderWidth: BorderWidth.normal, borderColor: Colors.cyan, paddingHorizontal: Spacing.xl, paddingVertical: Spacing.sm, marginTop: Spacing.md },
  btnText: { fontFamily: Fonts.mono, fontSize: FontSizes.sm, color: Colors.cyan, letterSpacing: 2 },
});

// ── Main AR screen ────────────────────────────────────────────────────────────
// ── AR target directional overlay ────────────────────────────────────────
function TargetOverlay({ targetLat, targetLng, userLat, userLng, heading }: {
  targetLat: number; targetLng: number;
  userLat: number; userLng: number;
  heading: number;
}) {
  const targetBearing = bearing(
    { latitude: userLat, longitude: userLng },
    { latitude: targetLat, longitude: targetLng }
  );
  const relAngle = ((targetBearing - heading) + 360) % 360;
  const distKm = haversineKm(userLat, userLng, targetLat, targetLng);
  const arrowLabel = relAngle < 22.5 || relAngle > 337.5 ? '▲' :
                     relAngle < 67.5  ? '↗' :
                     relAngle < 112.5 ? '▶' :
                     relAngle < 157.5 ? '↘' :
                     relAngle < 202.5 ? '▼' :
                     relAngle < 247.5 ? '↙' :
                     relAngle < 292.5 ? '◀' : '↖';
  return (
    <View style={tgt.container} pointerEvents="none">
      <Text style={tgt.arrow}>{arrowLabel}</Text>
      <Text style={tgt.label}>TARGET NODE</Text>
      <Text style={tgt.dist}>{distKm < 1 ? `${Math.round(distKm * 1000)}m` : `${distKm.toFixed(1)}km`}</Text>
      <Text style={tgt.bearing}>{Math.round(targetBearing)}° / {Math.round(relAngle)}° rel</Text>
    </View>
  );
}

const tgt = StyleSheet.create({
  container: {
    position: 'absolute',
    top: '35%',
    alignSelf: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0,0,10,0.7)',
    borderWidth: 1,
    borderColor: Colors.gold,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.sm,
  },
  arrow: { fontSize: 32, color: Colors.gold },
  label: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.gold, letterSpacing: 2, marginTop: 2 },
  dist: { fontFamily: Fonts.header, fontSize: FontSizes.xl, color: Colors.gold, marginTop: 2 },
  bearing: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.greyLight, marginTop: 2 },
});

export default function ARFieldScreen() {
  const { hasPermission, requestPermission } = useCameraPermission();
  const device = useCameraDevice('back');
  const route = useRoute<any>();

  // Throttled to 4Hz for the HUD display layer — ARFieldCanvas reads store directly
  const [hudData, setHudData] = useState({
    magnitude: 0, heading: 0, isAnomaly: false, isBaselineReady: false, isSimulationMode: false,
  });
  useEffect(() => {
    const id = setInterval(() => {
      const s = useFieldStore.getState();
      setHudData({
        magnitude: s.reading.magnitude,
        heading: s.reading.heading,
        isAnomaly: s.isAnomaly,
        isBaselineReady: s.isBaselineReady,
        isSimulationMode: s.isSimulationMode,
      });
    }, 250);
    return () => clearInterval(id);
  }, []);
  const reading = useFieldStore.getState().reading; // for static reads only
  const { isAnomaly, isBaselineReady, isSimulationMode } = hudData;

  const [nearbySites, setNearbySites] = useState<Nearbysite[]>([]);
  const [userLat, setUserLat] = useState<number | null>(null);
  const [userLng, setUserLng] = useState<number | null>(null);
  const prevAnomalyRef = useRef(false);
  const anomalyTripleRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Request camera permission
  useEffect(() => {
    if (!hasPermission) requestPermission();
  }, [hasPermission]);

  useFocusEffect(
    useCallback(() => {
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium).catch(() => {});

      (async () => {
        try {
          const { status } = await Location.requestForegroundPermissionsAsync();
          if (status === 'granted') {
            const loc = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
            setUserLat(loc.coords.latitude);
            setUserLng(loc.coords.longitude);
            setNearbySites(getNearbysSites(loc.coords.latitude, loc.coords.longitude));
          }
        } catch {}
      })();

      return () => {
        if (anomalyTripleRef.current) clearTimeout(anomalyTripleRef.current);
      };
    }, [])
  );

  // Anomaly: real-time subscribe (not throttled) for haptics + logging
  useEffect(() => {
    return useFieldStore.subscribe((s) => {
      const nowAnomaly = s.isAnomaly;

      if (nowAnomaly && !prevAnomalyRef.current) {
        // Log to AsyncStorage
        logAnomaly({
          magnitude: s.reading.magnitude,
          delta: s.anomalyDelta,
          x: s.reading.x,
          y: s.reading.y,
          z: s.reading.z,
          heading: s.reading.heading,
        }).catch(() => {});

        // Haptics: heavy + triple-pulse
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Heavy).catch(() => {});
        const pulse = (n: number) => {
          if (n <= 0) return;
          anomalyTripleRef.current = setTimeout(() => {
            Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light).catch(() => {});
            pulse(n - 1);
          }, 200);
        };
        pulse(3);
      }

      if (!nowAnomaly && prevAnomalyRef.current) {
        Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Soft).catch(() => {});
      }

      prevAnomalyRef.current = nowAnomaly;
    });
  }, []);

  const magnitudeColor = getMagnitudeColor(hudData.magnitude, isAnomaly);

  if (!hasPermission) {
    return <CameraPermissionDenied />;
  }

  if (!device) {
    return (
      <View style={styles.noCamera}>
        <Text style={styles.noCameraText}>NO REAR CAMERA DETECTED</Text>
      </View>
    );
  }

  return (
    <View style={styles.root}>
      {/* Layer 0: Live camera feed */}
      <Camera
        style={StyleSheet.absoluteFill}
        device={device}
        isActive={true}
        photo={false}
        video={false}
        audio={false}
      />

      {/* Layer 1: AR field canvas — transparent Three.js overlay */}
      <ARFieldCanvas />

      {/* Target node directional overlay (from MAP tab "OPEN IN AR") */}
      {route.params?.targetLat != null && userLat !== null && userLng !== null && (
        <TargetOverlay
          targetLat={route.params.targetLat as number}
          targetLng={route.params.targetLng as number}
          userLat={userLat}
          userLng={userLng}
          heading={hudData.heading}
        />
      )}

      {/* Layer 2: HUD */}

      {/* Top-left: app label */}
      <View style={styles.topLeft} pointerEvents="none">
        <Text style={styles.appLabel}>◈ RESONANCE MAP  AR MODE</Text>
        {isSimulationMode && (
          <Text style={styles.simLabel}>SIMULATION</Text>
        )}
        {!isBaselineReady && (
          <Text style={styles.calibratingLabel}>CALIBRATING...</Text>
        )}
      </View>

      {/* Top-right: magnitude + anomaly badge */}
      <View style={styles.topRight} pointerEvents="none">
        <Text style={[styles.magnitudeValue, { color: magnitudeColor }]}>
          {hudData.magnitude.toFixed(1)}
        </Text>
        <Text style={styles.magnitudeUnit}>µT</Text>
        {isBaselineReady
          ? <AnomalyBadge visible={isAnomaly} />
          : <Text style={styles.warmupLabel}>WARMUP</Text>
        }
      </View>

      {/* Nearby sacred site indicators */}
      {nearbySites.map((entry) => (
        <SiteIndicator key={entry.site.id} entry={entry} heading={hudData.heading} />
      ))}

      {/* Bottom bar: gradient + waveform + heading */}
      <View style={styles.bottomBar} pointerEvents="none">
        <View style={styles.waveformWrapper}>
          <Waveform />
        </View>
        <View style={styles.bottomStats}>
          <Text style={styles.headingLabel}>HDG</Text>
          <Text style={[styles.headingValue, { color: magnitudeColor }]}>
            {hudData.heading.toFixed(0)}°
          </Text>
          <Text style={styles.baselineLabel}>
            BASE  {useFieldStore.getState().rollingAverage.toFixed(1)} µT
          </Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  noCamera: {
    flex: 1,
    backgroundColor: Colors.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
  noCameraText: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.sm,
    color: Colors.grey,
    letterSpacing: 2,
  },

  // HUD top-left
  topLeft: {
    position: 'absolute',
    top: Platform.OS === 'android' ? 48 : 56,
    left: Spacing.md,
  },
  appLabel: {
    fontFamily: Fonts.mono,
    fontSize: 10,
    color: Colors.cyan,
    opacity: 0.6,
    letterSpacing: 1,
  },
  simLabel: {
    fontFamily: Fonts.mono,
    fontSize: 9,
    color: Colors.gold,
    opacity: 0.8,
    letterSpacing: 1.5,
    marginTop: 2,
  },
  calibratingLabel: {
    fontFamily: Fonts.mono,
    fontSize: 9,
    color: Colors.greyLight,
    opacity: 0.7,
    letterSpacing: 1.5,
    marginTop: 2,
  },
  warmupLabel: {
    fontFamily: Fonts.mono,
    fontSize: 9,
    color: Colors.greyLight,
    opacity: 0.6,
    letterSpacing: 1.5,
    marginTop: 3,
  },

  // HUD top-right
  topRight: {
    position: 'absolute',
    top: Platform.OS === 'android' ? 44 : 52,
    right: Spacing.md,
    alignItems: 'flex-end',
  },
  magnitudeValue: {
    fontFamily: Fonts.header,
    fontSize: 28,
    lineHeight: 32,
  },
  magnitudeUnit: {
    fontFamily: Fonts.mono,
    fontSize: 11,
    color: Colors.greyLight,
    opacity: 0.7,
  },

  // Bottom bar
  bottomBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    height: '28%',
    backgroundColor: 'transparent',
    // Gradient via a dark overlay at bottom
    borderTopWidth: 0,
    paddingBottom: Spacing.lg,
    justifyContent: 'flex-end',
  },
  waveformWrapper: {
    opacity: 0.75,
    marginBottom: 4,
    // Scale down the waveform height
    transform: [{ scaleY: 0.5 }],
    transformOrigin: 'bottom',
  },
  bottomStats: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.lg,
    gap: Spacing.md,
    backgroundColor: 'rgba(0,0,10,0.6)',
    paddingVertical: Spacing.xs,
  },
  headingLabel: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.greyLight,
    letterSpacing: 1.5,
  },
  headingValue: {
    fontFamily: Fonts.header,
    fontSize: FontSizes.lg,
  },
  baselineLabel: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.greyLight,
    opacity: 0.7,
    letterSpacing: 1,
    marginLeft: 'auto',
  },
});
