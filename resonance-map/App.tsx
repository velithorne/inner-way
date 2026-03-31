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

import FieldScreen from './src/screens/FieldScreen';
import ARFieldScreen from './src/screens/ARFieldScreen';
import MapScreen from './src/screens/MapScreen';
import CalibrationScreen from './src/screens/CalibrationScreen';
import AnomalyLogScreen from './src/screens/AnomalyLogScreen';
import { Colors, Fonts, FontSizes, Spacing, BorderWidth } from './src/constants/theme';

const Tab = createBottomTabNavigator();
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

// ── Custom tab bar ────────────────────────────────────────────────────────────
function TabBar({ state, descriptors, navigation }: any) {
  return (
    <View style={tabBarStyles.container}>
      {state.routes.map((route: any, index: number) => {
        const { options } = descriptors[route.key];
        const isFocused = state.index === index;
        const label = options.tabBarLabel ?? route.name;
        const icon = route.name === 'Field' ? '◈' : route.name === 'AR' ? '◉' : '◆';

        const opacity = useSharedValue(isFocused ? 1 : 0.45);
        useEffect(() => {
          opacity.value = withTiming(isFocused ? 1 : 0.45, { duration: 300 });
        }, [isFocused]);
        const animStyle = useAnimatedStyle(() => ({ opacity: opacity.value }));

        return (
          <TouchableOpacity
            key={route.key}
            style={tabBarStyles.tab}
            onPress={() => {
              const event = navigation.emit({ type: 'tabPress', target: route.key, canPreventDefault: true });
              if (!isFocused && !event.defaultPrevented) {
                navigation.navigate(route.name);
              }
            }}
            activeOpacity={0.7}
          >
            <Animated.View style={[tabBarStyles.inner, animStyle]}>
              <Text style={[tabBarStyles.icon, isFocused && tabBarStyles.iconActive]}>
                {icon}
              </Text>
              <Text style={[tabBarStyles.label, isFocused && tabBarStyles.labelActive]}>
                {label}
              </Text>
            </Animated.View>
            {isFocused && <View style={tabBarStyles.indicator} />}
          </TouchableOpacity>
        );
      })}
    </View>
  );
}

const tabBarStyles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    backgroundColor: Colors.backgroundPanel,
    borderTopWidth: BorderWidth.thin,
    borderTopColor: Colors.grey,
    height: 56,
  },
  tab: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
  },
  inner: {
    alignItems: 'center',
    gap: 2,
  },
  icon: {
    fontSize: 16,
    color: Colors.greyLight,
  },
  iconActive: {
    color: Colors.cyan,
  },
  label: {
    fontFamily: Fonts.mono,
    fontSize: 9,
    color: Colors.greyLight,
    letterSpacing: 1.5,
  },
  labelActive: {
    color: Colors.cyan,
  },
  indicator: {
    position: 'absolute',
    top: 0,
    left: '20%',
    right: '20%',
    height: 1.5,
    backgroundColor: Colors.cyan,
  },
});

// ── Tab navigator (Field + AR) ────────────────────────────────────────────────
function MainTabs() {
  return (
    <Tab.Navigator
      tabBar={(props) => <TabBar {...props} />}
      screenOptions={{ headerShown: false }}
    >
      <Tab.Screen
        name="Field"
        component={FieldScreen}
        options={{ tabBarLabel: 'FIELD' }}
      />
      <Tab.Screen
        name="AR"
        component={ARFieldScreen}
        options={{ tabBarLabel: 'AR' }}
      />
      <Tab.Screen
        name="Map"
        component={MapScreen}
        options={{ tabBarLabel: 'MAP' }}
      />
    </Tab.Navigator>
  );
}

// ── Root stack (tabs + modal screens) ────────────────────────────────────────
function RootNavigator({ initialRoute }: { initialRoute: string }) {
  return (
    <Stack.Navigator
      initialRouteName={initialRoute === 'Calibration' ? 'Calibration' : 'Main'}
      screenOptions={{ headerShown: false, animation: 'fade' }}
    >
      <Stack.Screen name="Main" component={MainTabs} />
      <Stack.Screen name="Calibration" component={CalibrationScreen} />
      <Stack.Screen name="AnomalyLog" component={AnomalyLogScreen} />
    </Stack.Navigator>
  );
}

// ── App entry ─────────────────────────────────────────────────────────────────
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
