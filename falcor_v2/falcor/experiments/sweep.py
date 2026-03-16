"""Sweep utilities."""

from __future__ import annotations

from falcor.experiments.experiment_plan import ExperimentPlan


def build_sweep(
    freq_start: float = 1.0,
    freq_stop: float = 100.0,
    steps: int = 10,
    magnet_states: list[str] | None = None,
    dwell_s: float = 5.0,
) -> ExperimentPlan:
    """Build a frequency sweep plan."""
    return ExperimentPlan(
        frequency_start_hz=freq_start,
        frequency_stop_hz=freq_stop,
        frequency_steps=steps,
        magnet_state_schedule=magnet_states or ["OFF", "ON"],
        dwell_time_s=dwell_s,
    )
