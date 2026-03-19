# Phase 18A — Anchor Files v0.1 Results

## Summary

Phase 18A introduces **Anchor Files v0.1**: a conservative metadata layer that detects files that define or stabilize the structure of nearby files (package roots, manifests, config files) and records them for reporting and future metadata reduction.

## Implementation

### 1. Anchor Model

- **anchor file**: Canonical filename (package.json, pyproject.toml, etc.)
- **anchor type**: package_root, manifest, project_metadata, dependency_descriptor, config_root, registry_index
- **anchor scope**: directory (files in same dir or subdirs)
- **anchored members**: Count of nearby files within scope
- **metadata_reduction_estimate**: Conservative estimate for future use

### 2. Canonical Anchor Filenames

- package.json, manifest.json
- pyproject.toml, setup.py, setup.cfg
- Cargo.toml, go.mod, requirements.txt, Pipfile
- tsconfig.json, webpack.config.js
- Makefile, README.md, index.html
- config.json, settings.json

### 3. Detection

- Deterministic: exact filename match only
- Scope: files in same directory or subdirs (max_scope_depth=3)
- Root anchors (e.g. package.json at project root) anchor all project files

### 4. Package Representation

- `shared/anchors.json`: `{ "anchors": [...] }` when anchors detected
- Optional: no validation failure if absent

### 5. Reporting

- `anchor_metrics`: anchors_detected, anchor_types, total_anchored_count, metadata_reduction_estimate
- `anchor_families`: path, type, anchored_count per anchor
- Archive explain: "Anchor Files (Phase 18A)" section

## Configuration

```json
"anchor_file": {"enabled": true}
```

## Usage

Anchor Files are metadata-only. They do not fold content. They:
- Improve explainability (which files organize the project)
- Record anchored family relationships
- Provide metadata_reduction_estimate for future optimization

## Tests

- test_anchor_filename_detection
- test_find_anchors_empty
- test_find_anchors_mixed_project
- test_archive_with_anchors
- test_explain_shows_anchors

## Verification

- All Phase 18A tests pass
- Verification sweep passes
- Exact reconstruction unchanged (anchors are metadata-only)
- Strict validation passes
