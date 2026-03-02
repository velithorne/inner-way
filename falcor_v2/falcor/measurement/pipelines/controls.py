"""Control group analysis: effect sizes, p-values, falsification score."""

from __future__ import annotations

from dataclasses import dataclass
from enum import Enum

import numpy as np
from scipy import stats

from falcor.measurement.artifacts.detectors import artifact_likelihood


class EvidenceGrade(str, Enum):
    A = "A"  # Survives controls, low artifact, stable audit
    B = "B"  # Signal exists, moderate artifact likelihood
    C = "C"  # Likely artifact / non-reproducible


def compute_effect_size(a: np.ndarray, b: np.ndarray) -> float:
    """Cohen's d."""
    if len(a) < 2 or len(b) < 2:
        return 0.0
    pooled_std = np.sqrt((np.var(a) + np.var(b)) / 2)
    if pooled_std < 1e-12:
        return 0.0
    return float((np.mean(a) - np.mean(b)) / pooled_std)


def permutation_test(a: np.ndarray, b: np.ndarray, n_perm: int = 1000) -> float:
    """Permutation test p-value."""
    observed = np.mean(a) - np.mean(b)
    combined = np.concatenate([a, b])
    n_a = len(a)
    count = 0
    rng = np.random.default_rng(42)
    for _ in range(n_perm):
        perm = rng.permutation(combined)
        diff = np.mean(perm[:n_a]) - np.mean(perm[n_a:])
        if abs(diff) >= abs(observed):
            count += 1
    return count / n_perm


def falsification_score(
    effect_size: float,
    p_value: float,
    artifact_lik: float,
    energy_audit_stable: bool,
) -> tuple[float, EvidenceGrade]:
    """
    Compute falsification score [0,1] and evidence grade.
    Higher score = more robust to falsification.
    """
    score = 0.0
    if p_value < 0.05 and abs(effect_size) > 0.2:
        score += 0.3
    if artifact_lik < 0.3:
        score += 0.3
    elif artifact_lik < 0.6:
        score += 0.15
    if energy_audit_stable:
        score += 0.4

    if score >= 0.7 and artifact_lik < 0.3 and energy_audit_stable:
        grade = EvidenceGrade.A
    elif score >= 0.4 and artifact_lik < 0.6:
        grade = EvidenceGrade.B
    else:
        grade = EvidenceGrade.C

    return min(1.0, score), grade
