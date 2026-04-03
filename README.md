# DECODE

Mobile app (React Native + Expo) for **DECODE — Reality Source Code** (Blueprint v1.0): a unified sensor-visualization instrument. This repository currently implements the **Biofield Scanner** sensor core (multi-sensor fusion, BPS gauge, scan modes) as the foundation for the broader DECODE roadmap.

**Important:** This is an experimental instrument. It does not diagnose medical conditions or prove “biofields.” All readings are labeled as experimental.

## Requirements

- Node.js 18+
- For sensor development: physical **Android** or **iOS** device with [Expo Go](https://expo.dev/go) or a development build

## Setup

```bash
npm install
npx expo start
```

Then open the project in Expo Go (scan QR) or press `a` / `i` for emulators. **Magnetometer and accelerometer do not work meaningfully in most simulators**—use a real device for validation.

## What is implemented

- **Core:** DSP filters (`src/dsp/`), FFT, accelerometer cardiac pipeline, magnetometer cardiac pipeline, BPS fusion (`src/services/bioSensor.ts`), Zustand store (`src/store/useBioStore.ts`)
- **UI:** Scan screen with BPS gauge, live stats, scan modes (CONTACT / PROXIMITY / SWEEP), ethics disclaimer

Roadmap items from the full DECODE blueprint (five layers, modes, AR, maps, sync) are not built yet.

## Validation protocol (biofield pipeline)

With **START** running on a real device:

1. **Phase A (0–10 s):** Phone on a stable table — note BPS.
2. **Phase B (10–20 s):** Phone flat on chest — BPS should rise vs Phase A (target: +15 BPS on most runs when the accelerometer path is working).
3. **Phase C (20–30 s):** Back on table — BPS should fall toward baseline.

## Legacy

The previous minimal README and HTML helper files are preserved under `_legacy/`.

## Android APK (same as other velithorne projects — no Expo token)

**Default release path:** push a **`v*`** tag → GitHub Actions runs **`expo prebuild`** + **Gradle `assembleRelease`** (embeds the JS bundle — **no Metro required** on the phone) → uploads **`DECODE-<tag>-release.apk`**. **No `EXPO_TOKEN`** required.

```bash
git tag v1.0.0-decode
git push origin v1.0.0-decode
```

**Download:** [github.com/velithorne/inner-way/releases/latest](https://github.com/velithorne/inner-way/releases/latest)

Workflow: `.github/workflows/release-apk.yml`

### Optional: EAS cloud build (Expo servers)

If you want an Expo-hosted build instead, add repo secret **`EXPO_TOKEN`** and run **Actions → Android APK release (EAS + GitHub)** manually (`.github/workflows/android-apk-eas.yml`). See `npm run build:android:apk` and `scripts/android-release.sh`.

## License

Private / project-specific — adjust as needed for your GitHub org.
