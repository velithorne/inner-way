import 'react-native-reanimated';
import React, { useEffect, useState } from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import { NavigationContainer, DefaultTheme } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useFonts } from 'expo-font';
import {
  ShareTechMono_400Regular,
} from '@expo-google-fonts/share-tech-mono';
import {
  Orbitron_500Medium,
  Orbitron_700Bold,
} from '@expo-google-fonts/orbitron';
import AsyncStorage from '@react-native-async-storage/async-storage';

import FieldScreen from './src/screens/FieldScreen';
import CalibrationScreen from './src/screens/CalibrationScreen';
import AnomalyLogScreen from './src/screens/AnomalyLogScreen';
import { Colors } from './src/constants/theme';

const Stack = createNativeStackNavigator();

const DARK_THEME = {
  ...DefaultTheme,
  dark: true,
  colors: {
    ...DefaultTheme.colors,
    background: Colors.background,
    card: Colors.backgroundPanel,
    text: Colors.cyan,
    border: Colors.grey,
    primary: Colors.cyan,
    notification: Colors.gold,
  },
};

const CALIBRATION_SEEN_KEY = '@resonance_map_calibration_seen';

export default function App() {
  const [fontsLoaded] = useFonts({
    ShareTechMono_400Regular,
    Orbitron_500Medium,
    Orbitron_700Bold,
  });
  const [initialRoute, setInitialRoute] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      const seen = await AsyncStorage.getItem(CALIBRATION_SEEN_KEY);
      if (!seen) {
        await AsyncStorage.setItem(CALIBRATION_SEEN_KEY, '1');
        setInitialRoute('Calibration');
      } else {
        setInitialRoute('Field');
      }
    })();
  }, []);

  if (!fontsLoaded || !initialRoute) {
    return (
      <View style={styles.splash}>
        <ActivityIndicator color={Colors.cyan} size="large" />
      </View>
    );
  }

  return (
    <NavigationContainer theme={DARK_THEME}>
      <Stack.Navigator
        initialRouteName={initialRoute}
        screenOptions={{ headerShown: false, animation: 'fade' }}
      >
        <Stack.Screen name="Field" component={FieldScreen} />
        <Stack.Screen name="Calibration" component={CalibrationScreen} />
        <Stack.Screen name="AnomalyLog" component={AnomalyLogScreen} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}

const styles = StyleSheet.create({
  splash: {
    flex: 1,
    backgroundColor: Colors.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
