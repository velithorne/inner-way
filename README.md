# Biofield Scanner

Citizen science mobile app (React Native + Expo) implementing the **Biofield Scanner Blueprint v1.0**: multi-sensor fusion for experimental biological signal detection on consumer phones.

**Important:** This is an experimental instrument. It does not diagnose medical conditions or prove “biofields.” All readings are labeled as experimental.

## Requirements

- Node.js 18+
- For sensor development: physical **Android** or **iOS** device with [Expo Go](https://expo.dev/go) or a development build

## Setup

```bash
npm install
npx expo start
```

Then open the project in Expo Go (scan QR) or press `a` / `i` for emulators. **Magnetometer and accelerometer do not work meaningfully in most simulators**—use a real device for Phase 1 validation.

## What is implemented

- **Phase 1 (core):** DSP filters (`src/dsp/`), FFT, accelerometer cardiac pipeline, magnetometer cardiac pipeline, BPS fusion (`src/services/bioSensor.ts`), Zustand store (`src/store/useBioStore.ts`)
- **Phase 2 (minimal):** Scan screen with BPS gauge, live stats, scan modes (CONTACT / PROXIMITY / SWEEP), ethics disclaimer

Roadmap items from the blueprint (calibration flows, AR overlay, maps, Supabase sync, research mode) are not built yet.

## Validation protocol (from blueprint)

With **START** running on a real device:

1. **Phase A (0–10 s):** Phone on a stable table — note BPS.
2. **Phase B (10–20 s):** Phone flat on chest — BPS should rise vs Phase A (target: +15 BPS on most runs when the accelerometer path is working).
3. **Phase C (20–30 s):** Back on table — BPS should fall toward baseline.

## Legacy

The previous minimal README and HTML helper files are preserved under `_legacy/`.

## Android APK (release build)

APKs are produced with **EAS Build** (cloud). This repo includes `eas.json` with a **`preview`** profile that sets `android.buildType` to **`apk`** for direct sideloading.

### One-time setup

1. Install dependencies: `npm install`
2. Log in to Expo: `npx eas-cli login`
3. Link the app to an EAS project (writes `expo.extra.eas.projectId` in `app.json`):  
   `npx eas-cli init --non-interactive`
4. Commit the updated `app.json` and push.

### Build from your machine

```bash
npm run build:android:apk
```

When the build finishes, the CLI prints a **build details URL**. Open it in a browser — that is your **release page** with logs and a **Download** button for the `.apk`. You can share that link for installation on Android devices.

### Build from GitHub Actions

Add a repository secret **`EXPO_TOKEN`** (create under [Expo access tokens](https://expo.dev/settings/access-tokens)). Then run **Actions → Android APK (EAS) → Run workflow**. The workflow waits for the build and prints the same build URL in the job log.

### GitHub Releases (optional)

EAS hosts the artifact; to attach the APK to a GitHub Release, download the `.apk` from the build page and upload it to a [GitHub Release](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository) for your repository.

## License

Private / project-specific — adjust as needed for your GitHub org.
