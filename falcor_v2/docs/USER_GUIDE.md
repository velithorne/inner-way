# FALCOR v2 User Guide

## Overview

FALCOR v2 is a desktop-only, offline multi-physics and measurement platform for reproducible, falsification-driven experiments. It does **not** provide instructions to build real devices; all "devices" are parametric virtual assemblies.

## Modes

- **SIM mode**: Fully simulated sensors and physics engine. Default for development and validation.
- **FIELD mode**: Reads real sensor streams. When hardware is unavailable, uses stub generators with a warning.

## Main Window Layout

```
+------------------+----------------------+------------------+
| Device           |                      | Property Editor   |
| Experiment Plan  |   3D Viewport        | Sensor Status     |
| Run Controls     |  (virtual assembly)  | Calibration       |
+------------------+----------------------+------------------+
| Results | Plots | Energy Audit | Logs/Terminal                  |
+----------------------------------------------------------------+
```

## Device Picker

- Load a virtual assembly JSON (e.g. `witches_hat_v1.json`).
- The 3D viewport renders components as parametric shapes (disk, cylinder, coil, box).

## Experiment Builder

- **Frequency sweep**: Start/stop/step (Hz).
- **RPM target schedule**: Optional list of RPM values.
- **Magnet state schedule**: OFF, ON, or PROFILE.
- **Dwell time**: Duration per step (s).
- **Settle time**: Pre-sampling settle (s).
- **Sample rate**: Hz.

## Run Controls

- Click **Run** to generate a run_id and lock the config snapshot.
- All runs are deterministic when a seed is set.

## Results Table

Per-step columns: Step, Freq (Hz), Magnet state, T_mean, Slope, B, Vib, Pass.

## Plots

- Select metric(s) vs time or vs frequency.
- Export PNG via the Export button.

## Energy Audit

- Breakdown: Pin, Pmech, Pthermal, PEM, Ploss, unknown/unmodeled.
- Uncertainty ranges reported.
- Flags: impossible states (negative temps, nonphysical energy creation).

## Calibration

One-sensor-at-a-time workflow:

1. Capture baseline window (stable conditions).
2. Estimate offset, scale, noise_sigma, lag.
3. Store calibration profile per sensor.

Use the UI or `falcor calibrate --sensor temp_ir` for workflow guidance.

## CLI Commands

| Command | Description |
|---------|-------------|
| `falcor run -d <device.json> -m SIM` | Execute run |
| `falcor export -r <run_id> -f csv` | Export to CSV |
| `falcor report -r <run_id>` | Print audit report |
| `falcor calibrate -s <sensor_id>` | Calibration workflow |
| `falcor gui` | Launch GUI |

## Interpreting Energy Audit

- **Pin**: Motor + coil power.
- **Pmech**: Torque × omega (if modeled).
- **Pthermal**: Heat capacity × dT/dt.
- **PEM**: Delta of EM stored energy.
- **Ploss**: Remainder.
- **unknown/unmodeled**: Explicitly labeled.

Flags indicate model/data errors (e.g. negative absolute temps).
