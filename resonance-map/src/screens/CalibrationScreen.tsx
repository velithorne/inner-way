import React, { useEffect, useRef, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  StatusBar,
  Platform,
} from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withRepeat,
  withSequence,
  Easing,
  cancelAnimation,
} from 'react-native-reanimated';
import Svg, { Circle, Path, G } from 'react-native-svg';
import { useNavigation } from '@react-navigation/native';
import { startCalibration } from '../services/magnetometer';
import { Colors, Fonts, FontSizes, Spacing, BorderWidth } from '../constants/theme';
import { CALIBRATION_DURATION_MS } from '../constants/thresholds';

const RING_SIZE = 180;
const RING_STROKE = 6;
const RING_RADIUS = (RING_SIZE - RING_STROKE) / 2;
const RING_CIRCUMFERENCE = 2 * Math.PI * RING_RADIUS;

function Figure8Illustration() {
  // Animated phone dot tracing figure-8
  const progress = useSharedValue(0);

  useEffect(() => {
    progress.value = withRepeat(
      withTiming(1, { duration: 2400, easing: Easing.linear }),
      -1,
      false
    );
    return () => cancelAnimation(progress);
  }, []);

  const dotStyle = useAnimatedStyle(() => {
    const t = progress.value * 2 * Math.PI;
    // Lissajous figure-8: x=sin(t), y=sin(2t)/2
    const x = 55 + Math.sin(t) * 50;
    const y = 70 + Math.sin(2 * t) * 30;
    return {
      position: 'absolute',
      left: x - 6,
      top: y - 6,
      width: 12,
      height: 12,
      borderRadius: 6,
      backgroundColor: Colors.cyan,
      shadowColor: Colors.cyan,
      shadowRadius: 6,
      shadowOpacity: 0.9,
      elevation: 4,
    };
  });

  return (
    <View style={figure8Styles.container}>
      <Svg width={110} height={140}>
        {/* Figure-8 path guide */}
        <Path
          d="M55,70 C55,40 105,40 105,70 C105,100 55,100 55,70 C55,40 5,40 5,70 C5,100 55,100 55,70"
          stroke={Colors.grey}
          strokeWidth={1}
          fill="none"
          strokeDasharray="3,4"
          opacity={0.5}
        />
        {/* Phone icon */}
        <G transform="translate(45, 60)">
          <Path
            d="M2,0 L18,0 A2,2 0 0 1 20,2 L20,22 A2,2 0 0 1 18,24 L2,24 A2,2 0 0 1 0,22 L0,2 A2,2 0 0 1 2,0 Z"
            stroke={Colors.greyLight}
            strokeWidth={1}
            fill={Colors.backgroundCard}
            opacity={0.6}
          />
        </G>
      </Svg>
      <Animated.View style={dotStyle} />
    </View>
  );
}

const figure8Styles = StyleSheet.create({
  container: {
    width: 110,
    height: 140,
    position: 'relative',
  },
});

interface ProgressRingProps {
  progress: number; // 0..1
}

function ProgressRing({ progress }: ProgressRingProps) {
  const dashoffset = RING_CIRCUMFERENCE * (1 - progress);
  return (
    <Svg width={RING_SIZE} height={RING_SIZE}>
      {/* Track */}
      <Circle
        cx={RING_SIZE / 2}
        cy={RING_SIZE / 2}
        r={RING_RADIUS}
        stroke={Colors.backgroundCard}
        strokeWidth={RING_STROKE}
        fill="none"
      />
      {/* Progress arc */}
      <Circle
        cx={RING_SIZE / 2}
        cy={RING_SIZE / 2}
        r={RING_RADIUS}
        stroke={Colors.cyan}
        strokeWidth={RING_STROKE}
        fill="none"
        strokeDasharray={`${RING_CIRCUMFERENCE}`}
        strokeDashoffset={dashoffset}
        strokeLinecap="round"
        transform={`rotate(-90 ${RING_SIZE / 2} ${RING_SIZE / 2})`}
        opacity={0.9}
      />
    </Svg>
  );
}

type Phase = 'intro' | 'calibrating' | 'complete';

export default function CalibrationScreen() {
  const navigation = useNavigation();
  const [phase, setPhase] = useState<Phase>('intro');
  const [countdown, setCountdown] = useState(CALIBRATION_DURATION_MS / 1000);
  const [ringProgress, setRingProgress] = useState(0);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const beginCalibration = async () => {
    setPhase('calibrating');
    const durationSec = CALIBRATION_DURATION_MS / 1000;
    let elapsed = 0;

    intervalRef.current = setInterval(() => {
      elapsed += 0.1;
      setCountdown(Math.max(0, durationSec - elapsed));
      setRingProgress(Math.min(1, elapsed / durationSec));
    }, 100);

    await startCalibration();

    if (intervalRef.current) clearInterval(intervalRef.current);
    setRingProgress(1);
    setCountdown(0);
    setPhase('complete');
  };

  useEffect(() => {
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, []);

  return (
    <View style={styles.root}>
      <StatusBar barStyle="light-content" backgroundColor={Colors.background} />

      <View style={styles.header}>
        <TouchableOpacity
          onPress={() =>
            navigation.canGoBack()
              ? navigation.goBack()
              : navigation.reset({ index: 0, routes: [{ name: 'Field' as never }] })
          }
          style={styles.backBtn}
        >
          <Text style={styles.backText}>← BACK</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>CALIBRATION</Text>
        <View style={{ width: 60 }} />
      </View>

      <View style={styles.content}>
        {phase === 'intro' && (
          <>
            <Text style={styles.instructionTitle}>HARD IRON CALIBRATION</Text>
            <Text style={styles.instructionText}>
              Move your device in a smooth figure-8 motion for 8 seconds.{'\n\n'}
              This removes magnetic interference from the device hardware and establishes a clean baseline.
            </Text>
            <Figure8Illustration />
            <Text style={styles.subInstruction}>
              Keep your device away from metal surfaces during calibration.
            </Text>
            <TouchableOpacity style={styles.startButton} onPress={beginCalibration}>
              <Text style={styles.startButtonText}>BEGIN CALIBRATION</Text>
            </TouchableOpacity>
          </>
        )}

        {phase === 'calibrating' && (
          <>
            <Text style={styles.instructionTitle}>CAPTURING DATA</Text>
            <Text style={styles.instructionText}>
              Move your device in a figure-8 pattern...
            </Text>
            <View style={styles.ringContainer}>
              <ProgressRing progress={ringProgress} />
              <View style={styles.ringCenter}>
                <Text style={styles.countdownText}>{Math.ceil(countdown)}</Text>
                <Text style={styles.countdownLabel}>SEC</Text>
              </View>
            </View>
            <Figure8Illustration />
          </>
        )}

        {phase === 'complete' && (
          <>
            <Text style={[styles.instructionTitle, { color: Colors.cyan }]}>
              CALIBRATION COMPLETE
            </Text>
            <Text style={[styles.subInstruction, { color: Colors.cyan, marginBottom: Spacing.xl }]}>
              — BASELINE LOCKED —
            </Text>
            <View style={styles.completeRing}>
              <ProgressRing progress={1} />
              <View style={styles.ringCenter}>
                <Text style={styles.checkmark}>✓</Text>
              </View>
            </View>
            <Text style={styles.instructionText}>
              Offset values saved. The sensor will now report corrected readings.
            </Text>
            <TouchableOpacity
              style={styles.startButton}
              onPress={() =>
                navigation.reset({ index: 0, routes: [{ name: 'Field' as never }] })
              }
            >
              <Text style={styles.startButtonText}>RETURN TO FIELD</Text>
            </TouchableOpacity>
          </>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.md,
    paddingTop: Platform.OS === 'android' ? Spacing.xl : Spacing.lg,
    paddingBottom: Spacing.sm,
    borderBottomWidth: BorderWidth.thin,
    borderBottomColor: Colors.grey,
  },
  backBtn: {
    padding: Spacing.xs,
  },
  backText: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.sm,
    color: Colors.cyanDim,
    letterSpacing: 1,
  },
  headerTitle: {
    fontFamily: Fonts.header,
    fontSize: FontSizes.md,
    color: Colors.cyan,
    letterSpacing: 4,
  },
  content: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: Spacing.xl,
    gap: Spacing.lg,
  },
  instructionTitle: {
    fontFamily: Fonts.header,
    fontSize: FontSizes.lg,
    color: Colors.gold,
    letterSpacing: 3,
    textAlign: 'center',
  },
  instructionText: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.sm,
    color: Colors.greyLight,
    textAlign: 'center',
    lineHeight: 22,
  },
  subInstruction: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.grey,
    textAlign: 'center',
    letterSpacing: 1,
  },
  ringContainer: {
    position: 'relative',
    width: RING_SIZE,
    height: RING_SIZE,
    alignItems: 'center',
    justifyContent: 'center',
  },
  ringCenter: {
    position: 'absolute',
    alignItems: 'center',
  },
  countdownText: {
    fontFamily: Fonts.header,
    fontSize: FontSizes.xxxl,
    color: Colors.cyan,
  },
  countdownLabel: {
    fontFamily: Fonts.mono,
    fontSize: FontSizes.xs,
    color: Colors.greyLight,
    letterSpacing: 2,
  },
  completeRing: {
    position: 'relative',
    width: RING_SIZE,
    height: RING_SIZE,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkmark: {
    fontSize: 48,
    color: Colors.cyan,
  },
  startButton: {
    borderWidth: BorderWidth.normal,
    borderColor: Colors.cyan,
    paddingHorizontal: Spacing.xl,
    paddingVertical: Spacing.md,
    marginTop: Spacing.lg,
  },
  startButtonText: {
    fontFamily: Fonts.header,
    fontSize: FontSizes.md,
    color: Colors.cyan,
    letterSpacing: 2,
  },
});
