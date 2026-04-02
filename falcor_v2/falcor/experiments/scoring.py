"""Scoring and pass/fail for experiment steps."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass
class StepScore:
    """Per-step score with pass/fail flags."""

    step_idx: int
    frequency_hz: float
    magnet_state: str
    pass_: bool
    flags: list[str]
    metrics: dict[str, Any]


def score_step(
    step_idx: int,
    frequency_hz: float,
    magnet_state: str,
    temps: dict[str, float],
    slopes: dict[str, float],
    em_metrics: dict[str, float],
    vib: float,
    power: float,
    thresholds: dict[str, float] | None = None,
) -> StepScore:
    """Score a single step. Pass if no threshold exceeded."""
    thresholds = thresholds or {}
    flags: list[str] = []
    for k, v in temps.items():
        if v < 0:
            flags.append(f"negative temp {k}")
        if v > 500:  # K
            flags.append(f"impossibly high temp {k}")
    if vib > thresholds.get("vib_max", 100):
        flags.append("vibration exceeded")
    if power < -0.01:
        flags.append("negative power")
    return StepScore(
        step_idx=step_idx,
        frequency_hz=frequency_hz,
        magnet_state=magnet_state,
        pass_=len(flags) == 0,
        flags=flags,
        metrics={"temps": temps, "slopes": slopes, "em": em_metrics, "vib": vib, "power": power},
    )
