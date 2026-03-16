"""Potential fold before commitment."""

from dataclasses import dataclass
from typing import Any

from infold.models.estimates import GainEstimate, StressEstimate


@dataclass
class CandidateCrease:
    """A potential fold before commitment: targets, estimated gain, stress, invariants, metadata."""

    operator_id: str
    targets: list[Any]  # file paths, regions, graph nodes, etc.
    gain: GainEstimate
    stress: StressEstimate
    invariants: list[str]  # declared invariants
    metadata: dict[str, Any]  # operator-specific data
