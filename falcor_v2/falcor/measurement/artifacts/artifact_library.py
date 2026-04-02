"""Artifact injection library for falsification."""

from __future__ import annotations

from enum import Enum
from typing import Any

import numpy as np

from falcor.core.rng import get_rng


class ArtifactType(str, Enum):
    THERMAL_DRIFT = "thermal_drift"
    SENSOR_LAG = "sensor_lag"
    RPM_RIPPLE = "rpm_ripple"
    EMI_BURST = "emi_burst"
    VIBRATION_SPIKE = "vibration_spike"


def inject_artifact(
    timeseries: np.ndarray,
    artifact_type: ArtifactType | str,
    params: dict[str, Any],
    seed: int | None = None,
) -> np.ndarray:
    """Inject artifact into timeseries. Returns modified copy."""
    rng = get_rng(seed)
    out = timeseries.copy()
    n = len(out)

    if isinstance(artifact_type, str):
        artifact_type = ArtifactType(artifact_type)

    if artifact_type == ArtifactType.THERMAL_DRIFT:
        drift_rate = params.get("drift_rate", 0.01)
        start_idx = int(params.get("start_frac", 0.3) * n)
        for i in range(start_idx, n):
            out[i] += drift_rate * (i - start_idx)

    elif artifact_type == ArtifactType.SENSOR_LAG:
        lag_steps = int(params.get("lag_steps", 5))
        alpha = params.get("alpha", 0.3)
        filtered = np.zeros_like(out)
        filtered[0] = out[0]
        for i in range(1, n):
            filtered[i] = alpha * out[i] + (1 - alpha) * filtered[i - 1]
        out = filtered

    elif artifact_type == ArtifactType.RPM_RIPPLE:
        amplitude = params.get("amplitude", 2.0)
        period = params.get("period", 10)
        ripple = amplitude * np.sin(2 * np.pi * np.arange(n) / period) + rng.normal(0, 0.5, n)
        out = out + ripple

    elif artifact_type == ArtifactType.EMI_BURST:
        burst_amplitude = params.get("amplitude", 0.5)
        burst_start = int(params.get("start_frac", 0.5) * n)
        burst_len = int(params.get("length_frac", 0.05) * n)
        for i in range(burst_start, min(burst_start + burst_len, n)):
            out[i] += burst_amplitude * rng.uniform(0.5, 1.5)

    elif artifact_type == ArtifactType.VIBRATION_SPIKE:
        spike_amplitude = params.get("amplitude", 5.0)
        spike_idx = int(params.get("position_frac", 0.6) * n)
        if 0 <= spike_idx < n:
            out[spike_idx] += spike_amplitude * rng.uniform(0.8, 1.2)

    return out
