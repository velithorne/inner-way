"""Time base and sampling utilities."""

from __future__ import annotations

import numpy as np


def uniform_time_grid(t_start: float, t_end: float, sample_rate_hz: float) -> np.ndarray:
    """Generate uniform time grid."""
    n = int((t_end - t_start) * sample_rate_hz) + 1
    return np.linspace(t_start, t_end, n)


def dt_from_rate(sample_rate_hz: float) -> float:
    """Get timestep from sample rate."""
    return 1.0 / sample_rate_hz
