# Phase 10B — Tesseract Planner v0.1 Results

## Summary

Tesseract Planner adds a **meta-planning/routing layer** that uses multi-dimensional family identity (structure, byte, metadata, time) to guide routing, operator family priority, and small planner biases. Deterministic, additive, conservative.

## What Tesseract Planner Adds

- **Tesseract profile schema**: structure_strength, byte_strength, metadata_strength, time_strength, dominant_dimension, secondary_dimension, route, operator_family_priority, planner_bias
- **Dimension strength computation** from creature-style signals (structured_ratio, opaque_ratio, parser_confidence, duplicate/template/hierarchy density, metadata_overhead_pressure, lineage_context)
- **Routing integration**: Tesseract route (structural_first, byte_first, metadata_sensitive, lineage_sensitive, balanced) influences borderline file routing when conf 0.45–0.65 and route=byte_first
- **Operator family priority**: structural > metadata > byte (or variants by dominant dimension)
- **Planner bias**: favor_physical_size, favor_logical_gain, favor_compactness, favor_lineage_continuity as modest guidance
- **Visibility**: report, explain, manifest (route, dominant, operator_priority)

## How Routing and Operator Priority Use Multi-Dimensional Identity

- **Routing**: Session-level route chosen from dimension strengths. When route=byte_first and file has borderline parser confidence (0.45–0.65), nudge to chunk_first. Fallback: existing routing logic.
- **Operator priority**: Operator family (structural/byte/metadata) gets small bonus when it matches tesseract operator_family_priority rank.
- **Planner bias**: Small additive terms to final_net_value from planner_bias (favor_physical_size, etc.).

## Dimensions Supported

| Dimension | Signals used |
|-----------|--------------|
| structure | structured_ratio, parser_confidence, duplicate/template/hierarchy/dependency density |
| byte | opaque_ratio, chunk_reuse_potential |
| metadata | metadata_overhead_pressure, file_count |
| time | lineage_context |

## Exact Reconstruction and Validation

- **Exact reconstruction**: Intact. Tesseract Planner only adds small biases and routing nudges; no changes to fold operators or correctness.
- **Validation**: Intact. All 198 tests pass, verification sweep passes.

## CLI / Output

```bash
# Archive explain shows Tesseract Planner
python3 -m infold.cli archive explain archive.infold

# Report includes tesseract_planner (route, dominant, operator_priority, planner_bias)
# Manifest includes tesseract_planner_route, tesseract_planner_dominant, tesseract_planner_operator_priority
```

## Example Output

```
Tesseract Planner:
  route=balanced (mixed_signals)
  dominant=structure secondary=metadata
  operator_priority=structural>metadata>byte
```
