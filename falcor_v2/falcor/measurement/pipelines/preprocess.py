"""Preprocessing pipeline."""

from __future__ import annotations

import numpy as np


def preprocess_timeseries(
    data: np.ndarray,
    remove_outliers: bool = True,
    outlier_std: float = 3.0,
) -> np.ndarray:
    """Basic preprocessing: optional outlier clipping."""
    out = data.copy()
    if remove_outliers and len(out) > 2:
        mean = np.mean(out)
        std = np.std(out)
        if std > 1e-12:
            out = np.clip(out, mean - outlier_std * std, mean + outlier_std * std)
    return out
