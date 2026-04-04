# PHANTOM — The Invisible City

React Native (Expo) app that visualizes WiFi as a 3D world. This repo implements **Phase 0** (production-first pipeline + dev client defaults) and **Phase 1** (Android WiFi scanner + validation UI) from the PHANTOM blueprint.

## Run (never use Expo Go for performance work)

```bash
npm install
npx eas-cli build -p android --profile development   # install dev client APK once
npm start                                              # expo start --dev-client
```

Open the **development build** on device, not Expo Go.

## What is in this branch

- `expo-dev-client`, `eas.json` profiles (`development` / `preview` / `production`)
- Native Android `WifiScanModule` (`WifiManager.scanResults`) + JS `WifiScanner` with 2s polling
- `ValidationScreen`: live network list (RSSI, band, channel, distance estimate), spinning Three.js cube for FPS baseline, triple-tap title to toggle FPS counter
- Package id: `com.phantom.app`

## Android release APK (no Metro on device)

Push a `v*` tag; CI runs Gradle `assembleRelease` on the committed `android/` tree (includes custom WiFi native code — **do not** run `expo prebuild` in CI without re-applying native edits).

## License

Private / project-specific.
