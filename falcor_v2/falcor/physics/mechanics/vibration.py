"""Synthetic vibration model + FFT features."""

from __future__ import annotations

import numpy as np


def synthetic_vibration(
    t: np.ndarray,
    base_rms: float,
    omega: float,
    harmonics: list[float],
    rng: np.random.Generator,
) -> np.ndarray:
    """Generate synthetic vibration signal (m/s^2 or g)."""
    signal = base_rms * rng.standard_normal(len(t))
    for h in harmonics:
        signal += 0.1 * base_rms * np.sin(2 * np.pi * h * omega * t + rng.uniform(0, 2 * np.pi))
    return signal


def rms(signal: np.ndarray) -> float:
    """RMS of signal."""
    return float(np.sqrt(np.mean(signal**2)))


def max_abs(signal: np.ndarray) -> float:
    """Max absolute value."""
    return float(np.max(np.abs(signal)))
