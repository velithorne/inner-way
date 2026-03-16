# Infold Core

Structure-aware platform for folding data, patterns, relationships, and behavior into reusable, reconstructable families.

**Core idea:** Store families instead of copies.

## Release Notes

### v0.2.0 (Core freeze)

- **Package spec v1** formalized: required manifest, ledger, shared/, maps/, reports/, snapshots/
- **Operators**: exact_repetition, symbol_table, template_skeleton, hierarchy_mirror, dependency_motif
- **Exact-mode guarantees**: byte-for-byte reconstruction, deterministic unfold
- **Template reliability**: fixed slot_ratio (slot lines not blocks), rejection diagnostics
- **Infold Archive v0.1**: create, inspect, validate, reconstruct commands

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

## Infold Archive

Create, inspect, validate, and reconstruct folded archives:

```bash
# Create archive (fold + export + zip)
python3 -m infold.cli archive create --source . --output archive.infold

# Inspect archive contents
python3 -m infold.cli archive inspect archive.infold

# Validate package spec
python3 -m infold.cli archive validate archive.infold

# Reconstruct to directory
python3 -m infold.cli archive reconstruct archive.infold --output ./restored
```

## License

MIT
