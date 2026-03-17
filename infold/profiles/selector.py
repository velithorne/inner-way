"""
Auto profile selection: deterministic scoring based on project metrics.

Phase 6C: Optimized for physical folded size. Benchmark evidence (Phase 6B)
shows golem wins physical for all categories; serpent preferred for lineage.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

from infold.profiles.definitions import VALID_PROFILES

# Tie-break order when scores equal (deterministic)
_PROFILE_TIE_ORDER = ("golem", "serpent", "dragon", "fox", "sparrow")


def _compute_metrics(sheet: Any) -> dict[str, Any]:
    """Compute project metrics from project sheet. Deterministic."""
    file_nodes = getattr(sheet, "file_nodes", {}) or {}
    metrics = getattr(sheet, "metrics", {}) or {}
    raw_size = metrics.get("original_size_bytes", 0)
    file_count = metrics.get("file_count", len(file_nodes))
    folder_count = metrics.get("folder_count", 0)

    total_size = 0
    confidences: list[float] = []
    structured_count = 0
    low_structure_count = 0
    for path, node in file_nodes.items():
        raw = getattr(node, "raw_text", "") or ""
        total_size += len(raw.encode("utf-8"))
        conf = getattr(node, "parser_confidence", 0.0) or 0.0
        confidences.append(conf)
        if conf >= 0.7:
            structured_count += 1
        elif conf < 0.4:
            low_structure_count += 1
    if not total_size and file_count:
        total_size = raw_size
    avg_file_size = total_size / file_count if file_count else 0
    avg_confidence = sum(confidences) / len(confidences) if confidences else 0.5
    structured_ratio = structured_count / file_count if file_count else 0
    opaque_ratio = low_structure_count / file_count if file_count else 0

    return {
        "raw_size": raw_size or total_size,
        "file_count": file_count,
        "folder_count": folder_count,
        "avg_file_size": avg_file_size,
        "avg_confidence": avg_confidence,
        "structured_ratio": structured_ratio,
        "opaque_ratio": opaque_ratio,
    }


def _has_lineage_context(source_path: Path) -> bool:
    """Check if lineage/sync context exists (e.g. .infold-sync)."""
    sync_dir = source_path / ".infold-sync"
    return sync_dir.exists() and (sync_dir / "lineage.json").exists()


def _compute_profile_scores(
    metrics: dict[str, Any],
    lineage: bool,
) -> tuple[dict[str, float], dict[str, dict[str, Any]]]:
    """
    Compute per-profile scores for physical folded size optimization.
    Returns (profile -> score, profile -> score_breakdown).
    Deterministic.
    """
    raw_size = metrics["raw_size"]
    file_count = metrics["file_count"]
    structured_ratio = metrics["structured_ratio"]
    opaque_ratio = metrics["opaque_ratio"]

    scores: dict[str, float] = {}
    breakdown: dict[str, dict[str, Any]] = {}

    # Golem: benchmark evidence wins physical for all categories (Phase 6B)
    golem_base = 100.0
    golem_opaque_bonus = min(10.0, opaque_ratio * 20)  # chunk bias helps opaque
    scores["golem"] = golem_base + golem_opaque_bonus
    breakdown["golem"] = {
        "base": golem_base,
        "opaque_bonus": round(golem_opaque_bonus, 2),
        "reason": "physical_optimized",
    }

    # Serpent: lineage/sync workflow preference (overrides physical when sync context)
    if lineage:
        scores["serpent"] = 105.0  # higher than golem for lineage workflows
        breakdown["serpent"] = {"base": 105.0, "reason": "lineage_context"}
    else:
        scores["serpent"] = 45.0
        breakdown["serpent"] = {"base": 45.0, "reason": "no_lineage"}

    # Dragon: best for logical gain, not primary for physical
    dragon_base = 70.0
    dragon_large_bonus = 5.0 if (raw_size >= 500_000 and structured_ratio >= 0.6) else 0.0
    scores["dragon"] = dragon_base + dragon_large_bonus
    breakdown["dragon"] = {
        "base": dragon_base,
        "large_structured_bonus": dragon_large_bonus,
        "reason": "gain_optimized",
    }

    # Fox: balanced baseline
    scores["fox"] = 60.0
    breakdown["fox"] = {"base": 60.0, "reason": "balanced"}

    # Sparrow: overhead-sensitive, but golem wins for tiny (benchmark)
    scores["sparrow"] = 55.0
    breakdown["sparrow"] = {"base": 55.0, "reason": "overhead_sensitive"}

    return scores, breakdown


def select_profile_auto(
    sheet: Any,
    config: dict[str, Any],
    source_path: Path | None = None,
) -> tuple[str, str, dict[str, Any]]:
    """
    Deterministic auto-selection of fold profile.
    Optimized for physical folded size (Phase 6C).
    Returns (profile_name, reason, factors_dict).
    """
    metrics = _compute_metrics(sheet)
    lineage = False
    if source_path:
        lineage = _has_lineage_context(Path(source_path))

    scores, breakdown = _compute_profile_scores(metrics, lineage)

    # Select profile with highest score; tie-break by fixed order
    best_score = max(scores.values())
    candidates = [p for p, s in scores.items() if s == best_score]
    selected = min(candidates, key=lambda p: _PROFILE_TIE_ORDER.index(p) if p in _PROFILE_TIE_ORDER else 999)

    reason = breakdown.get(selected, {}).get("reason", "scored")
    factors: dict[str, Any] = {
        "raw_size": metrics["raw_size"],
        "file_count": metrics["file_count"],
        "avg_file_size": round(metrics["avg_file_size"], 1),
        "structured_ratio": round(metrics["structured_ratio"], 3),
        "opaque_ratio": round(metrics["opaque_ratio"], 3),
        "lineage_context": lineage,
        "scores": {p: round(s, 2) for p, s in scores.items()},
        "score_breakdown": breakdown.get(selected, {}),
        "selected_score": round(scores[selected], 2),
    }
    return selected, reason, factors
