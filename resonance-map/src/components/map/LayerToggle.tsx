import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Colors, Fonts, FontSizes, Spacing, BorderWidth } from '../../constants/theme';

export interface LayerState {
  myNodes: boolean;
  sacredSites: boolean;
  geology: boolean;
}

interface Props {
  layers: LayerState;
  onChange: (layers: LayerState) => void;
}

interface RowProps {
  icon: string;
  label: string;
  active: boolean;
  onToggle: () => void;
}

function ToggleRow({ icon, label, active, onToggle }: RowProps) {
  return (
    <TouchableOpacity style={[row.container, active && row.active]} onPress={onToggle} activeOpacity={0.7}>
      <Text style={[row.icon, active && row.iconActive]}>{icon}</Text>
      <Text style={[row.label, active && row.labelActive]}>{label}</Text>
      <View style={[row.pip, active && row.pipActive]} />
    </TouchableOpacity>
  );
}

const row = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.sm,
    paddingVertical: 7,
    gap: 6,
    opacity: 0.5,
  },
  active: { opacity: 1 },
  icon: { fontSize: 11, color: Colors.greyLight },
  iconActive: { color: Colors.cyan },
  label: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.greyLight, letterSpacing: 1, flex: 1 },
  labelActive: { color: Colors.cyan },
  pip: { width: 5, height: 5, borderRadius: 2.5, backgroundColor: Colors.grey },
  pipActive: { backgroundColor: Colors.cyan },
});

export default function LayerToggle({ layers, onChange }: Props) {
  return (
    <View style={styles.container}>
      <ToggleRow
        icon="◈"
        label="MY NODES"
        active={layers.myNodes}
        onToggle={() => onChange({ ...layers, myNodes: !layers.myNodes })}
      />
      <View style={styles.divider} />
      <ToggleRow
        icon="◆"
        label="SACRED SITES"
        active={layers.sacredSites}
        onToggle={() => onChange({ ...layers, sacredSites: !layers.sacredSites })}
      />
      <View style={styles.divider} />
      <ToggleRow
        icon="/"
        label="GEOLOGY"
        active={layers.geology}
        onToggle={() => onChange({ ...layers, geology: !layers.geology })}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: 'rgba(6,6,18,0.88)',
    borderWidth: BorderWidth.thin,
    borderColor: Colors.grey,
    borderRadius: 2,
    overflow: 'hidden',
    minWidth: 120,
  },
  divider: {
    height: BorderWidth.thin,
    backgroundColor: Colors.grey,
    opacity: 0.4,
    marginHorizontal: Spacing.sm,
  },
});
