"""
Fold Planner v2: expanded scoring and planning layer.

Scoring components: logical_gain, physical_gain, utility, reconstruction_confidence,
operator_priority_bonus, downstream_synergy_bonus, metadata_penalty, stress_penalty,
conflict_penalty, redundancy_penalty.

Per-candidate fields: final_net_value, planner_decision, planner_reason.
Decisions: accept, reject_low_value, reject_high_stress, reject_conflict,
reject_redundant, defer, superseded_by_higher_priority_candidate.

Deterministic and explainable.
"""

from dataclasses import dataclass, field
from typing import Any

from infold.engine.interaction_policy import conflicts_with_committed
from infold.models.candidate import CandidateCrease
from infold.models.estimates import GainEstimate, StressEstimate

# Planner decision types
ACCEPT = "accept"
REJECT_LOW_VALUE = "reject_low_value"
REJECT_HIGH_STRESS = "reject_high_stress"
REJECT_CONFLICT = "reject_conflict"
REJECT_REDUNDANT = "reject_redundant"
DEFER = "defer"
SUPERSEDED_BY_HIGHER_PRIORITY = "superseded_by_higher_priority_candidate"

# Operator priority (higher = preferred when competing). Used for bonus.
OPERATOR_PRIORITY: dict[str, int] = {
    "exact_repetition": 5,
    "symbol_table": 4,
    "template_skeleton": 3,
    "hierarchy_mirror": 2,
    "dependency_motif": 1,
}


@dataclass
class PlannerDecisionRecord:
    """Per-candidate planner outcome with full explainability."""

    candidate: CandidateCrease
    final_net_value: float
    planner_decision: str
    planner_reason: str
    logical_gain: int
    physical_gain: int
    utility: float
    reconstruction_confidence: float
    operator_priority_bonus: float
    downstream_synergy_bonus: float
    metadata_penalty: float
    stress_penalty: float
    conflict_penalty: float
    redundancy_penalty: float

    @property
    def skip(self) -> bool:
        """Backward compat: True if not accepted."""
        return self.planner_decision != ACCEPT

    @property
    def stress(self) -> float:
        """Backward compat: stress from candidate."""
        return self.candidate.stress.overall_stress


def _metadata_penalty_bytes(candidate: CandidateCrease) -> int:
    """Estimate metadata overhead in bytes."""
    n_targets = len(candidate.targets)
    return 50 + n_targets * 20


def score_candidate_v2(
    candidate: CandidateCrease,
    config: dict[str, Any],
    committed_paths: set[str] | None = None,
    has_conflict: bool = False,
    has_redundancy: bool = False,
) -> PlannerDecisionRecord:
    """
    Score a fold candidate with full v2 components. Deterministic.
    """
    gain = candidate.gain
    stress = candidate.stress
    logical_gain = gain.net_bytes_saved
    physical_gain = logical_gain  # metadata folds: 0 physical; content folds: same as logical
    utility = gain.utility_score
    reconstruction_confidence = gain.confidence
    stress_val = stress.overall_stress

    meta_bytes = _metadata_penalty_bytes(candidate)
    operator_priority_bonus = OPERATOR_PRIORITY.get(candidate.operator_id, 0) * 0.02
    downstream_synergy_bonus = 0.0  # v2: no downstream reuse yet
    metadata_penalty = -0.001 * meta_bytes
    stress_penalty = -2.0 * stress_val
    conflict_penalty = -10.0 if has_conflict else 0.0
    redundancy_penalty = -5.0 if has_redundancy else 0.0

    planner_config = config.get("planner", {})
    logical_weight = planner_config.get("logical_weight", 0.001)
    physical_weight = planner_config.get("physical_weight", 0.0005)
    utility_weight = planner_config.get("utility_weight", 1.0)
    confidence_weight = planner_config.get("reconstruction_confidence_weight", 0.5)
    min_net_value = planner_config.get("min_net_value", -0.5)
    max_stress = planner_config.get("max_stress", 1.0)

    final_net_value = (
        logical_weight * logical_gain
        + physical_weight * physical_gain
        + utility_weight * utility
        + confidence_weight * reconstruction_confidence
        + operator_priority_bonus
        + downstream_synergy_bonus
        + metadata_penalty
        + stress_penalty
        + conflict_penalty
        + redundancy_penalty
    )

    if has_conflict:
        planner_decision = REJECT_CONFLICT
        planner_reason = "conflict with committed paths"
    elif has_redundancy:
        planner_decision = REJECT_REDUNDANT
        planner_reason = "redundant with existing fold"
    elif stress_val > max_stress:
        planner_decision = REJECT_HIGH_STRESS
        planner_reason = f"stress {stress_val:.3f} > max {max_stress}"
    elif final_net_value < min_net_value:
        planner_decision = REJECT_LOW_VALUE
        planner_reason = f"net_value {final_net_value:.4f} < min {min_net_value}"
    else:
        planner_decision = ACCEPT
        planner_reason = "accepted"

    return PlannerDecisionRecord(
        candidate=candidate,
        final_net_value=final_net_value,
        planner_decision=planner_decision,
        planner_reason=planner_reason,
        logical_gain=logical_gain,
        physical_gain=physical_gain,
        utility=utility,
        reconstruction_confidence=reconstruction_confidence,
        operator_priority_bonus=operator_priority_bonus,
        downstream_synergy_bonus=downstream_synergy_bonus,
        metadata_penalty=metadata_penalty,
        stress_penalty=stress_penalty,
        conflict_penalty=conflict_penalty,
        redundancy_penalty=redundancy_penalty,
    )


def filter_candidates_v2(
    candidates: list[CandidateCrease],
    config: dict[str, Any],
    committed_paths: set[str] | None = None,
) -> tuple[list[CandidateCrease], list[PlannerDecisionRecord]]:
    """
    Filter candidates with full planner v2. Returns (accepted, all_decisions).
    all_decisions includes both accepted and rejected with planner_decision/reason.
    """
    committed = committed_paths or set()
    accepted: list[CandidateCrease] = []
    all_decisions: list[PlannerDecisionRecord] = []

    for c in candidates:
        has_conflict, _ = conflicts_with_committed(c, committed)
        record = score_candidate_v2(c, config, committed_paths=committed, has_conflict=has_conflict)
        all_decisions.append(record)
        if record.planner_decision == ACCEPT:
            accepted.append(c)
    return accepted, all_decisions


def score_candidate(candidate: CandidateCrease, config: dict[str, Any]) -> "PlannerDecisionRecord":
    """Score candidate (v2, returns PlannerDecisionRecord). Backward compat alias."""
    return score_candidate_v2(candidate, config)


# Backward compatibility: v1-style filter_candidates
def filter_candidates(
    candidates: list[CandidateCrease],
    config: dict[str, Any],
) -> tuple[list[CandidateCrease], list[tuple[CandidateCrease, str]]]:
    """Filter candidates (v1 compat). Returns (accepted, rejected_with_reason)."""
    accepted, decisions = filter_candidates_v2(candidates, config)
    rejected: list[tuple[CandidateCrease, str]] = []
    for d in decisions:
        if d.planner_decision != ACCEPT:
            rejected.append((d.candidate, d.planner_reason))
    return accepted, rejected
