"""
Phase 10C: Tesseract Execution Plan and Cooperation v0.1.

Conservative orchestration: short, safe execution plans for
structural→metadata and byte→metadata cooperation.
"""

from __future__ import annotations

from typing import Any

COOPERATION_MODES = ("structural_then_metadata", "byte_then_metadata", "primary_only", "balanced")


def generate_execution_plan(profile: dict[str, Any]) -> dict[str, Any]:
    """
    Generate safe execution plan from Tesseract profile.
    Deterministic. No long chains, no recursion.
    """
    dominant = profile.get("dominant_dimension", "structure")
    secondary = profile.get("secondary_dimension", "structure")
    route = profile.get("route", "balanced")
    strengths = {
        "structure_strength": profile.get("structure_strength", 0),
        "byte_strength": profile.get("byte_strength", 0),
        "metadata_strength": profile.get("metadata_strength", 0),
        "time_strength": profile.get("time_strength", 0),
    }
    s = strengths["structure_strength"]
    b = strengths["byte_strength"]
    m = strengths["metadata_strength"]
    t = strengths["time_strength"]

    execution_steps: list[str] = []
    cooperation_mode = "primary_only"
    plan_reason = ""

    if t >= 0.6:
        execution_steps = ["primary"]
        cooperation_mode = "primary_only"
        plan_reason = "time_dominant_conservative"
    elif dominant == "structure" and secondary == "metadata" and m >= 0.25:
        execution_steps = ["structural", "metadata"]
        cooperation_mode = "structural_then_metadata"
        plan_reason = "structure_metadata_cooperation"
    elif dominant == "byte" and secondary == "metadata" and m >= 0.25:
        execution_steps = ["byte", "metadata"]
        cooperation_mode = "byte_then_metadata"
        plan_reason = "byte_metadata_cooperation"
    elif dominant == "structure" and secondary == "byte":
        execution_steps = ["structural"]
        cooperation_mode = "primary_only"
        plan_reason = "structure_byte_no_chain_v01"
    elif dominant == "byte" and secondary == "structure":
        execution_steps = ["byte"]
        cooperation_mode = "primary_only"
        plan_reason = "byte_structure_no_chain_v01"
    elif dominant == "metadata" and m >= 0.5:
        execution_steps = ["metadata"]
        cooperation_mode = "primary_only"
        plan_reason = "metadata_primary"
    else:
        execution_steps = ["primary"]
        cooperation_mode = "balanced"
        plan_reason = "mixed_signals_single_step"

    return {
        "dominant_dimension": dominant,
        "secondary_dimension": secondary,
        "route": route,
        "operator_family_priority": profile.get("operator_family_priority", []),
        "execution_steps": execution_steps,
        "cooperation_mode": cooperation_mode,
        "plan_reason": plan_reason,
    }


def execution_plan_allows_metadata_followup(plan: dict[str, Any]) -> bool:
    """True if plan allows metadata cleanup step (structural_then_metadata or byte_then_metadata)."""
    return plan.get("cooperation_mode") in ("structural_then_metadata", "byte_then_metadata")


def execution_plan_summary(plan: dict[str, Any]) -> str:
    """Concise human-readable summary."""
    steps = "->".join(plan.get("execution_steps", []))
    return f"steps={steps} mode={plan.get('cooperation_mode')} reason={plan.get('plan_reason')}"
