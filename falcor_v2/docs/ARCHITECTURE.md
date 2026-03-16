# FALCOR v2 Architecture

## Module Layout

```
falcor/
  core/       Config, manifest, paths, logging, rng, units, timebase, errors
  devices/    Virtual assembly, materials, base device
  physics/    Thermal, mechanics, EM, coupling
  measurement/ Sensors, artifacts, pipelines
  experiments/ Plan, sweep, executor, scoring, energy_audit, reports
  storage/    Dataset, writers, readers, exports
  cli/        Main, commands
  ui/         Main window, widgets, 3D viewport
```

## Separation of Concerns

- **Core**: No physics, no UI. Pure utilities.
- **Physics**: No measurement, no storage. First-principles only.
- **Measurement**: No physics internals. Sensors, artifacts, pipelines.
- **Experiments**: Orchestrates physics + measurement + storage.
- **Storage**: Parquet, JSON, CSV. No business logic.
- **UI**: Presentation only. Calls experiments/storage.

## Data Flow

```
Config + Device
    → ExperimentPlan
    → RunExecutor
    → Physics (thermal, EM, mechanics)
    → Measurement (sensors, artifacts)
    → Storage (parquet, JSON)
    → Reports + Audit
```

## Unit Discipline

- All internal computation: **SI units**.
- Conversions at boundaries (e.g. RPM ↔ rad/s).

## Determinism

- RNG seeded from config + run_id.
- `create_run_rng(run_id, seed)` for reproducible runs.

## Manifest

- Hash of: config snapshot, device assembly, git commit, lib versions.
- Immutable per run.
