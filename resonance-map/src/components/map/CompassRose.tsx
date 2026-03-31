import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import Svg, { Line, Circle, Text as SvgText, G } from 'react-native-svg';
import { Colors, Fonts, FontSizes } from '../../constants/theme';

interface Props {
  mapHeading: number;       // degrees — true north rotation
  magneticHeading: number;  // from magnetometer
}

const SIZE = 80;
const CX = SIZE / 2;
const CY = SIZE / 2;
const R_OUTER = 34;
const R_INNER = 10;

export default function CompassRose({ mapHeading, magneticHeading }: Props) {
  const declination = ((magneticHeading - mapHeading) + 360) % 360;
  const decSign = declination > 180
    ? `-${(360 - declination).toFixed(0)}`
    : `+${declination.toFixed(0)}`;

  const toRad = (d: number) => (d * Math.PI) / 180;

  // Cardinal lines rotate with map heading
  const cardinals = [
    { label: 'N', angle: -mapHeading, color: Colors.gold },
    { label: 'S', angle: 180 - mapHeading, color: Colors.cyan },
    { label: 'E', angle: 90 - mapHeading, color: Colors.cyan },
    { label: 'W', angle: 270 - mapHeading, color: Colors.cyan },
  ];

  // Magnetic north needle
  const magAngle = toRad(magneticHeading - mapHeading);
  const magX = CX + (R_OUTER - 4) * Math.sin(magAngle);
  const magY = CY - (R_OUTER - 4) * Math.cos(magAngle);

  return (
    <View style={styles.wrapper}>
      <Svg width={SIZE} height={SIZE}>
        {/* Outer ring */}
        <Circle cx={CX} cy={CY} r={R_OUTER} stroke={Colors.grey} strokeWidth={0.5} fill="rgba(0,0,10,0.7)" />

        {/* Cardinal tick lines */}
        {cardinals.map(({ label, angle, color }) => {
          const a = toRad(angle);
          const x1 = CX + (R_INNER + 2) * Math.sin(a);
          const y1 = CY - (R_INNER + 2) * Math.cos(a);
          const x2 = CX + (R_OUTER - 2) * Math.sin(a);
          const y2 = CY - (R_OUTER - 2) * Math.cos(a);
          const lx = CX + (R_OUTER - 9) * Math.sin(a);
          const ly = CY - (R_OUTER - 9) * Math.cos(a);
          return (
            <G key={label}>
              <Line x1={x1} y1={y1} x2={x2} y2={y2} stroke={color} strokeWidth={label === 'N' ? 1.5 : 0.8} />
              <SvgText
                x={lx}
                y={ly + 3}
                fontSize={7}
                fill={color}
                textAnchor="middle"
                fontFamily="monospace"
                fontWeight={label === 'N' ? 'bold' : 'normal'}
              >
                {label}
              </SvgText>
            </G>
          );
        })}

        {/* Centre dot */}
        <Circle cx={CX} cy={CY} r={R_INNER} stroke={Colors.grey} strokeWidth={0.5} fill="rgba(0,0,10,0.8)" />
        <Circle cx={CX} cy={CY} r={2} fill={Colors.cyan} />

        {/* Magnetic north indicator — gold dashed */}
        <Line
          x1={CX} y1={CY}
          x2={magX} y2={magY}
          stroke={Colors.gold}
          strokeWidth={1}
          strokeDasharray="3,2"
          opacity={0.7}
        />
      </Svg>

      {/* Declination label */}
      <Text style={styles.dec}>DEC {decSign}°</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    alignItems: 'center',
  },
  dec: {
    fontFamily: Fonts.mono,
    fontSize: 8,
    color: Colors.gold,
    opacity: 0.7,
    marginTop: 2,
    letterSpacing: 0.5,
  },
});
