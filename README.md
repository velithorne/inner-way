# DECODE

**DECODE — Reality Source Code** (Blueprint v1.0). This app renders phone sensor data as a unified visual layer over the camera. This repository implements the roadmap in phases; **Phase 1** is the **Acoustic Shell** — room geometry from microphone impulse response (not a Biofield Scanner clone).

**Important:** Experimental instrument. Not medical advice.

## Requirements

- Node.js 18+
- **Physical Android or iOS device** — Phase 1 needs camera + microphone + speaker

## Setup

```bash
npm install
npx expo start
```

## Phase 1 (current): Acoustic Shell

Per the blueprint:

- **20–200 Hz log sine sweep** (~2 s) played via speaker while the mic records
- **Deconvolution** (sweep / recorded in frequency domain → impulse response)
- **Modal peaks** → estimated **width / height / depth** (enclosed space) or **open hemisphere** outdoors
- **Three.js wireframe** overlay (`#001850`) on the camera feed
- **Continuous:** sweep every **30 s** or after **>5 m** GPS movement (location permission)

Legacy **Biofield Scanner** prototype code is kept under `_legacy/biofield-scanner/` for reference only; it is not part of the app entry.

## Android APK (velithorne / inner-way)

Push a **`v*`** tag → GitHub Actions builds **`DECODE-<tag>-release.apk`** (Gradle `assembleRelease`, no `EXPO_TOKEN`).

```bash
git tag v1.1.0-decode-phase1
git push origin v1.1.0-decode-phase1
```

**Download:** [github.com/velithorne/inner-way/releases/latest](https://github.com/velithorne/inner-way/releases/latest)

Optional EAS: add **`EXPO_TOKEN`** → Actions → **Android APK release (EAS + GitHub)**.

## License

Private / project-specific — adjust as needed for your GitHub org.
