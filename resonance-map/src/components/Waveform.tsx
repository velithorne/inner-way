import React, { useMemo } from 'react';
import { View, StyleSheet, Dimensions } from 'react-native';
import Svg, { Polyline, Line, Rect, Text as SvgText } from 'react-native-svg';
import { useFieldStore } from '../store/useFieldStore';
import { Colors, Fonts, FontSizes } from '../constants/theme';
import { FIELD_WEAK_MAX, FIELD_NORMAL_MAX, ANOMALY_DEVIATION_PERCENT } from '../constants/thresholds';

const { width: SCREEN_WIDTH } = Dimensions.get('window');
const HEIGHT = 100;
const PADDING = { top: 12, bottom: 20, left: 4, right: 4 };

function buildPolylinePoints(
  history: number[],
  minVal: number,
  maxVal: number,
  width: number,
  height: number
): string {
  if (history.length < 2) return '';
  const range = maxVal - minVal || 1;
  return history
    .map((v, i) => {
      const x = PADDING.left + (i / (history.length - 1)) * (width - PADDING.left - PADDING.right);
      const y = PADDING.top + (1 - (v - minVal) / range) * (height - PADDING.top - PADDING.bottom);
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(' ');
}

export default function Waveform() {
  const history = useFieldStore((s) => s.history);
  const rollingAverage = useFieldStore((s) => s.rollingAverage);
  const isAnomaly = useFieldStore((s) => s.isAnomaly);

  const width = SCREEN_WIDTH;

  const { minVal, maxVal, thresholdY, normalPoints, anomalySegments } = useMemo(() => {
    if (history.length === 0) {
      return {
        minVal: 0,
        maxVal: 100,
        thresholdY: HEIGHT / 2,
        normalPoints: '',
        anomalySegments: [] as { x1: number; x2: number }[],
      };
    }

    const base = rollingAverage || FIELD_NORMAL_MAX;
    const threshold = base * (1 + ANOMALY_DEVIATION_PERCENT);
    const minRaw = Math.min(...history);
    const maxRaw = Math.max(...history, threshold * 1.1);
    const minV = Math.max(0, minRaw - 5);
    const maxV = maxRaw + 5;
    const range = maxV - minV || 1;

    const drawH = HEIGHT - PADDING.top - PADDING.bottom;
    const drawW = width - PADDING.left - PADDING.right;

    const ty = PADDING.top + (1 - (threshold - minV) / range) * drawH;

    // Build normal points (everything)
    const allPoints = buildPolylinePoints(history, minV, maxV, width, HEIGHT);

    // Build anomaly segments (indices where value > threshold)
    const segments: { x1: number; x2: number }[] = [];
    let segStart: number | null = null;

    for (let i = 0; i < history.length; i++) {
      const isAbove = history[i] > threshold;
      if (isAbove && segStart === null) {
        segStart = i;
      } else if (!isAbove && segStart !== null) {
        segments.push({
          x1: PADDING.left + (segStart / (history.length - 1)) * drawW,
          x2: PADDING.left + (i / (history.length - 1)) * drawW,
        });
        segStart = null;
      }
    }
    if (segStart !== null) {
      segments.push({
        x1: PADDING.left + (segStart / (history.length - 1)) * drawW,
        x2: PADDING.left + drawW,
      });
    }

    return {
      minVal: minV,
      maxVal: maxV,
      thresholdY: ty,
      normalPoints: allPoints,
      anomalySegments: segments,
    };
  }, [history, rollingAverage]);

  return (
    <View style={styles.container}>
      <Svg width={width} height={HEIGHT}>
        {/* Background */}
        <Rect x={0} y={0} width={width} height={HEIGHT} fill={Colors.backgroundCard} />

        {/* Anomaly threshold line */}
        <Line
          x1={PADDING.left}
          y1={thresholdY}
          x2={width - PADDING.right}
          y2={thresholdY}
          stroke={Colors.gold}
          strokeWidth={0.8}
          strokeDasharray="4,4"
          opacity={0.7}
        />

        {/* Anomaly highlight bands */}
        {anomalySegments.map((seg, i) => (
          <Rect
            key={i}
            x={seg.x1}
            y={PADDING.top}
            width={seg.x2 - seg.x1}
            height={HEIGHT - PADDING.top - PADDING.bottom}
            fill={Colors.gold}
            opacity={0.08}
          />
        ))}

        {/* Waveform line */}
        {normalPoints.length > 0 && (
          <Polyline
            points={normalPoints}
            fill="none"
            stroke={isAnomaly ? Colors.gold : Colors.cyan}
            strokeWidth={1.2}
            opacity={0.9}
          />
        )}

        {/* Threshold label */}
        <SvgText
          x={width - PADDING.right - 2}
          y={thresholdY - 3}
          fontSize={FontSizes.xs}
          fill={Colors.gold}
          opacity={0.7}
          textAnchor="end"
          fontFamily="monospace"
        >
          THRESHOLD
        </SvgText>
      </Svg>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: Colors.backgroundCard,
    borderTopWidth: 0.5,
    borderTopColor: Colors.grey,
  },
});
