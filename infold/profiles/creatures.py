"""
Phase 6D: Adaptive Origami Creatures v0.1.

Species/traits/senses model with deterministic bounded adaptation.
Extends profiles; does not replace them.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

from infold.profiles.definitions import VALID_PROFILES

# Trait names (all in [0, 1] after normalization)
TRAIT_NAMES = (
    "compactness_bias",
    "structure_bias",
    "byte_fold_bias",
    "lineage_bias",
    "analysis_depth",
    "metadata_tolerance",
    "risk_tolerance",
)

# Base trait values per species (0–1 scale). Fox = baseline 0.5.
SPECIES_BASE_TRAITS: dict[str, dict[str, float]] = {
    "sparrow": {
        "compactness_bias": 0.9,
        "structure_bias": 0.4,
        "byte_fold_bias": 0.3,
        "lineage_bias": 0.2,
        "analysis_depth": 0.3,
        "metadata_tolerance": 0.3,
        "risk_tolerance": 0.2,
    },
    "fox": {
        "compactness_bias": 0.5,
        "structure_bias": 0.5,
        "byte_fold_bias": 0.5,
        "lineage_bias": 0.5,
        "analysis_depth": 0.5,
        "metadata_tolerance": 0.5,
        "risk_tolerance": 0.5,
    },
    "dragon": {
        "compactness_bias": 0.3,
        "structure_bias": 0.9,
        "byte_fold_bias": 0.4,
        "lineage_bias": 0.4,
        "analysis_depth": 0.9,
        "metadata_tolerance": 0.7,
        "risk_tolerance": 0.7,
    },
    "golem": {
        "compactness_bias": 0.8,
        "structure_bias": 0.3,
        "byte_fold_bias": 0.9,
        "lineage_bias": 0.3,
        "analysis_depth": 0.4,
        "metadata_tolerance": 0.5,
        "risk_tolerance": 0.5,
    },
    "serpent": {
        "compactness_bias": 0.7,
        "structure_bias": 0.5,
        "byte_fold_bias": 0.5,
        "lineage_bias": 0.9,
        "analysis_depth": 0.6,
        "metadata_tolerance": 0.6,
        "risk_tolerance": 0.6,
    },
}

# Bounds for adaptation deltas (max shift per rule)
ADAPT_DELTA_MAX = 0.15


def _clamp(v: float, lo: float = 0.0, hi: float = 1.0) -> float:
    return max(lo, min(hi, v))


def compute_signals(sheet: Any, source_path: Path | None) -> dict[str, Any]:
    """Compute sensed archive signals from sheet. Deterministic."""
    file_nodes = getattr(sheet, "file_nodes", {}) or {}
    metrics = getattr(sheet, "metrics", {}) or {}
    raw_size = metrics.get("original_size_bytes", 0)
    file_count = metrics.get("file_count", len(file_nodes))
    total_size = sum(len((getattr(n, "raw_text", "") or "").encode("utf-8")) for n in file_nodes.values())
    if not total_size and file_count:
        total_size = raw_size
    avg_file_size = total_size / file_count if file_count else 0
    confidences = [getattr(n, "parser_confidence", 0.0) or 0.0 for n in file_nodes.values()]
    parser_confidence_mean = sum(confidences) / len(confidences) if confidences else 0.5
    structured = sum(1 for c in confidences if c >= 0.7)
    opaque = sum(1 for c in confidences if c < 0.4)
    structured_ratio = structured / file_count if file_count else 0
    opaque_ratio = opaque / file_count if file_count else 0

    lineage_context = False
    if source_path:
        sync_dir = Path(source_path) / ".infold-sync"
        lineage_context = sync_dir.exists() and (sync_dir / "lineage.json").exists()

    # Density estimates from file count / size (pre-operator; conservative)
    duplicate_density = min(1.0, file_count / 50) * 0.3 if file_count else 0
    template_density = structured_ratio * 0.5
    hierarchy_density = min(1.0, metrics.get("folder_count", 0) / 20) * 0.4 if file_count else 0
    dependency_density = structured_ratio * 0.4
    chunk_reuse_potential = opaque_ratio * 0.7
    metadata_overhead_pressure = min(1.0, file_count / 100) * 0.5 if file_count else 0

    return {
        "raw_size": raw_size or total_size,
        "file_count": file_count,
        "avg_file_size": avg_file_size,
        "structured_ratio": round(structured_ratio, 4),
        "opaque_ratio": round(opaque_ratio, 4),
        "parser_confidence_mean": round(parser_confidence_mean, 4),
        "duplicate_density": round(duplicate_density, 4),
        "template_density": round(template_density, 4),
        "hierarchy_density": round(hierarchy_density, 4),
        "dependency_density": round(dependency_density, 4),
        "chunk_reuse_potential": round(chunk_reuse_potential, 4),
        "metadata_overhead_pressure": round(metadata_overhead_pressure, 4),
        "lineage_context": lineage_context,
    }


def adapt_traits(
    species: str,
    traits_initial: dict[str, float],
    signals: dict[str, Any],
) -> tuple[dict[str, float], list[str]]:
    """
    Apply deterministic bounded adaptation. Returns (traits_final, reasons).
    No random behavior; no persistent state.
    """
    traits = dict(traits_initial)
    reasons: list[str] = []
    opaque = signals.get("opaque_ratio", 0)
    struct = signals.get("structured_ratio", 0)
    template_d = signals.get("template_density", 0)
    hierarchy_d = signals.get("hierarchy_density", 0)
    meta_pressure = signals.get("metadata_overhead_pressure", 0)
    lineage = signals.get("lineage_context", False)
    parser_conf = signals.get("parser_confidence_mean", 0.5)
    chunk_pot = signals.get("chunk_reuse_potential", 0)

    if opaque >= 0.3:
        delta = min(ADAPT_DELTA_MAX, opaque * 0.2)
        traits["byte_fold_bias"] = _clamp(traits["byte_fold_bias"] + delta)
        reasons.append("high opaque_ratio -> +byte_fold_bias")
    if template_d >= 0.2 or hierarchy_d >= 0.2:
        delta = min(ADAPT_DELTA_MAX, (template_d + hierarchy_d) * 0.15)
        traits["structure_bias"] = _clamp(traits["structure_bias"] + delta)
        reasons.append("high template/hierarchy density -> +structure_bias")
    if meta_pressure >= 0.3:
        delta = min(ADAPT_DELTA_MAX, meta_pressure * 0.2)
        traits["compactness_bias"] = _clamp(traits["compactness_bias"] + delta)
        reasons.append("metadata overhead pressure -> +compactness_bias")
    if lineage:
        delta = ADAPT_DELTA_MAX * 0.5
        traits["lineage_bias"] = _clamp(traits["lineage_bias"] + delta)
        reasons.append("lineage context -> +lineage_bias")
    if parser_conf < 0.5:
        delta = min(ADAPT_DELTA_MAX, (0.5 - parser_conf) * 0.3)
        traits["structure_bias"] = _clamp(traits["structure_bias"] - delta)
        reasons.append("low parser confidence -> -structure_bias")
    if chunk_pot >= 0.4:
        delta = min(ADAPT_DELTA_MAX, chunk_pot * 0.15)
        traits["byte_fold_bias"] = _clamp(traits["byte_fold_bias"] + delta)
        reasons.append("chunk reuse potential -> +byte_fold_bias")

    return traits, reasons


def traits_to_behavior(traits: dict[str, float]) -> dict[str, Any]:
    """Map final traits to conservative config overrides. Narrow and safe."""
    out: dict[str, Any] = {}
    compact = traits.get("compactness_bias", 0.5)
    risk = traits.get("risk_tolerance", 0.5)
    meta_tol = traits.get("metadata_tolerance", 0.5)

    if compact >= 0.65:
        out.setdefault("package_export", {})["compact"] = True
        out["package_export"]["report_text"] = False
        out["package_export"]["inventory_minimal"] = True
    if compact < 0.4:
        out.setdefault("package_export", {})["compact"] = False
        out["package_export"]["report_text"] = True

    min_net = -1.0 + (risk * 1.5)
    min_net = _clamp(min_net, -1.0, 0.5)
    out.setdefault("planner", {})["min_net_value"] = round(min_net, 2)
    out["planner"]["metadata_penalty_weight"] = round(-0.002 + (meta_tol * 0.0015), 4)

    mtf_min = 16 if meta_tol >= 0.5 else 24
    out.setdefault("thresholds", {})
    out["thresholds"].setdefault("metadata_table_fold", {})["min_net_gain_bytes"] = mtf_min

    return out


def run_creature(
    species: str,
    sheet: Any,
    source_path: Path | None,
    mode: str,
    reason: str,
) -> dict[str, Any]:
    """
    Run creature adaptation: compute signals, adapt traits, produce behavior.
    Returns creature info dict for manifest/report.
    """
    base = SPECIES_BASE_TRAITS.get(species, SPECIES_BASE_TRAITS["fox"])
    traits_initial = dict(base)
    signals = compute_signals(sheet, source_path)
    traits_final, adapt_reasons = adapt_traits(species, traits_initial, signals)
    behavior = traits_to_behavior(traits_final)

    changes: list[str] = []
    for k in TRAIT_NAMES:
        i = traits_initial.get(k, 0.5)
        f = traits_final.get(k, 0.5)
        if abs(f - i) >= 0.01:
            changes.append(f"{k}: {i:.2f} -> {f:.2f}")

    return {
        "fold_species": species,
        "fold_species_mode": mode,
        "fold_species_reason": reason,
        "fold_creature_signals": {k: round(v, 4) if isinstance(v, (int, float)) else v for k, v in signals.items()},
        "fold_creature_traits_initial": {k: round(v, 4) for k, v in traits_initial.items()},
        "fold_creature_traits_final": {k: round(v, 4) for k, v in traits_final.items()},
        "fold_creature_adapt_reasons": adapt_reasons,
        "fold_creature_behavior_changes": changes,
        "fold_creature_behavior_overrides": behavior,
    }
