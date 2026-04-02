# FALCOR v2 — Physics-First Discovery Harness

A reproducible, falsification-driven multi-physics and measurement test platform for desktop use.

## Features

- **SIM mode**: Fully simulated sensors and physics engine
- **FIELD mode**: Reads real sensor streams (stub fallback when unavailable)
- **Deterministic runs**: Seed-based reproducibility
- **Immutable manifests**: Hash-snapshot per run
- **Energy accounting**: Pin, Pmech, Pthermal, PEM, losses with uncertainty
- **Blind trials**: Randomized conditions, sealed truth, falsification scoring
- **Publishable artifacts**: Configs, manifests, plots, reports

## Requirements

- Python 3.11+
- Windows 10/11 or Linux (desktop only, no GPU required)

## Quick Install

```bash
cd falcor_v2
pip install -e .
```

## Quick Start

```bash
# GUI
falcor gui

# CLI demo sweep
falcor run --device falcor/devices/examples/witches_hat_v1.json --mode SIM --plan plans/demo_sweep.json
```

See [docs/QUICKSTART.md](docs/QUICKSTART.md) for details.

## Documentation

- [QUICKSTART.md](docs/QUICKSTART.md) — Install and run demo
- [USER_GUIDE.md](docs/USER_GUIDE.md) — Dashboard, calibration, sweeps
- [SCIENCE_MODEL.md](docs/SCIENCE_MODEL.md) — Equations and assumptions
- [ARCHITECTURE.md](docs/ARCHITECTURE.md) — Code structure
- [VALIDATION_PLAN.md](docs/VALIDATION_PLAN.md) — Sanity checks
- [SAFETY_SCOPE.md](docs/SAFETY_SCOPE.md) — Software-only scope

## License

MIT License. See [LICENSE](LICENSE).
