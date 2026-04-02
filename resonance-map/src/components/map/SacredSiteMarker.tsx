import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Marker, Circle } from 'react-native-maps';
import { SacredSite } from '../../constants/sacredSites';
import { Colors, Fonts } from '../../constants/theme';

interface Props {
  site: SacredSite;
  onPress: (site: SacredSite) => void;
  anomalyCount: number;
}

export default function SacredSiteMarker({ site, onPress, anomalyCount }: Props) {
  const hasProximityAnomaly = anomalyCount > 0;

  return (
    <>
      {/* Translucent radius influence circle */}
      <Circle
        center={{ latitude: site.lat, longitude: site.lng }}
        radius={site.radius * 1000}
        strokeColor={Colors.gold + '30'}
        strokeWidth={0.8}
        fillColor={Colors.gold + '08'}
      />

      {/* Diamond marker */}
      <Marker
        coordinate={{ latitude: site.lat, longitude: site.lng }}
        onPress={() => onPress(site)}
        tracksViewChanges={false}
        anchor={{ x: 0.5, y: 0.5 }}
      >
        <View style={diamond.wrapper}>
          {/* Glow */}
          <View style={[diamond.glow, hasProximityAnomaly && diamond.glowActive]} />
          {/* Diamond shape */}
          <View style={[diamond.shape, hasProximityAnomaly && diamond.shapeActive]}>
            <Text style={diamond.icon}>◆</Text>
          </View>
          {/* Proximity badge */}
          {hasProximityAnomaly && (
            <View style={diamond.badge}>
              <Text style={diamond.badgeText}>{anomalyCount}</Text>
            </View>
          )}
        </View>
      </Marker>
    </>
  );
}

const diamond = StyleSheet.create({
  wrapper: {
    width: 32,
    height: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
  glow: {
    position: 'absolute',
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: Colors.gold + '20',
  },
  glowActive: {
    backgroundColor: Colors.gold + '40',
  },
  shape: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  shapeActive: {
    // brighter when has proximity links
  },
  icon: {
    fontSize: 16,
    color: Colors.gold,
    textShadowColor: Colors.gold,
    textShadowRadius: 4,
    textShadowOffset: { width: 0, height: 0 },
  },
  badge: {
    position: 'absolute',
    top: -2,
    right: -2,
    width: 13,
    height: 13,
    borderRadius: 6.5,
    backgroundColor: Colors.cyan,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeText: {
    fontSize: 7,
    color: '#000',
    fontWeight: 'bold',
  },
});
