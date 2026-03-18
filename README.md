# Infold Core

Structure-aware platform for folding data, patterns, relationships, and behavior into reusable, reconstructable families.

**Core idea:** Store families instead of copies.

## Release Notes

### v0.2.0 (Core freeze)

- **Package spec v1** formalized: required manifest, ledger, shared/, maps/, reports/, snapshots/
- **Operators**: exact_repetition, symbol_table, template_skeleton, hierarchy_mirror, dependency_motif, byte_fold
- **Exact-mode guarantees**: byte-for-byte reconstruction, deterministic unfold
- **Template reliability**: fixed slot_ratio (slot lines not blocks), rejection diagnostics
- **Infold Archive v0.1**: create, inspect, validate, reconstruct commands
- **Byte Fold v0.1**: chunk-based folding for mixed/binary-like content (shared/chunks/, maps/chunk_reconstruction.json)

See [docs/PACKAGE_SPEC.md](docs/PACKAGE_SPEC.md) for full package spec and limitations.

## Benchmark Summary (v0.2.0)

| Dataset | Files | Raw | ZIP | Gzip | Infold logical gain | Infold physical | Folds | Reconstruction |
|---------|-------|-----|-----|------|---------------------|-----------------|-------|----------------|
| duplicate-heavy-python | 6 | 3,351 | 2,118 | 1,046 | 190 | 3,161 | 1 exact | ok |
| template-heavy | 3 | 422 | 660 | 149 | 127 | 295 | 1 template | ok |
| config-heavy | 6 | 442 | 1,016 | 192 | 79 | 363 | 1 exact | ok |
| infold-workspace | 145 | 275,786 | 92,791 | 44,956 | 28,235 | 247,551 | 10 exact, 1 hierarchy, 5 dep | ok |
| mixed-small | 5 | 340 | 823 | 254 | 0 | 340 | 0 | ok |
| hierarchy-mirror | 6 | 249 | 1,019 | 122 | 20 | 229 | 1 hierarchy | ok |
| dependency-motif | 3 | 320 | 558 | 123 | 50 | 270 | 1 dep | ok |

Run `python3 -m infold.cli --benchmark-suite` for live results.

## Scope

- **In scope:** Python, JavaScript/TypeScript, JSON/YAML configs, Markdown, plain text
- **Out of scope:** Binaries, images, video, encrypted files, already compressed archives

## Setup

```bash
# Create virtual environment
python -m venv .venv
source .venv/bin/activate  # or .venv\Scripts\activate on Windows

# Install in editable mode
pip install -e .
```

## Configuration

Default configuration is in `infold/config.json`. Override with a project-local config file.

## Fold Profiles (Phase 6A)

Adaptive Origami Profiles change fold behavior by size, structure, and context:

| Profile | Use case |
|---------|----------|
| **auto** | Default; selects sparrow/fox/dragon/golem/serpent by project metrics |
| **sparrow** | Tiny archives, overhead-sensitive |
| **fox** | Balanced structured projects |
| **dragon** | Large structure-rich archives |
| **golem** | Binary/opaque-heavy content |
| **serpent** | Lineage/snapshot-oriented workflows |

```bash
# Auto-select profile (default)
python3 -m infold.cli archive create --source . --output archive.infold

# Manual profile
python3 -m infold.cli archive create --source . --output archive.infold --profile dragon

# Profile comparison benchmark (Phase 6B): run all datasets across all profiles
python3 -m infold.cli --profile-compare --profile-compare-output results/pc

# Creature comparison (Phase 6D): static vs adaptive behavior
python3 -m infold.cli --creature-compare --creature-compare-output results/creature_compare

# Disable creature adaptation (static profile only)
python3 -m infold.cli archive create --source . --output archive.infold --profile fox --no-creature
```

## Adaptive Origami Creatures (Phase 6D)

Profiles are extended with a bounded adaptive strategy layer. Each profile becomes a *species* with measurable traits that can shift within a run based on archive signals (opaque ratio, template density, metadata pressure, lineage context, etc.). Adaptation is deterministic and explainable.

- **Traits**: compactness_bias, structure_bias, byte_fold_bias, lineage_bias, analysis_depth, metadata_tolerance, risk_tolerance
- **Signals**: raw_size, file_count, structured_ratio, opaque_ratio, parser_confidence, duplicate/template/hierarchy density, chunk_reuse_potential, lineage_context
- **Behavior mapping**: planner min_net_value, compact package preference, metadata table fold aggressiveness

Creature info is recorded in manifest, report, and archive explain. Use `--no-creature` to disable adaptation.

## Tesseract Fold (Phase 10)

Multi-dimensional family identity layer: structure, byte reuse, metadata reuse, temporal persistence. Enriches search, sync trace, and explain without changing operators or archive format.

```bash
# Search with tesseract (structure, byte, metadata, time dimensions)
python3 -m infold.cli archive search archive.infold --with-tesseract

# Sync search with tesseract (+ lineage for temporal dimension)
python3 -m infold.cli archive sync search --dir .infold-sync --with-tesseract --with-lineage

# Sync trace (always includes tesseract)
python3 -m infold.cli archive sync trace --dir .infold-sync

# Archive explain (includes Tesseract section)
python3 -m infold.cli archive explain archive.infold
```

## Tesseract Planner (Phase 10B)

Meta-planning layer using multi-dimensional identity to guide routing, operator priority, and planner biases:

- **Routes**: structural_first, byte_first, metadata_sensitive, lineage_sensitive, balanced
- **Operator family priority**: structural > metadata > byte (or variants by dominant dimension)
- **Planner bias**: favor_physical_size, favor_logical_gain, favor_compactness, favor_lineage_continuity

Shown in archive explain, report, and manifest.

## Tesseract Cooperation (Phase 10C)

Conservative orchestration: short execution plans for structural→metadata and byte→metadata cooperation.

- **structural_then_metadata**: Structural folds then metadata compaction (lower min_net threshold)
- **byte_then_metadata**: Byte fold then metadata compaction
- **primary_only**: No cross-family chaining (structure+byte, time-dominant, mixed)

Shown in archive explain, report, manifest.

## Tesseract Evaluation (Phase 10D)

Measure the effect of Tesseract Planner and Cooperation on physical size, logical gain, and metadata cleanup:

```bash
# Run Tesseract evaluation: baseline vs planner vs cooperation
python3 -m infold.cli --tesseract-evaluate --tesseract-evaluate-output results/tesseract_evaluate
```

Exports JSON, CSV, and Markdown with cooperation win summary, per-dataset per-mode metrics, and recommendations.

## Lean Mode and Operating Styles (Phase 11)

Infold supports two operating styles:

| Style | Use case | Flags |
|-------|----------|-------|
| **Size-first** | Smallest archive, production | `--lean` or `--profile golem --no-creature --no-tesseract` |
| **Analysis-rich** | Full diagnostics, research | Default (auto, creature, Tesseract on) |

```bash
# Size-first: smallest archive (compact, no creature, no Tesseract)
python3 -m infold.cli archive create --source . --output archive.infold --lean

# Size-first with explicit profile
python3 -m infold.cli archive create --source . --output archive.infold --profile golem --no-creature --no-tesseract

# Analysis-rich: default (full creature + Tesseract)
python3 -m infold.cli archive create --source . --output archive.infold

# Disable only Tesseract (keep creature)
python3 -m infold.cli archive create --source . --output archive.infold --no-tesseract

# Disable only Tesseract Cooperation (planner stays on)
python3 -m infold.cli archive create --source . --output archive.infold --no-tesseract-cooperation
```

Manifest records `creature_enabled`, `tesseract_planner_enabled`, `tesseract_cooperation_enabled`. Use `archive explain` to see which features were used.

## Quick Workflows (Phase 8)

```bash
# Analyze project (no archive): fold + concise summary
python3 -m infold.cli analyze --source .

# Archive workflow: create + validate + summary in one
python3 -m infold.cli archive workflow --source . --output archive.infold

# Sync capture: add snapshot + timeline summary
python3 -m infold.cli archive sync init --dir .infold-sync --source .
python3 -m infold.cli archive sync capture --dir .infold-sync --source .

# Real-project showcase: create, validate, reconstruct, save results
python3 -m infold.cli archive showcase --source . --output-dir results/showcase
```

## Infold Archive

Create, inspect, validate, explain, compare, and reconstruct folded archives:

```bash
# Create archive (fold + export + zip)
python3 -m infold.cli archive create --source . --output archive.infold

# Workflow: create + validate + summary in one command
python3 -m infold.cli archive workflow --source . --output archive.infold

# Showcase: full workflow, save to directory (archive, restored, summary.md)
python3 -m infold.cli archive showcase --source . --output-dir results/showcase

# Inspect archive contents
python3 -m infold.cli archive inspect archive.infold

# Explain: fold counts, gain contributors, rejected candidates, reconstruction guarantees
python3 -m infold.cli archive explain archive.infold

# Validate package spec (ledger consistency, shared artifacts, manifest/report)
python3 -m infold.cli archive validate archive.infold

# List shared artifacts and fold families
python3 -m infold.cli archive list archive.infold

# Detailed stats (per-operator, template purity, rejection counts)
python3 -m infold.cli archive stats archive.infold

# Compare two archives (includes which families changed)
python3 -m infold.cli archive compare archive1.infold archive2.infold

# Search within archive(s) (Infold Search v1.0)
# Path: .infold file or directory of .infold archives
python3 -m infold.cli archive search archive.infold --operator template_skeleton
python3 -m infold.cli archive search archive.infold --with-tesseract  # multi-dimensional family identity
python3 -m infold.cli archive search archive.infold --path foo.py --explain
python3 -m infold.cli archive search ./archives_dir --group-by operator
python3 -m infold.cli archive search ./archives_dir --source-path template --min-logical-gain 50
python3 -m infold.cli archive search ./archives_dir --debug-friendly true --created-after 2024-01-01
python3 -m infold.cli archive search archive.infold --rejected --planner-decision reject_conflict
python3 -m infold.cli archive search archive.infold --output results.json --export json
python3 -m infold.cli archive search archive.infold --export csv
python3 -m infold.cli archive search archive.infold --export markdown

# JSON output (inspect, explain, list, stats, compare, search)
python3 -m infold.cli archive inspect archive.infold --json

# Reconstruct to directory
python3 -m infold.cli archive reconstruct archive.infold --output ./restored

# Infold Sync v0.1 — versioned snapshots, lineage, compare, report
python3 -m infold.cli archive sync init --dir .infold-sync --source .
python3 -m infold.cli archive sync add --dir .infold-sync --source .
python3 -m infold.cli archive sync list --dir .infold-sync
python3 -m infold.cli archive sync validate --dir .infold-sync
python3 -m infold.cli archive sync summary --dir .infold-sync
python3 -m infold.cli archive sync search --dir .infold-sync --operator template_skeleton

# Phase 7 — Lineage insights
python3 -m infold.cli archive sync timeline --dir .infold-sync
python3 -m infold.cli archive sync trace --dir .infold-sync --family template
python3 -m infold.cli archive sync lineage-report --dir .infold-sync
python3 -m infold.cli archive sync search --dir .infold-sync --with-lineage
python3 -m infold.cli archive sync compare v1.infold v2.infold
python3 -m infold.cli archive sync compare latest previous --dir .infold-sync
python3 -m infold.cli archive sync report v1.infold v2.infold
python3 -m infold.cli archive sync diff --dir .infold-sync
python3 -m infold.cli archive sync report-diff --dir .infold-sync
python3 -m infold.cli archive sync reconstruct latest --dir .infold-sync --output ./restored
```

### Demo workflow

Full example using the duplicate-heavy fixture:

```bash
# 1. Create archive from a small project
python3 -m infold.cli archive create --source tests/fixtures/duplicate_python --output demo.infold
# Created: /path/to/demo.infold

# 2. Inspect
python3 -m infold.cli archive inspect demo.infold
# Archive: /path/to/demo.infold
# Source: /path/to/tests/fixtures/duplicate_python
# Files: 8, Folds: 2
# Logical gain: 1,107 bytes
# Physical folded: 5,125 bytes

# 3. Explain (fold counts, gain contributors, reconstruction guarantees)
python3 -m infold.cli archive explain demo.infold
# Archive Explain
# ===============
# Path: /path/to/demo.infold
# Package summary:
#   file_count: 8
#   fold_count: 2
#   logical_gain_bytes: 1,107
#   ...
# Fold counts by operator:
#   exact_repetition: 2
# Biggest gain contributors:
#   exact_repetition: 1,107 bytes
# Reconstruction guarantees:
#   mode=deterministic
#   min_infold=0.2.0
#   debug_friendly=True

# 4. Validate
python3 -m infold.cli archive validate demo.infold
# Valid

# 5. Reconstruct
python3 -m infold.cli archive reconstruct demo.infold --output ./restored
# Reconstructed 8 files to ./restored

# 6. Verify (optional)
diff -rq tests/fixtures/duplicate_python ./restored
# (no output = identical)
```

### Search workflow examples

```bash
# Find template families
python3 -m infold.cli archive search archive.infold --operator template_skeleton

# Find folds involving a path
python3 -m infold.cli archive search archive.infold --path foo.py --explain

# Search across lineage with lineage metadata
python3 -m infold.cli archive sync search --dir .infold-sync --with-lineage --operator exact_repetition

# Export to file
python3 -m infold.cli archive search archive.infold --output results.json --export json
```

### Sync / timeline workflow examples

```bash
# Initialize and capture snapshots
python3 -m infold.cli archive sync init --dir .infold-sync --source .
python3 -m infold.cli archive sync capture --dir .infold-sync --source .   # add + timeline

# Trace family lifecycle
python3 -m infold.cli archive sync trace --dir .infold-sync --family template

# Change-focused report
python3 -m infold.cli archive sync lineage-report --dir .infold-sync
```

### Profile usage guidance

| When to use | Profile |
|-------------|---------|
| Default (let Infold choose) | `auto` |
| Tiny project, minimize overhead | `sparrow` or `golem` |
| Balanced codebase | `fox` |
| Large, structure-rich | `dragon` |
| Binary/opaque-heavy | `golem` |
| Versioned/sync workflow | `serpent` |

### Real-project case study

Run a full showcase on your project:

```bash
python3 -m infold.cli archive showcase --source . --output-dir results/my_project_showcase
```

Output: `archive.infold`, `restored/`, `summary.md`, `summary.json`, `explain.txt`, `stats.txt`.
Compare raw vs ZIP vs gzip vs Infold in `summary.md`.

## License

MIT
