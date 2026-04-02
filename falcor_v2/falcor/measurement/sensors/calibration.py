"""One-sensor-at-a-time calibration workflow."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Callable

import numpy as np


@dataclass
class CalibrationResult:
    """Calibration profile from baseline window."""

    offset: float
    scale: float
    noise_sigma: float
    lag_s: float


def calibrate_sensor(
    samples: np.ndarray,
    truth_values: np.ndarray | None = None,
) -> CalibrationResult:
    """
    Estimate offset, scale, noise_sigma from baseline window.
    If truth_values provided: scale = std(samples)/std(truth), offset = mean(samples) - scale*mean(truth)
    Else: offset = mean(samples), scale = 1, noise_sigma = std(samples)
    """
    if len(samples) == 0:
        return CalibrationResult(0.0, 1.0, 0.0, 0.0)
    mean_s = np.mean(samples)
    std_s = np.std(samples)
    if truth_values is not None and len(truth_values) == len(samples):
        mean_t = np.mean(truth_values)
        std_t = np.std(truth_values)
        scale = std_s / std_t if std_t > 0 else 1.0
        offset = mean_s - scale * mean_t
    else:
        offset = mean_s
        scale = 1.0
    return CalibrationResult(offset=offset, scale=scale, noise_sigma=float(std_s), lag_s=0.0)
