# PHANTOM — The Invisible City

React Native (Expo) app that visualizes WiFi as a 3D world. This repo implements **Phase 0** (production-first pipeline + dev client defaults), **Phase 1** (Android WiFi scanner + validation list), and **Phase 2** (SIGNAL tab: AR camera + expanding wave rings) from the PHANTOM blueprint.

## Run (never use Expo Go for performance work)

**One-shot (after you have an Expo account):** link the project, commit `projectId`, and build the dev-client APK:

```bash
npm install
export EXPO_TOKEN=xxxxxxxx   # https://expo.dev/settings/access-tokens — or use `npx eas-cli login` instead
npm run eas:onboard            # runs scripts/setup-eas-and-dev-build.sh
```

Or step by step:

```bash
npm install
npx eas-cli login            # if you prefer not to use EXPO_TOKEN locally
npm run eas:init             # writes expo.extra.eas.projectId — commit app.json
npm run build:android:dev    # EAS development profile → install APK on phone
npm start                    # expo start --dev-client — open from installed dev client
```

**GitHub Actions (optional):** add repo secret `EXPO_TOKEN`, then run workflow **Android APK release (EAS + GitHub)** to build in the cloud.

Open the **development build** on device, not Expo Go.

## What is in this branch

- `expo-dev-client`, `eas.json` profiles (`development` / `preview` / `production`)
- Native Android `WifiScanModule` (`WifiManager.scanResults`) + JS `WifiScanner` with 2s polling
- **Tabs:** SIGNAL (AR waves), NETWORKS (Phase 1 validation + cube FPS), CITY / FIELD placeholders
- **SIGNAL:** `react-native-vision-camera` + transparent `expo-gl` overlay; up to **20** strongest networks as additive ring wavefronts (`WaveEmitter` / `WaveScene`); triple-tap HUD for FPS
- **NETWORKS:** live list (RSSI, band, channel, FSPL distance), spinning cube, triple-tap title for FPS
- Package id: `com.phantom.app`

## Android release APK (no Metro on device)

Push a `v*` tag; CI runs Gradle `assembleRelease` on the committed `android/` tree (includes custom WiFi native code — **do not** run `expo prebuild` in CI without re-applying native edits).

## License

Private / project-specific.
