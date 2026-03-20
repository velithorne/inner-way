# Phase 11 — Lean Mode and Overhead Reduction Results

## Summary

Phase 11 adds explicit feature switches and a size-first lean mode to reduce archive overhead from advanced strategy layers (Creatures, Tesseract Planner, Tesseract Cooperation).

## Implemented

1. **CLI feature switches**
   - `--no-creature` — disable creature adaptation
   - `--no-tesseract` — disable Tesseract Planner and Cooperation
   - `--no-tesseract-cooperation` — disable Cooperation only (planner stays on)
   - `--lean` — size-first mode (compact, no creature, no Tesseract, minimal metadata)

2. **Manifest recording**
   - `creature_enabled`, `tesseract_planner_enabled`, `tesseract_cooperation_enabled` always present
   - Rich creature/tesseract fields omitted when disabled or in lean mode

3. **Lean mode**
   - Compact package export
   - report_text off
   - Creature off, Tesseract off
   - Minimal report (no rejected_candidates_detail, no creature/tesseract rich fields)

4. **Operating styles**
   - Size-first: `--lean` or `--profile golem --no-creature --no-tesseract`
   - Analysis-rich: default (auto, creature, Tesseract on)

## Real-Project Comparison

| Mode | Archive Size | Logical Gain |
|------|--------------|--------------|
| auto --lean | **2,853,626** (smallest) | 5,009,612 |
| golem --no-creature --no-tesseract | 2,870,003 | 5,009,612 |
| golem --no-creature | 2,870,601 | 5,009,612 |
| auto (full) | 2,871,186 | 5,009,612 |

Lean mode improved physical size by ~17 KB vs full mode. Creature + Tesseract overhead: ~1.2 KB when both enabled.

## Exact Reconstruction and Validation

All modes preserve exact reconstruction and strict validation.
