"""Artifact detectors: identify segments, compute likelihood."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

import numpy as np


@dataclass
class ArtifactSegment:
    """Detected artifact segment."""

    start_idx: int
    end_idx: int
    artifact_type: str
    likelihood: float


def detect_artifacts(
    timeseries: np.ndarray,
    threshold_std: float = 3.0,
) -> list[ArtifactSegment]:
    """Simple detector: segments where value exceeds mean + threshold_std * std."""
    if len(timeseries) < 3:
        return []
    mean = np.mean(timeseries)
    std = np.std(timeseries)
    if std < 1e-12:
        return []
    segments: list[ArtifactSegment] = []
    in_segment = False
    start = 0
    for i, v in enumerate(timeseries):
        z = abs(v - mean) / std if std > 0 else 0
        if z > threshold_std:
            if not in_segment:
                start = i
                in_segment = True
        else:
            if in_segment:
                segments.append(
                    ArtifactSegment(
                        start_idx=start,
                        end_idx=i,
                        artifact_type="unknown",
                        likelihood=min(1.0, threshold_std / 2),
                    )
                )
                in_segment = False
    if in_segment:
        segments.append(
            ArtifactSegment(
                start_idx=start,
                end_idx=len(timeseries),
                artifact_type="unknown",
                likelihood=min(1.0, threshold_std / 2),
            )
        )
    return segments


def artifact_likelihood(
    timeseries: np.ndarray,
    segments: list[ArtifactSegment] | None = None,
) -> float:
    """Compute overall artifact likelihood [0,1]. Higher = more likely artifact contamination."""
    if segments is None:
        segments = detect_artifacts(timeseries)
    if not segments or len(timeseries) == 0:
        return 0.0
    total_contaminated = sum(s.end_idx - s.start_idx for s in segments)
    frac = total_contaminated / len(timeseries)
    avg_likelihood = np.mean([s.likelihood for s in segments]) if segments else 0
    return min(1.0, frac * 2 + avg_likelihood * 0.5)
