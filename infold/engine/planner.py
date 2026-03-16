"""
Fold Planner v1: scoring and planning layer for fold candidates.

Scores using logical gain, physical gain, utility, stress, metadata penalty.
Skips candidates when net value is poor.
Deterministic behavior.
"""

from dataclasses import dataclass
from typing import Any

from infold.models.candidate import CandidateCrease
from infold.models.estimates import GainEstimate, StressEstimate


@dataclass
class CandidateScore:
    """Scored candidate with net value."""

    candidate: CandidateCrease
    logical_gain: int
    physical_gain: int
    utility: float
    stress: float
    metadata_penalty: int
    net_value: float
    skip: bool
    skip_reason: str | None


def _metadata_penalty(candidate: CandidateCrease) -> int:
    """Estimate metadata overhead in bytes."""
    n_targets = len(candidate.targets)
    return 50 + n_targets * 20


def score_candidate(
    candidate: CandidateCrease,
    config: dict[str, Any],
) -> CandidateScore:
    """
    Score a fold candidate. Deterministic.
    net_value = logical_gain + 0.1*physical_gain + utility - stress - 0.001*metadata_penalty
    Skip when net_value < min_net_value threshold.
    """
    gain = candidate.gain
    stress = candidate.stress
    logical_gain = gain.net_bytes_saved
    physical_gain = logical_gain  # same for content folds; metadata folds have 0 physical
    utility = gain.utility_score
    stress_val = stress.overall_stress
    meta_penalty = _metadata_penalty(candidate)

    planner_config = config.get("planner", {})
    min_net_value = planner_config.get("min_net_value", 0.0)
    logical_weight = planner_config.get("logical_weight", 0.001)
    utility_weight = planner_config.get("utility_weight", 1.0)
    stress_weight = planner_config.get("stress_weight", -2.0)
    meta_weight = planner_config.get("metadata_penalty_weight", -0.001)

    net_value = (
        logical_weight * logical_gain
        + utility_weight * utility
        + stress_weight * stress_val
        + meta_weight * meta_penalty
    )

    skip = net_value < min_net_value
    skip_reason = f"net_value {net_value:.4f} < {min_net_value}" if skip else None

    return CandidateScore(
        candidate=candidate,
        logical_gain=logical_gain,
        physical_gain=physical_gain,
        utility=utility,
        stress=stress_val,
        metadata_penalty=meta_penalty,
        net_value=net_value,
        skip=skip,
        skip_reason=skip_reason,
    )


def filter_candidates(
    candidates: list[CandidateCrease],
    config: dict[str, Any],
) -> tuple[list[CandidateCrease], list[tuple[CandidateCrease, str]]]:
    """
    Filter candidates by score. Returns (accepted, rejected_with_reason).
    """
    accepted: list[CandidateCrease] = []
    rejected: list[tuple[CandidateCrease, str]] = []
    for c in candidates:
        score = score_candidate(c, config)
        if score.skip:
            rejected.append((c, score.skip_reason or "low net value"))
        else:
            accepted.append(c)
    return accepted, rejected
