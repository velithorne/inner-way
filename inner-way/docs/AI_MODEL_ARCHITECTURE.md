# Inner Way — Custom AI Model Architecture

> Designed from scratch. Runs locally. Adapts to you.

---

## Design Principles

1. **Offline-first** — All inference on device
2. **Privacy** — No data leaves unless you request cloud
3. **Adaptive** — Learns your interaction style over time
4. **Efficient** — Runs on phone hardware (CPU, optional GPU)

---

## MVP Architecture: Hybrid Rule + Small NN

For the first version ("when it talks to me by itself"), we use a hybrid approach:

```
User Input (voice/text)
        │
        ▼
┌───────────────────┐
│  Speech-to-Text    │  Android SpeechRecognizer or Vosk (offline)
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│  Intent Classifier │  Small NN or rule-based
│  (What does user   │  → greet, question, command, chitchat, goodbye
│   want?)           │
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│  Entity Extraction │  Slots: time, place, contact, topic
│  (Optional)        │  Rule-based or small NER
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│  Response Generator│  Template + retrieval + (optional) small LM
│  (What to say)     │  Local response database
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│  Text-to-Speech    │  Android TTS (on-device voices)
└───────────────────┘
```

---

## Components

### 1. Intent Classifier

**Options:**
- **A) Rule-based:** Keyword + pattern matching. Fast, no training. Limited.
- **B) Small NN:** 2–3 layer MLP, ~50k params. Trained on intent labels. Runs in TFLite.
- **C) Embedding + classifier:** Use small sentence embedding (e.g. MiniLM) + linear layer. Better generalization.

**MVP:** Start with (A), migrate to (B) when we have enough labeled data.

### 2. Response Generator

**Options:**
- **A) Templates:** "Good morning. It's {time}. {weather}." Fills slots. Predictable.
- **B) Retrieval:** Embed user input, find nearest response in local DB. More varied.
- **C) Small LM:** 50–100M param model. Generates from scratch. Most flexible, heaviest.

**MVP:** (A) + (B) — templates for structured (time, weather), retrieval for open-ended.

### 3. Proactive Triggers

The AI speaks first when:
- First launch / wake
- Time-based (e.g. morning greeting)
- Notification arrives (configurable)
- Location change (optional)
- User hasn't interacted for X hours (optional)

Stored in a simple rule engine: `Trigger → Condition → Action (say X)`.

### 4. Adaptive Personality

**Stored locally:**
- `verbosity`: 0–2 (brief, normal, detailed)
- `formality`: 0–2 (casual, neutral, formal)
- `proactivity`: 0–2 (quiet, normal, chatty)
- `topics_favoured`: Set of topics user engages with
- `phrases_user_likes`: Responses user reacted positively to

**Update rules:**
- If user says "shorter" / "brief" → verbosity--
- If user engages with a topic repeatedly → topics_favoured.add
- If user says "don't do that" → proactivity-- for that trigger

---

## Future: Custom Small Transformer

When we outgrow the hybrid approach:

### Architecture Sketch

- **Model type:** Decoder-only transformer (GPT-style)
- **Size:** 50–100M params
- **Context:** 512 tokens
- **Vocab:** BPE, ~10k tokens
- **Training:** 
  - Base: Train on curated dialogue data
  - Fine-tune: Your conversations (with your permission)
- **Inference:** TFLite or ONNX Runtime on device

### Data We'd Need

- Dialogue datasets (open source)
- Your interaction logs (you control)
- Synthetic data for commands (calls, calendar, etc.)

---

## File Structure (Proposed)

```
core/
├── ai/
│   ├── intent/          # Intent classification
│   ├── response/        # Response generation
│   ├── proactive/      # Trigger engine
│   └── memory/         # User prefs, context
├── voice/
│   ├── stt/            # Speech-to-text
│   └── tts/            # Text-to-speech
└── ...
```

---

*This document evolves as we build. Next: implement the voice loop (STT → AI → TTS) for MVP.*
