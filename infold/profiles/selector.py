"""
Auto profile selection: deterministic choice based on project metrics.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

from infold.profiles.definitions import VALID_PROFILES


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


def select_profile_auto(
    sheet: Any,
    config: dict[str, Any],
    source_path: Path | None = None,
) -> tuple[str, str, dict[str, Any]]:
    """
    Deterministic auto-selection of fold profile.
    Returns (profile_name, reason, factors_dict).
    """
    metrics = _compute_metrics(sheet)
    raw_size = metrics["raw_size"]
    file_count = metrics["file_count"]
    avg_file_size = metrics["avg_file_size"]
    structured_ratio = metrics["structured_ratio"]
    opaque_ratio = metrics["opaque_ratio"]

    factors: dict[str, Any] = {
        "raw_size": raw_size,
        "file_count": file_count,
        "avg_file_size": avg_file_size,
        "structured_ratio": round(structured_ratio, 3),
        "opaque_ratio": round(opaque_ratio, 3),
    }

    lineage = False
    if source_path:
        lineage = _has_lineage_context(Path(source_path))
        factors["lineage_context"] = lineage

    # Sparrow: tiny archives, overhead-sensitive
    if raw_size < 2000 or (file_count <= 5 and raw_size < 10000):
        return "sparrow", "tiny_archive", factors

    # Serpent: lineage/snapshot context
    if lineage:
        return "serpent", "lineage_context", factors

    # Golem: binary/opaque-heavy
    if opaque_ratio >= 0.4 or (opaque_ratio >= 0.25 and structured_ratio < 0.5):
        return "golem", "opaque_heavy", factors

    # Dragon: large structure-rich
    if raw_size >= 500_000 and structured_ratio >= 0.6:
        return "dragon", "large_structured", factors

    # Fox: balanced default
    return "fox", "balanced", factors
