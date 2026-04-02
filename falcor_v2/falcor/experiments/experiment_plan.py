"""Experiment plan: sweep grid, schedules, parameters."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Literal

import numpy as np


@dataclass
class ExperimentPlan:
    """Experiment plan with frequency sweep, RPM, magnet state."""

    mode: Literal["SIM", "FIELD"] = "SIM"
    frequency_start_hz: float = 1.0
    frequency_stop_hz: float = 100.0
    frequency_steps: int = 10
    rpm_schedule: list[float] | None = None
    magnet_state_schedule: list[Literal["OFF", "ON", "PROFILE"]] | None = None
    dwell_time_s: float = 5.0
    settle_time_s: float = 2.0
    sample_rate_hz: float = 10.0
    seed: int | None = None

    def __post_init__(self):
        if self.magnet_state_schedule is None:
            self.magnet_state_schedule = ["OFF", "ON"]
        if self.rpm_schedule is None:
            self.rpm_schedule = [100.0]

    def get_frequency_grid(self) -> np.ndarray:
        """Linear frequency grid."""
        return np.linspace(
            self.frequency_start_hz,
            self.frequency_stop_hz,
            self.frequency_steps,
        )

    def get_sweep_steps(self) -> list[dict]:
        """List of (freq, rpm, magnet_state) steps."""
        steps = []
        freqs = self.get_frequency_grid()
        for f in freqs:
            for rpm in self.rpm_schedule or [100.0]:
                for mag in self.magnet_state_schedule or ["OFF", "ON"]:
                    steps.append({"frequency_hz": float(f), "rpm": rpm, "magnet_state": mag})
        return steps
