"""Gain and stress estimates for fold candidates."""

from dataclasses import dataclass


@dataclass(frozen=True)
class GainEstimate:
    """Estimated benefit of a fold before commitment."""

    gross_bytes_saved: int
    metadata_cost: int
    net_bytes_saved: int
    utility_score: float
    confidence: float


@dataclass(frozen=True)
class StressEstimate:
    """Estimated cost or distortion introduced by a fold."""

    ambiguity: float
    reconstruction_complexity: float
    parser_dependence: float
    overlap_risk: float
    overall_stress: float
