# The Resonance Map — Phase 1

**A cinematic real-time electromagnetic field visualiser for Android.**

Dark intelligence display. Cerebro meets scientific instrument.

---

## What It Does

- Reads the device magnetometer at **60 Hz**
- Renders a **live 3D vector field** (Three.js via expo-gl) with animated field lines that physically bend toward the measured magnetic vector
- Detects **anomalies** when field magnitude deviates >15% from a 30-second rolling baseline
- Logs every anomaly with **GPS coordinates**, timestamp and full xyz readings
- **Simulation mode** when running on a device without a magnetometer (shows realistic sine-wave data)
- **Haptic feedback** on every anomaly detection
- **Calibration routine** — 8-second figure-8 motion to remove hard-iron offset
- Screen stays awake while scanning (`expo-keep-awake`)

---

## Tech Stack

| Layer | Library |
|-------|---------|
| Framework | React Native + Expo SDK 54 |
| Sensor | `expo-sensors` Magnetometer |
| 3D render | `three` + `expo-gl` |
| 2D overlays | `react-native-svg` |
| Animation | `react-native-reanimated` 3 |
| State | `zustand` |
| Storage | `@react-native-async-storage/async-storage` |
| Navigation | `@react-navigation/native` |
| Fonts | Share Tech Mono + Orbitron |

---

## Aesthetic

- Background: `#00000A` (near-black)
- Field lines: `#00FFE5` (cyan/teal)
- Anomaly: `#FFB700` (gold)
- Readouts: green-on-black terminal style
- Headers: Orbitron
- Data: Share Tech Mono

---

## File Structure

```
src/
  screens/
    FieldScreen.tsx       ← main visualisation + layout
    CalibrationScreen.tsx ← figure-8 calibration modal
    AnomalyLogScreen.tsx  ← past detections log
  services/
    magnetometer.ts       ← sensor subscription, anomaly detection, calibration
    anomalyLog.ts         ← AsyncStorage persistence + GPS tagging
  components/
    FieldCanvas.tsx       ← Three.js expo-gl 3D render
    Waveform.tsx          ← SVG scrolling waveform
    DataReadout.tsx       ← terminal data panel
  store/
    useFieldStore.ts      ← Zustand global state
  constants/
    theme.ts              ← colours, fonts, spacing
    thresholds.ts         ← field strength and anomaly constants
```

---

## Building the APK

### Option 1: EAS Build (cloud, recommended — no SDK required)

1. Create a free account at [expo.dev](https://expo.dev)
2. Log in: `eas login`
3. Configure project: `eas build:configure`
4. Build preview APK:
   ```bash
   cd resonance-map
   eas build --platform android --profile preview
   ```
5. Download the `.apk` from the EAS dashboard link printed after the build.

### Option 2: Local build (requires Android SDK + JDK 17)

```bash
cd resonance-map
npx expo run:android --variant release
```

The APK will be at `android/app/build/outputs/apk/release/app-release.apk`.

### Option 3: Development build (instant, for testing)

```bash
cd resonance-map
npx expo start
```

Scan QR code with **Expo Go** app. Note: expo-gl (Three.js) requires a **development build**, not Expo Go for 3D rendering.

---

## Phase 2+ Integration Hooks

The codebase has clearly marked stubs for upcoming phases:

- **`PHASE3_SYNC_ENDPOINT`** in `thresholds.ts` — server sync endpoint
- **`syncAnomalyLog()`** in `anomalyLog.ts` — uploads pending entries once connected
- **`onSchumannDataReceived`** in `useFieldStore.ts` — Schumann resonance feed hook
- `AnomalyEntry.synced` flag tracks what's been uploaded

Phase 2: Map view + sacred sites database  
Phase 3: Server sync + Schumann resonance feed  
Phase 4: AR camera overlay
