import 'react-native-reanimated';
import React, { useEffect, useState } from 'react';
import { View, Text, ActivityIndicator, StyleSheet, TouchableOpacity } from 'react-native';
import { NavigationContainer, DefaultTheme } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { useFonts } from 'expo-font';
import { ShareTechMono_400Regular } from '@expo-google-fonts/share-tech-mono';
import { Orbitron_500Medium, Orbitron_700Bold } from '@expo-google-fonts/orbitron';
import AsyncStorage from '@react-native-async-storage/async-storage';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
} from 'react-native-reanimated';

import { AppState, AppStateStatus } from 'react-native';
import MapScreen      from './src/screens/MapScreen';
import ARFieldScreen  from './src/screens/ARFieldScreen';
import CalibrationScreen from './src/screens/CalibrationScreen';
import { startMagnetometer, stopMagnetometer, resetMagnetometerBaseline } from './src/services/magnetometer';
// FieldScreen kept in codebase — removed from nav as per Phase 3 restructure
// import FieldScreen from './src/screens/FieldScreen';

import { Colors, Fonts, FontSizes, Spacing, BorderWidth } from './src/constants/theme';

const Tab   = createBottomTabNavigator();
const Stack = createNativeStackNavigator();

const DARK_THEME = {
  ...DefaultTheme,
  dark: true,
  colors: {
    ...DefaultTheme.colors,
    background: Colors.background,
    card: Colors.background,
    text: Colors.cyan,
    border: Colors.grey,
    primary: Colors.cyan,
    notification: Colors.gold,
  },
};

const CALIBRATION_SEEN_KEY = '@resonance_map_calibration_seen';

// ── Custom 2-tab bar ──────────────────────────────────────────────────────
function TabBar({ state, descriptors, navigation }: any) {
  return (
    <View style={tabStyles.bar}>
      {state.routes.map((route: any, index: number) => {
        const { options } = descriptors[route.key];
        const isFocused = state.index === index;
        const label = options.tabBarLabel ?? route.name;
        const icon = route.name === 'Map' ? '◈' : '◉';

        const opacity = useSharedValue(isFocused ? 1 : 0.4);
        useEffect(() => {
          opacity.value = withTiming(isFocused ? 1 : 0.4, { duration: 300 });
        }, [isFocused]);
        const animStyle = useAnimatedStyle(() => ({ opacity: opacity.value }));

        return (
          <TouchableOpacity
            key={route.key}
            style={tabStyles.tab}
            onPress={() => {
              const event = navigation.emit({ type: 'tabPress', target: route.key, canPreventDefault: true });
              if (!isFocused && !event.defaultPrevented) navigation.navigate(route.name);
            }}
            activeOpacity={0.7}
          >
            <Animated.View style={[tabStyles.inner, animStyle]}>
              <Text style={[tabStyles.icon, isFocused && tabStyles.iconActive]}>{icon}</Text>
              <Text style={[tabStyles.label, isFocused && tabStyles.labelActive]}>{label}</Text>
            </Animated.View>
            {isFocused && <View style={tabStyles.indicator} />}
          </TouchableOpacity>
        );
      })}
    </View>
  );
}

const tabStyles = StyleSheet.create({
  bar: {
    flexDirection: 'row',
    backgroundColor: Colors.backgroundPanel,
    borderTopWidth: BorderWidth.thin,
    borderTopColor: Colors.grey,
    height: 56,
  },
  tab: { flex: 1, alignItems: 'center', justifyContent: 'center', position: 'relative' },
  inner: { alignItems: 'center', gap: 2 },
  icon: { fontSize: 16, color: Colors.greyLight },
  iconActive: { color: Colors.cyan },
  label: { fontFamily: Fonts.mono, fontSize: 9, color: Colors.greyLight, letterSpacing: 2 },
  labelActive: { color: Colors.cyan },
  indicator: {
    position: 'absolute',
    top: 0,
    left: '20%',
    right: '20%',
    height: 1.5,
    backgroundColor: Colors.cyan,
  },
});

// ── 2-tab navigator ───────────────────────────────────────────────────────
function MainTabs() {
  return (
    <Tab.Navigator
      tabBar={(props) => <TabBar {...props} />}
      screenOptions={{ headerShown: false }}
      initialRouteName="Map"
    >
      <Tab.Screen name="Map" component={MapScreen}     options={{ tabBarLabel: 'MAP' }} />
      <Tab.Screen name="AR"  component={ARFieldScreen} options={{ tabBarLabel: 'AR' }} />
    </Tab.Navigator>
  );
}

// ── Root stack ────────────────────────────────────────────────────────────
function RootNavigator({ initialRoute }: { initialRoute: string }) {
  return (
    <Stack.Navigator
      initialRouteName={initialRoute === 'Calibration' ? 'Calibration' : 'Main'}
      screenOptions={{ headerShown: false, animation: 'fade' }}
    >
      <Stack.Screen name="Main"        component={MainTabs} />
      <Stack.Screen name="Calibration" component={CalibrationScreen} />
    </Stack.Navigator>
  );
}

// ── App ───────────────────────────────────────────────────────────────────
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
        setInitialRoute('Main');
      }
    })();
  }, []);

  // Start magnetometer once globally — never stop/start on tab switch
  useEffect(() => {
    startMagnetometer();
    const sub = AppState.addEventListener('change', (state: AppStateStatus) => {
      if (state === 'background' || state === 'inactive') {
        stopMagnetometer();
        resetMagnetometerBaseline();
      } else if (state === 'active') {
        startMagnetometer();
      }
    });
    return () => {
      sub.remove();
      stopMagnetometer();
    };
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
      <RootNavigator initialRoute={initialRoute} />
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
