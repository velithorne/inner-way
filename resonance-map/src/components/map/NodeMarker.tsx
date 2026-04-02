import React, { useEffect } from 'react';
import { View, StyleSheet } from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withTiming,
  Easing,
} from 'react-native-reanimated';
import { Marker, Circle } from 'react-native-maps';
import { AnomalyEntry } from '../../services/anomalyLog';
import { Colors } from '../../constants/theme';

function getNodeColor(magnitude: number): string {
  if (magnitude < 20) return Colors.cyan;
  if (magnitude < 50) return '#00AA88';
  return Colors.gold;
}

function getNodeSize(magnitude: number): number {
  const base = 12;
  const extra = Math.min(8, (magnitude / 60) * 8);
  return base + extra;
}

interface PulseMarkerProps {
  color: string;
  size: number;
  verified: boolean;
}

function PulseMarker({ color, size, verified }: PulseMarkerProps) {
  const scale = useSharedValue(1);
  const opacity = useSharedValue(0.7);

  useEffect(() => {
    if (!verified) {
      scale.value = withRepeat(
        withTiming(2.2, { duration: 1600, easing: Easing.out(Easing.ease) }),
        -1,
        false
      );
      opacity.value = withRepeat(
        withTiming(0, { duration: 1600, easing: Easing.out(Easing.ease) }),
        -1,
        false
      );
    }
  }, [verified]);

  const pulseStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
    opacity: opacity.value,
  }));

  return (
    <View style={[dot.container, { width: size * 2 + 8, height: size * 2 + 8 }]}>
      {/* Pulse ring — not shown on verified nodes */}
      {!verified && (
        <Animated.View
          style={[
            dot.ring,
            pulseStyle,
            {
              width: size * 2,
              height: size * 2,
              borderRadius: size,
              borderColor: color,
            },
          ]}
        />
      )}
      {/* Outer ring */}
      <View
        style={[
          dot.outer,
          {
            width: size * 2,
            height: size * 2,
            borderRadius: size,
            borderColor: color,
            borderWidth: verified ? 2 : 1.5,
            backgroundColor: verified ? color + '30' : 'transparent',
          },
        ]}
      >
        {/* Inner dot */}
        <View style={[dot.inner, { backgroundColor: verified ? color : '#FFFFFF' }]} />
        {/* Verified checkmark overlay */}
        {verified && (
          <View style={dot.checkOverlay}>
            <View style={dot.checkmark} />
          </View>
        )}
      </View>
    </View>
  );
}

const dot = StyleSheet.create({
  container: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  ring: {
    position: 'absolute',
    borderWidth: 1,
  },
  outer: {
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1.5,
  },
  inner: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  checkOverlay: {
    position: 'absolute',
    top: -10,
    right: -10,
    width: 14,
    height: 14,
    borderRadius: 7,
    backgroundColor: Colors.gold,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkmark: {
    width: 5,
    height: 8,
    borderBottomWidth: 1.5,
    borderRightWidth: 1.5,
    borderColor: '#000',
    transform: [{ rotate: '45deg' }, { translateY: -1 }],
  },
});

interface Props {
  entry: AnomalyEntry;
  onPress: (entry: AnomalyEntry) => void;
  showProximity: boolean;
}

export default function NodeMarker({ entry, onPress, showProximity }: Props) {
  if (entry.coordinates.lat === null || entry.coordinates.lng === null) return null;

  const color = getNodeColor(entry.magnitude);
  const size = getNodeSize(entry.magnitude);

  return (
    <Marker
      coordinate={{
        latitude: entry.coordinates.lat,
        longitude: entry.coordinates.lng,
      }}
      onPress={() => onPress(entry)}
      tracksViewChanges={false}
      anchor={{ x: 0.5, y: 0.5 }}
    >
      <PulseMarker color={color} size={size} verified={entry.verified === true} />
    </Marker>
  );
}
