import { StatusBar } from 'expo-status-bar';
import { View, StyleSheet } from 'react-native';

import { DataValidationScreen } from './src/screens/DataValidationScreen';

export default function App() {
  return (
    <View style={styles.root}>
      <StatusBar style="light" />
      <DataValidationScreen />
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: '#0a0e14',
  },
});
