# Inner Way

> A Jarvis-like AI OS that runs your phone. Dark. Weird. Yours.

Inner Way is a custom launcher and AI assistant that replaces your home screen. It speaks to you, learns how you interact, and runs entirely on your device by default.

---

## Design

- **[DESIGN.md](../DESIGN.md)** — Full design document (foundation, features, MVP)
- **[docs/AI_MODEL_ARCHITECTURE.md](docs/AI_MODEL_ARCHITECTURE.md)** — Custom AI model architecture

---

## Setup

### Requirements

- Android Studio (Ladybug or newer recommended)
- JDK 17
- Android device or emulator (API 26+)

### Build

1. Open `inner-way/` in Android Studio (it will create `local.properties` with your SDK path)
2. Sync Gradle (Android Studio will download the wrapper if needed)
3. Run on device or emulator

Or from command line: ensure `ANDROID_HOME` is set, then run `./gradlew assembleDebug`

### Set as Default Launcher

When you first open Inner Way, Android will ask if you want to use it as your home screen. Choose **Inner Way** and tap **Always**.

---

## MVP Goal

**"When it talks to me by itself"**

- Dark red/black/grey UI ✓ (scaffold in place)
- Launcher as home screen ✓ (scaffold in place)
- Voice in/out + proactive speech — *next*

---

## Project Structure

```
inner-way/
├── app/                 # Launcher + UI
├── docs/                # Architecture, AI design
├── DESIGN.md            # Full design doc
└── README.md
```

---

## License

TBD
