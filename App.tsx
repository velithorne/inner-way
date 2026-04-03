import { StatusBar } from 'expo-status-bar';
import { View, StyleSheet } from 'react-native';

import { SiliconScreen } from './src/screens/SiliconScreen';

export default function App() {
  return (
    <View style={styles.root}>
      <StatusBar style="light" />
      <SiliconScreen />
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: '#0a0e14',
  },
});
