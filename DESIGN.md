# Inner Way — Personal AI OS Design

> A Jarvis-like AI that runs your phone. Dark. Weird. Yours.

---

## 1. Foundation

**Approach:** Option C — App/launcher that replaces the home screen and system UI.

- Runs as default launcher on Android
- Full-screen takeover with custom UI
- Access to system APIs (calls, messages, calendar, etc.)
- No need to modify Android itself — we own the experience layer

**Target:** Android 8.0+ (API 26+) for broad device support.

---

## 2. Interface & Aesthetic

### Visual Design
- **Palette:** Red, black, grey
  - Primary black: `#0a0a0a`, `#111111`
  - Grey scale: `#1a1a1a`, `#2a2a2a`, `#404040`, `#6b6b6b`
  - Red accents: `#8b0000`, `#b22222`, `#dc143c`, `#ff4444`
  - Text: `#e0e0e0` (primary), `#a0a0a0` (secondary)
- **Vibe:** Dark, slightly unsettling, industrial, terminal-like
- **Typography:** Monospace or geometric sans — nothing soft or rounded

### Input
- **Voice:** Primary for commands and conversation
- **Touch:** Gestures, taps, swipes for navigation and control
- Both always available; context determines which feels natural

### Output
- **Voice:** TTS for responses, notifications, proactive speech
- **Screen:** Minimal UI — information when needed, otherwise ambient or dark

### Personality
- **Adaptive:** Learns from how you interact
  - Formal vs casual tone
  - Verbosity (brief vs detailed)
  - Proactivity level (how often it speaks unprompted)
- Stored locally; no cloud dependency for personality tuning

---

## 3. Core Features (Full Vision)

| Feature | Description | MVP? |
|---------|-------------|------|
| **Calls** | Initiate, answer, manage calls via voice/touch | Phase 2 |
| **Messages** | Read, compose, send SMS/notifications | Phase 2 |
| **Calendar** | Events, reminders, scheduling | Phase 2 |
| **Smart Home** | Control devices (when connected) | Phase 2 |
| **Navigation** | Directions, traffic, places | Phase 2 |
| **Reminders** | Time/location-based alerts | Phase 2 |
| **Search** | Web, device, contacts | Phase 2 |
| **Media** | Music, podcasts, playback control | Phase 2 |
| **Proactive Voice** | Speaks to you unprompted | **MVP** |
| **Conversation** | Natural back-and-forth | **MVP** |

---

## 4. AI Model — Custom Design

### Philosophy
- Built from scratch (architecture designed by us)
- Runs **offline/local first**
- Cloud/internet available when you explicitly want it

### Architecture (Proposed)

```
┌─────────────────────────────────────────────────────────────┐
│                    INNER WAY AI STACK                        │
├─────────────────────────────────────────────────────────────┤
│  Intent Layer        │  Understands: commands, questions,   │
│  (NLU)               │  chitchat, proactive triggers         │
├─────────────────────────────────────────────────────────────┤
│  Memory Layer        │  Short-term (session), long-term     │
│                      │  (user prefs, facts, context)        │
├─────────────────────────────────────────────────────────────┤
│  Response Layer      │  Generates: text, actions, TTS       │
│  (Generation)        │  Custom small model or hybrid        │
├─────────────────────────────────────────────────────────────┤
│  Action Layer        │  Executes: calls, calendar, etc.    │
└─────────────────────────────────────────────────────────────┘
```

### Model Options (We Design Together)

1. **Hybrid Rule + Small NN**
   - Intent classification (small neural net)
   - Slot extraction for entities
   - Template + retrieval for responses
   - Fully local, fast, private

2. **Small Custom Transformer**
   - ~50–100M params, trainable on device or locally
   - Fine-tuned for your voice and style
   - Requires ML pipeline (data, training, export)

3. **Retrieval-Augmented**
   - Local embedding model
   - Response database you curate
   - Combines retrieval + light generation

**Recommendation for MVP:** Start with (1) — hybrid rule + small NN. Gets you talking quickly. Evolve to (2) or (3) as we refine.

### Privacy
- All processing on-device by default
- Cloud only when you say "search the web" or similar
- No telemetry; no data leaves device unless you ask

---

## 5. MVP Definition

**"When it talks to me by itself"**

- [ ] Launcher runs as default home screen
- [ ] Dark, red/black/grey UI
- [ ] Voice input (STT) works
- [ ] Voice output (TTS) works
- [ ] AI responds to what you say
- [ ] **AI initiates conversation** — greets you, gives time/weather, asks how you are, reminds you of something
- [ ] Basic adaptive behaviour (e.g. remembers you prefer short answers)

---

## 6. Tech Stack

| Layer | Technology |
|-------|------------|
| **Platform** | Android (Kotlin) |
| **UI** | Jetpack Compose — custom, no Material by default |
| **Launcher** | Custom `Launcher` + `LauncherApps` API |
| **STT** | Android SpeechRecognizer (on-device) or Vosk (fully offline) |
| **TTS** | Android TextToSpeech (on-device voices) |
| **AI** | Custom (see above) — Kotlin + possibly ONNX/TFLite for NN parts |
| **Persistence** | Room DB for memory, preferences, logs |

---

## 7. Project Structure (Proposed)

```
inner-way/
├── app/                    # Main launcher + UI
├── core/                   # AI, STT, TTS, memory
├── features/               # Calls, messages, calendar, etc. (later)
├── design/                 # Assets, themes
└── docs/                   # This file, architecture notes
```

---

## 8. Timeline

No rush. We build in phases:

- **Phase 1:** Launcher shell + dark UI + basic voice loop (you speak → AI responds)
- **Phase 2:** Proactive voice (it talks first)
- **Phase 3:** Adaptive personality
- **Phase 4:** Full feature set (calls, messages, calendar, etc.)
- **Phase 5:** Custom model refinement

---

*Next step: Set up the Android project and launcher scaffold.*
