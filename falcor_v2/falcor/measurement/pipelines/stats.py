"""Statistical helpers."""

from __future__ import annotations

import numpy as np
from scipy import stats


def t_test_independent(a: np.ndarray, b: np.ndarray) -> tuple[float, float]:
    """Welch t-test. Returns (statistic, p-value)."""
    result = stats.ttest_ind(a, b, equal_var=False)
    return float(result.statistic), float(result.pvalue)


def mean_with_uncertainty(x: np.ndarray) -> tuple[float, float]:
    """Mean and standard error of mean."""
    if len(x) == 0:
        return 0.0, 0.0
    return float(np.mean(x)), float(np.std(x) / np.sqrt(len(x)))
