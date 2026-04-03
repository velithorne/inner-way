import { useEffect, useRef, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import Svg, { Defs, LinearGradient, Path, Stop } from 'react-native-svg';
import type { BpsLabel } from '../store/useBioStore';

const SIZE = 220;
const STROKE = 14;
const R = (SIZE - STROKE) / 2;
const CX = SIZE / 2;
const CY = SIZE / 2;
const START = (135 * Math.PI) / 180;
const SWEEP = (270 * Math.PI) / 180;

function arcPath(fromT: number, toT: number): string {
  const x1 = CX + R * Math.cos(fromT);
  const y1 = CY + R * Math.sin(fromT);
  const x2 = CX + R * Math.cos(toT);
  const y2 = CY + R * Math.sin(toT);
  const large = toT - fromT > Math.PI ? 1 : 0;
  return `M ${x1} ${y1} A ${R} ${R} 0 ${large} 1 ${x2} ${y2}`;
}

function lerp(a: number, b: number, t: number): number {
  return a + (b - a) * t;
}

type Props = {
  bps: number;
  label: BpsLabel;
};

export function BPSGauge({ bps, label }: Props) {
  const display = useRef(0);
  const target = useRef(bps);
  const [, setTick] = useState(0);

  useEffect(() => {
    target.current = bps;
  }, [bps]);

  useEffect(() => {
    let id: number;
    const step = () => {
      display.current = lerp(display.current, target.current, 0.08);
      setTick((n) => n + 1);
      id = requestAnimationFrame(step);
    };
    id = requestAnimationFrame(step);
    return () => cancelAnimationFrame(id);
  }, []);

  const t = Math.max(0, Math.min(100, display.current)) / 100;
  const end = START + t * SWEEP;
  const d = arcPath(START, end);

  return (
    <View style={styles.wrap}>
      <Svg width={SIZE} height={SIZE}>
        <Defs>
          <LinearGradient id="bpsGrad" x1="0%" y1="100%" x2="100%" y2="0%">
            <Stop offset="0%" stopColor="#1a237e" />
            <Stop offset="35%" stopColor="#00bcd4" />
            <Stop offset="70%" stopColor="#ffc107" />
            <Stop offset="100%" stopColor="#ffffff" />
          </LinearGradient>
        </Defs>
        <Path
          d={arcPath(START, START + SWEEP)}
          stroke="#222"
          strokeWidth={STROKE}
          fill="none"
          strokeLinecap="round"
        />
        <Path
          d={d}
          stroke="url(#bpsGrad)"
          strokeWidth={STROKE}
          fill="none"
          strokeLinecap="round"
        />
      </Svg>
      <View style={styles.center} pointerEvents="none">
        <Text style={styles.value}>{Math.round(display.current)}</Text>
        <Text style={styles.unit}>BPS</Text>
      </View>
      <Text style={styles.label}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    alignItems: 'center',
    justifyContent: 'center',
    height: SIZE + 28,
  },
  center: {
    position: 'absolute',
    alignItems: 'center',
    justifyContent: 'center',
  },
  value: {
    color: '#fff',
    fontSize: 42,
    fontWeight: '700',
    fontVariant: ['tabular-nums'],
  },
  unit: {
    color: '#90a4ae',
    fontSize: 12,
    letterSpacing: 2,
  },
  label: {
    marginTop: 4,
    color: '#b0bec5',
    fontSize: 14,
    letterSpacing: 1,
  },
});
