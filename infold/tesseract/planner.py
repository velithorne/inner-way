"""
Phase 10B: Tesseract Planner v0.1 — meta-planning layer.

Uses multi-dimensional family identity to guide routing, operator priority,
and planner biases. Deterministic, additive, conservative.
"""

from __future__ import annotations

from typing import Any

TESSERACT_ROUTES = (
    "structural_first",
    "byte_first",
    "metadata_sensitive",
    "lineage_sensitive",
    "balanced",
)

OPERATOR_FAMILIES = ("structural", "byte", "metadata")

PLANNER_BIAS_KEYS = ("favor_physical_size", "favor_logical_gain", "favor_compactness", "favor_lineage_continuity")

# Operator -> family for priority
OPERATOR_TO_FAMILY: dict[str, str] = {
    "exact_repetition": "structural",
    "template_skeleton": "structural",
    "hierarchy_mirror": "structural",
    "dependency_motif": "structural",
    "symbol_table": "structural",
    "byte_fold": "byte",
}


def _clamp(v: float, lo: float = 0.0, hi: float = 1.0) -> float:
    return max(lo, min(hi, v))


def compute_dimension_strengths(signals: dict[str, Any]) -> dict[str, float]:
    """
    Compute bounded strengths (0-1) for structure, byte, metadata, time.
    Uses creature-style signals. Deterministic.
    """
    struct_ratio = signals.get("structured_ratio", 0)
    parser_conf = signals.get("parser_confidence_mean", 0.5)
    dup_d = signals.get("duplicate_density", 0)
    template_d = signals.get("template_density", 0)
    hierarchy_d = signals.get("hierarchy_density", 0)
    dep_d = signals.get("dependency_density", 0)

    structure_strength = _clamp(
        0.3 * struct_ratio
        + 0.2 * parser_conf
        + 0.15 * (dup_d + template_d)
        + 0.1 * (hierarchy_d + dep_d),
    )

    opaque = signals.get("opaque_ratio", 0)
    chunk_pot = signals.get("chunk_reuse_potential", 0)
    byte_strength = _clamp(0.5 * opaque + 0.4 * chunk_pot)

    meta_pressure = signals.get("metadata_overhead_pressure", 0)
    file_count = signals.get("file_count", 0)
    metadata_strength = _clamp(0.4 * meta_pressure + 0.2 * min(1.0, file_count / 80))

    lineage = 1.0 if signals.get("lineage_context", False) else 0.0
    time_strength = _clamp(0.8 * lineage)

    return {
        "structure_strength": round(structure_strength, 4),
        "byte_strength": round(byte_strength, 4),
        "metadata_strength": round(metadata_strength, 4),
        "time_strength": round(time_strength, 4),
    }


def _dominant_and_secondary(strengths: dict[str, float]) -> tuple[str, str]:
    """Pick dominant and secondary dimension. Deterministic tie-break: structure > byte > metadata > time."""
    order = ("structure", "byte", "metadata", "time")
    sorted_dims = sorted(order, key=lambda d: -strengths.get(f"{d}_strength", 0))
    dominant = sorted_dims[0] if sorted_dims else "structure"
    secondary = sorted_dims[1] if len(sorted_dims) > 1 else "structure"
    return dominant, secondary


def _choose_route(strengths: dict[str, float], dominant: str) -> tuple[str, str]:
    """Choose route from strengths. Returns (route, reason)."""
    s = strengths.get("structure_strength", 0)
    b = strengths.get("byte_strength", 0)
    m = strengths.get("metadata_strength", 0)
    t = strengths.get("time_strength", 0)

    if t >= 0.6:
        return ("lineage_sensitive", "time_strength_high")
    if s >= 0.6 and b < 0.4:
        return ("structural_first", "structure_dominant")
    if b >= 0.6 and s < 0.4:
        return ("byte_first", "byte_dominant")
    if m >= 0.5 and (s < 0.5 or b < 0.5):
        return ("metadata_sensitive", "metadata_pressure")
    return ("balanced", "mixed_signals")


def _operator_family_priority(dominant: str) -> list[str]:
    """Return operator family priority order. structural, byte, metadata."""
    if dominant == "structure":
        return ["structural", "metadata", "byte"]
    if dominant == "byte":
        return ["byte", "metadata", "structural"]
    if dominant == "metadata":
        return ["metadata", "byte", "structural"]
    if dominant == "time":
        return ["structural", "metadata", "byte"]
    return ["structural", "metadata", "byte"]


def _planner_bias(strengths: dict[str, float], route: str) -> dict[str, float]:
    """Small conservative planner bias values. Modest guidance only."""
    bias: dict[str, float] = {
        "favor_physical_size": 0.0,
        "favor_logical_gain": 0.0,
        "favor_compactness": 0.0,
        "favor_lineage_continuity": 0.0,
    }
    m = strengths.get("metadata_strength", 0)
    t = strengths.get("time_strength", 0)
    b = strengths.get("byte_strength", 0)

    if route == "metadata_sensitive" and m >= 0.4:
        bias["favor_compactness"] = 0.05
    if route == "lineage_sensitive" and t >= 0.5:
        bias["favor_lineage_continuity"] = 0.03
    if route == "byte_first" and b >= 0.5:
        bias["favor_physical_size"] = 0.02
    if route == "structural_first":
        bias["favor_logical_gain"] = 0.02
    return {k: round(v, 4) for k, v in bias.items()}


def compute_tesseract_planner_profile(signals: dict[str, Any]) -> dict[str, Any]:
    """
    Compute full Tesseract planning profile for the session.
    Deterministic. Used to guide routing, operator priority, planner bias.
    """
    strengths = compute_dimension_strengths(signals)
    dominant, secondary = _dominant_and_secondary(strengths)
    route, route_reason = _choose_route(strengths, dominant)
    operator_priority = _operator_family_priority(dominant)
    planner_bias = _planner_bias(strengths, route)

    return {
        "structure_strength": strengths["structure_strength"],
        "byte_strength": strengths["byte_strength"],
        "metadata_strength": strengths["metadata_strength"],
        "time_strength": strengths["time_strength"],
        "dominant_dimension": dominant,
        "secondary_dimension": secondary,
        "route": route,
        "route_reason": route_reason,
        "operator_family_priority": operator_priority,
        "planner_bias": planner_bias,
    }


def tesseract_planner_summary(profile: dict[str, Any]) -> str:
    """Concise human-readable summary."""
    parts = [
        f"route={profile.get('route', '?')}",
        f"dominant={profile.get('dominant_dimension', '?')}",
        f"priority={'>'.join(profile.get('operator_family_priority', []))}",
    ]
    bias = profile.get("planner_bias", {})
    active = [k for k, v in bias.items() if v and v > 0]
    if active:
        parts.append(f"bias={','.join(active)}")
    return "; ".join(parts)
