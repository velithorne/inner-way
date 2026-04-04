import { StyleSheet, Text, View } from 'react-native';

type Props = {
  title: string;
  phase: string;
};

export function PlaceholderScreen({ title, phase }: Props) {
  return (
    <View style={styles.root}>
      <Text style={styles.title}>{title}</Text>
      <Text style={styles.phase}>{phase}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: '#0a0e14',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  title: {
    color: '#eceff1',
    fontSize: 20,
    fontWeight: '700',
    marginBottom: 8,
  },
  phase: {
    color: '#78909c',
    fontSize: 14,
    textAlign: 'center',
  },
});
