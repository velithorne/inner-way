# FALCOR-CIVILIZATION

An offline-first, desktop-like Android app that runs a phone-hosted self-adversarial research lab ("scientific civilization") for experiment design, execution, adversarial artifact testing, preregistration, blinding, replication scoring, and auto-generated paper-style reports.

## Features

- **Offline-first**: No cloud required. All data stored locally.
- **Deterministic & replayable**: Any run can be re-executed from saved config + seed.
- **Tamper-evident ledger**: Hash-chained append-only proof ledger (SHA-256).
- **Preregistration + blind analysis**: Analysis plan frozen (hashed) before labels/reveals.
- **Self-adversarial loop**:
  - **ScientistAgent**: Proposes hypotheses, metrics, stopping rules.
  - **SkepticAgent**: Injects artifacts (thermal drift, RPM ripple, EMI, etc.) to test robustness.
  - **AuditorAgent**: Enforces integrity (energy bounds, drift checks, BH-FDR correction).
  - **ReplicatorAgent**: Reruns findings with new seeds.
- **SIM MODE**: Realistic sensor streams with artifacts. No hardware required for MVP.

## Tech Stack

- Kotlin + Jetpack Compose
- Room (SQLite)
- Coroutines + Flow
- Kotlin Serialization

## Building

1. Create `local.properties` with your Android SDK path:
   ```
   sdk.dir=/path/to/Android/sdk
   ```
   Or set `ANDROID_HOME` environment variable.

2. Build:
   ```bash
   ./gradlew assembleDebug
   ```

Or open in Android Studio and run.

## Running the Demo

1. Launch the app.
2. Select the default project "Falcor Device".
3. Tap **Demo Cycle** to run a short deterministic cycle.
4. View results in:
   - **QUEUE**: Experiment runs and statuses
   - **ARENA**: Adversary vs detector (injections, catches)
   - **LEDGER**: Proof ledger with Verify button
   - **REPORTS**: Findings and export

## Adding New Hypotheses

Edit `ScientistAgent.kt` and add to `hypothesisTemplates`:

```kotlin
Triple(
    "Your Hypothesis Title",
    "Description of the hypothesis",
    listOf("metric1", "metric2", "metric3")
)
```

## Adding New Artifacts

Edit `ArtifactInjector.kt`:

1. Add a new case in `inject()` for your artifact type.
2. Register in `ARTIFACT_LIBRARY`:

```kotlin
"your_artifact" to { amp: Double -> ArtifactSpec("your_artifact", amp, ...) }
```

## Hardware Integration (Future)

Implement `HardwareStreamProvider` in `engine/hardware/` for serial/BLE sensor streams. The app uses SIM mode by default.

## File Storage

Per-run storage:
```
/storage/.../Android/data/<package>/files/projects/<projectId>/runs/<runId>/
  config.json
  artifacts.json
  raw.csv
  analysis.json
  report.md
```

Global ledger: `ledger.jsonl` (append-only)

## Export

Export a "Repro Bundle" ZIP from the Reports tab:
- config.json
- artifacts.json
- raw.csv
- analysis.json
- report.md
- ledger slice

## Unit Tests

```bash
./gradlew test
```

Tests cover:
- Ledger hashing/verification
- Benjamini-Hochberg FDR correction
- Prereg freeze rule (hash determinism)
- Sim determinism given seed

## Safety

This app is an integrity engine. It tests, falsifies, and reports. It does NOT provide exotic energy extraction instructions or claim to break physics. All experiments are limited to signal integrity, sensor systems, and simulated effects.
