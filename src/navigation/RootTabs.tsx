import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Ionicons } from '@expo/vector-icons';
import { Platform, StyleSheet } from 'react-native';
import { EarthScreen } from '../screens/EarthScreen';
import { ZoneScreen } from '../screens/ZoneScreen';
import { COLORS } from '../constants/theme';

const Tab = createBottomTabNavigator();

export function RootTabs() {
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarStyle: styles.tabBar,
        tabBarShowLabel: false,
        tabBarActiveTintColor: COLORS.cyan,
        tabBarInactiveTintColor: 'rgba(0, 255, 200, 0.35)',
      }}
    >
      <Tab.Screen
        name="Zone"
        component={ZoneScreen}
        options={{
          tabBarIcon: ({ color, focused }) => (
            <Ionicons
              name="locate"
              size={26}
              color={focused ? COLORS.cyan : color}
              style={focused ? styles.iconGlow : undefined}
            />
          ),
        }}
      />
      <Tab.Screen
        name="Earth"
        component={EarthScreen}
        options={{
          tabBarIcon: ({ color, focused }) => (
            <Ionicons
              name="globe-outline"
              size={26}
              color={focused ? COLORS.cyan : color}
              style={focused ? styles.iconGlow : undefined}
            />
          ),
        }}
      />
    </Tab.Navigator>
  );
}

const styles = StyleSheet.create({
  tabBar: {
    position: 'absolute',
    backgroundColor: '#000000',
    borderTopColor: 'rgba(0, 255, 200, 0.2)',
    borderTopWidth: 1,
    height: Platform.OS === 'ios' ? 56 : 56,
  },
  iconGlow: {
    shadowColor: COLORS.cyan,
    shadowOpacity: 0.85,
    shadowRadius: 8,
  },
});
