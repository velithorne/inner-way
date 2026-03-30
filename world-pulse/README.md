# World Pulse

> One word. Right now. The whole planet.

World Pulse is a dark, cinematic, real-time planetary interface — a living emotional consciousness display for the globe. Phase 1 is a polished visual prototype with no backend: everything is simulated to make the globe feel alive from the first second.

---

## Screenshots

Open the app and you'll immediately see:
- A slowly rotating dark Earth with atmospheric glow
- Anonymous emotion pulses appearing across the globe
- An emotion selection panel to add your own pulse
- A subtle starfield and cinematic lighting

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | React 19 + TypeScript |
| 3D Rendering | Three.js + React Three Fiber |
| 3D Helpers | Drei (OrbitControls) |
| Shaders | Custom GLSL (atmosphere, earth surface, pulse rings) |
| Animation | Framer Motion (UI) + useFrame (3D) |
| State | Zustand |
| Build | Vite |

---

## Getting Started

```bash
# 1. Enter the project directory
cd world-pulse

# 2. Install dependencies
npm install

# 3. Start the dev server
npm run dev
```

Then open [http://localhost:5173](http://localhost:5173).

### Production Build

```bash
npm run build
npm run preview
```

---

## Project Structure

```
src/
├── app/
│   └── store.ts              # Zustand global state
├── components/
│   ├── Globe/
│   │   ├── GlobeScene.tsx    # R3F Canvas + camera + lights
│   │   ├── EarthMesh.tsx     # Procedural dark Earth with GLSL
│   │   ├── AtmosphereLayer.tsx # Rim-lit atmospheric glow
│   │   ├── PulseSystem.tsx   # Renders all active PulseMarkers
│   │   ├── PulseMarker.tsx   # Per-pulse animated ring system
│   │   └── StarField.tsx     # Ambient star particles
│   └── UI/
│       ├── AppShell.tsx      # Top-level layout + hook bootstrap
│       ├── EmotionSelector.tsx # Emotion chip grid
│       ├── SubmissionPanel.tsx # Prompt + submit flow
│       ├── LiveStatusBadge.tsx # Active pulse count indicator
│       └── DebugControls.tsx # Developer controls panel
├── data/
│   ├── emotions.ts           # Emotion palette & config
│   ├── mockPulses.ts         # Pulse factory functions
│   └── demoLocations.ts      # Demo cities + regional bounds
├── hooks/
│   ├── useMockEngine.ts      # Interval-driven pulse spawner
│   └── useGlowAccumulation.ts # Derives globe glow from pulse density
├── lib/
│   ├── geo.ts                # lat/lon → Vector3 conversion
│   ├── colors.ts             # Emotion → Three.Color helpers
│   └── animation.ts          # Easing + pulse progress utils
├── shaders/
│   ├── earth.glsl.ts         # Procedural earth surface shader
│   ├── atmosphere.glsl.ts    # Rim-light atmosphere shader
│   └── pulse.glsl.ts         # Expanding ring pulse shader
└── types/
    └── index.ts              # EmotionKey, PulseEvent, GeoPoint, etc.
```

---

## How Pulse Rendering Works

Each `PulseEvent` has a `lat`, `lon`, `emotion`, `startTime`, and `duration`.

**Coordinate mapping:** `latLonToVector3()` in `lib/geo.ts` converts geographic coordinates into a 3D point on the globe's surface using the spherical coordinate transform. A quaternion is derived so the pulse ring faces the surface normal.

**Ring animation:** Each pulse renders 3 staggered rings using a custom GLSL fragment shader (`pulse.glsl.ts`). The shader takes a `uProgress` uniform (0→1) and draws a thin luminous band at that fractional radius, with a soft glow falloff. The rings are staggered by 0, 1/3, and 2/3 of the cycle so they appear to cascade outward. Scale is driven by `ringScale()` (easeOutExpo) and opacity by `pulseOpacity()` (fade in → hold → fade out envelope).

**Blending:** All pulse geometry uses `THREE.AdditiveBlending` with `depthWrite: false` so overlapping pulses naturally accumulate brightness, creating the bioluminescence effect in dense areas.

**Cleanup:** The `useMockEngine` hook runs `cleanExpiredPulses()` every 2 seconds, removing pulses whose `startTime + duration` has elapsed.

---

## Emotion System

Emotions are defined in `src/data/emotions.ts` as an array of `EmotionDefinition` objects:

```typescript
{
  key: 'electric',
  label: 'Electric',
  color: '#00f5ff',
  glowStrength: 1.0,
  soundCategory: 'high-spark', // placeholder for Phase 2
}
```

To add a new emotion, append to the `EMOTIONS` array and add its `EmotionKey` to the union type in `src/types/index.ts`.

---

## Mock Live Mode

The `useMockEngine` hook drives the simulated activity:

- Fires on a configurable interval (default 1800ms)
- Has a burst probability that triggers multi-pulse events
- Supports region biasing (global / urban / asia / europe / americas)

All settings are exposed in the Debug panel (bottom-right corner, ⚙ Debug button).

**Debug panel features:**
- Toggle live mode on/off
- Tune interval speed (400ms → 5000ms)
- Adjust burst probability and size
- Select region bias
- Fire demo city pulses (Tokyo, New York, London, etc.)
- Storm Density button — triggers 12 simultaneous pulses

---

## Phase 2 Integration Points

When the backend is ready, these are the exact seams to replace:

| Phase 1 (mock) | Phase 2 (real) |
|---|---|
| `createUserPulse()` in `mockPulses.ts` | Replace with `navigator.geolocation.getCurrentPosition()` |
| `useMockEngine` hook | Replace/supplement with WebSocket subscription |
| `createMockPulse()` | Ingest from live event stream |
| `PulseEvent.startTime` | Use server timestamp for sync |
| Zustand `addPulse` | Remains unchanged — just feed real data into it |

The entire visual and animation layer needs zero changes for Phase 2.

---

## Performance Notes

- Earth sphere uses 128×128 segments for a smooth silhouette
- StarField uses `BufferGeometry` with pre-allocated Float32Arrays
- Pulse markers use additive blending with `depthWrite: false` to avoid expensive depth testing
- `cleanExpiredPulses` prevents unbounded pulse accumulation
- Vite HMR keeps iteration fast during development

---

## Emotion Palette

| Key | Label | Color |
|---|---|---|
| heavy | Heavy | Deep Indigo `#3b1f6e` |
| electric | Electric | Bright Cyan `#00f5ff` |
| hollow | Hollow | Void Grey `#6b7280` |
| tender | Tender | Warm Rose `#f472b6` |
| anxious | Anxious | Sharp Amber `#f59e0b` |
| calm | Calm | Soft Teal `#2dd4bf` |
| raw | Raw | Deep Crimson `#991b1b` |
| wonder | Wonder | Gold `#f6c90e` |
| numb | Numb | Muted Slate `#475569` |
| alive | Alive | Vivid Green `#22c55e` |
