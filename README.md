# Infold Core v1

Structure-aware platform for folding data, patterns, relationships, and behavior into reusable, reconstructable families.

**Core idea:** Store families instead of copies.

## Current Build Target

Infold Core v1 — the first working fold engine for code projects and structured text.

## Phase 1 Scope

- **In scope:** Python, JavaScript/TypeScript, JSON/YAML configs, Markdown, plain text
- **Out of scope:** Binaries, images, video, encrypted files, already compressed archives

## Build Sequence (Step-by-Step)

1. ✅ Create repository skeleton and configuration files
2. Implement core data classes
3. Implement intake layer for scanning and filtering project files
4. Implement Python parser and text fallback parser
5. Implement base operator interface
6. Implement validation pipeline
7. Implement Exact Repetition Fold v1
8. Implement fold engine orchestration
9. Implement reporting and benchmark runner
10. Test on small projects, then Template Skeleton Fold

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

## License

MIT
