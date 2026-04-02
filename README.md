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

## Android APK (same flow as other Cursor / Expo projects)

This repo mirrors the usual setup: **`origin` → GitHub**, repo secret **`EXPO_TOKEN`**, **EAS** cloud build, optional **GitHub Release** with the `.apk` attached.

| Step | What to do |
|------|------------|
| 1 | `git remote add origin https://github.com/ORG/REPO.git` and push this branch |
| 2 | [Expo access token](https://expo.dev/settings/access-tokens) → add as GitHub secret **`EXPO_TOKEN`** |
| 3 | One-time link: `npx eas-cli login` then `npx eas-cli init --non-interactive` → commit **`app.json`** (gets `expo.extra.eas.projectId`) |
| 4 | **CI:** Actions → **Android APK release (EAS + GitHub)** → Run workflow — or push tag **`v1.0.0`** |

The workflow (`.github/workflows/android-apk-eas.yml`) runs the same commands as below: **`eas build -p android --profile preview`**, downloads **`applicationArchiveUrl`**, uploads **`biofield-scanner.apk`** to a GitHub Release. The job summary shows **`https://github.com/ORG/REPO/releases/tag/...`**.

### Local machine (identical commands to CI)

After `eas init` has written `projectId` into `app.json`:

```bash
export EXPO_TOKEN=...   # same token as the GitHub secret
npm run release:android
```

Or only the cloud build (Expo prints the [build details / APK](https://docs.expo.dev/build-reference/apk/) URL):

```bash
npm run build:android:apk
```

`scripts/android-release.sh` is the same pipeline as the workflow: EAS build → `curl` the APK → optional `gh release create` if the GitHub CLI is logged in.

## License

Private / project-specific — adjust as needed for your GitHub org.
