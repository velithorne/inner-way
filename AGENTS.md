# AGENTS.md

## Repository overview

This is a multi-project repository with 4 independent products on separate Git branches. The `main` branch contains only a README and a static HTML page. Each project lives on its own feature branch:

| Branch | Project | Tech Stack |
|---|---|---|
| `cursor/digital-monster-game-6571` | Monster Soul (web game) | React 19, Vite 7, Three.js, Zustand |
| `cursor/falcor-v2-system-foundation-e1e7` | FALCOR v2 (physics harness) | Python 3.11+, PySide6, NumPy, SciPy |
| `cursor/personal-ai-os-6913` | Inner Way AI OS | Kotlin, Jetpack Compose (Android) |
| `cursor/falcor-civilization-core-8c2c` | FALCOR Civilization | Kotlin, Jetpack Compose (Android) |

All projects are offline-first with no external API/database dependencies.

## Cursor Cloud specific instructions

### Monster Soul (Web — `monster-game/`)

- **Dev server**: `cd monster-game && npm run dev -- --host 0.0.0.0` (serves on `:5173`)
- **Lint**: `cd monster-game && npm run lint` — pre-existing lint warnings/errors exist (Math.random purity, unused vars); these are not regressions.
- **Build**: `cd monster-game && npm run build`
- **Install deps**: `cd monster-game && npm install`
- Camera access is optional; the app gracefully shows "Skip (Random Monster)" when no camera is available.

### FALCOR v2 (Python — `falcor_v2/`)

- **Install**: `cd falcor_v2 && pip install -e ".[dev]"` — the `falcor` CLI is installed to `~/.local/bin`, which must be on `PATH` (`export PATH="$HOME/.local/bin:$PATH"`).
- **Tests**: `cd falcor_v2 && pytest` (24 tests covering solvers, RNG, manifests, energy audit, etc.)
- **Lint**: `cd falcor_v2 && ruff check .` — pre-existing lint issues exist.
- **Run a simulation**: `cd falcor_v2 && falcor run --device falcor/devices/examples/witches_hat_v1.json --mode SIM --seed 42` — results go to `lab_results/runs/<run_id>/`.
- **GUI**: `falcor gui` requires a display (PySide6/Qt). In headless environments, use CLI commands instead.

### Android projects (Inner Way, FALCOR Civilization)

These require Android SDK + JDK 17 and are not runnable in standard Cloud Agent VMs. They build via `./gradlew assembleDebug` and have GitHub Actions workflows for CI.

### Key gotchas

- Each project is on a separate branch. To work on a specific project, either switch to that branch or use `git checkout <branch> -- <dir>/` to bring its files into your working tree.
- The `falcor` CLI help uses positional subcommands (not `--help`). Run bare `falcor` to see usage.
- Monster Soul uses `package-lock.json` (npm), not pnpm/yarn.
