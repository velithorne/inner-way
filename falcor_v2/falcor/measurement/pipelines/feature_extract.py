"""Feature extraction from timeseries."""

from __future__ import annotations

import numpy as np


def extract_slope(t: np.ndarray, y: np.ndarray) -> float:
    """Linear regression slope (dy/dt)."""
    if len(t) < 2 or len(y) < 2:
        return 0.0
    A = np.vstack([t, np.ones(len(t))]).T
    slope, _ = np.linalg.lstsq(A, y, rcond=None)[0]
    return float(slope)


def extract_mean(y: np.ndarray) -> float:
    return float(np.mean(y))


def extract_std(y: np.ndarray) -> float:
    return float(np.std(y)) if len(y) > 1 else 0.0
