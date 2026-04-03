# SILICON

**SILICON** is a React Native + Expo app that implements the SILICON Blueprint v1.0 vision: your device as an explorable 3D world where hardware and live processes map to geography. This repository is bootstrapped for **Phase 1 — Android system data foundation**: a Kotlin native module exposes CPU, memory, running processes, network deltas, storage, and battery; the app shows a **data validation** screen (no 3D yet) so readings can be checked against Developer Options and system settings.

The previous **Biofield Scanner** prototype code lives under `_legacy/biofield-scanner/` for reference only.

## Requirements

- Node.js 18+
- **Android:** physical device or emulator for native `SystemData` module and validation UI

## Setup

```bash
npm install
npx expo start
```

Run on Android (`a`) or install a development build. The validation screen polls device data every 500ms.

## Android APK via GitHub Releases (no Expo token)

Push a **`v*`** tag. GitHub Actions runs `expo prebuild` + Gradle `assembleRelease` (JS bundle embedded; no Metro on device) and uploads **`SILICON-<tag>-release.apk`**.

```bash
git tag v1.0.0-silicon
git push origin v1.0.0-silicon
```

**Download:** open your repo on GitHub → **Releases** → latest release → APK asset.

Workflow: `.github/workflows/release-apk.yml`

### Optional: EAS cloud build

Add repository secret **`EXPO_TOKEN`** (from [expo.dev access tokens](https://expo.dev/settings/access-tokens)), then run **Actions → Android APK release (EAS + GitHub)**. See `npm run build:android:apk` and `scripts/android-release.sh`.

## Native module

- Android: `android/app/src/main/java/com/silicon/app/SystemDataModule.kt`
- JS bridge: `src/native/systemData.ts`

The `android/` folder is tracked in git so custom native code is not lost when CI runs prebuild.

## License

Private / project-specific — adjust as needed for your GitHub org.
