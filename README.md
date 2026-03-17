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
```

## Infold Archive

Create, inspect, validate, explain, compare, and reconstruct folded archives:

```bash
# Create archive (fold + export + zip)
python3 -m infold.cli archive create --source . --output archive.infold

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

## License

MIT
