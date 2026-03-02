"""Blind trial runner: randomized conditions, sealed truth."""

from __future__ import annotations

import json
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from falcor.core.rng import get_rng


@dataclass
class SealedTruth:
    """Sealed truth (hidden until reveal)."""

    magnet_states: list[str]
    anomaly_injected: list[bool]
    artifact_params: list[dict[str, Any]]


class BlindTrialRunner:
    """Run experiments with randomized hidden conditions."""

    def __init__(self, seed: int | None = None):
        self.rng = get_rng(seed)
        self._sealed: SealedTruth | None = None

    def randomize_conditions(
        self,
        n_trials: int,
        magnet_options: list[str],
        inject_anomaly_frac: float = 0.0,
    ) -> SealedTruth:
        """Generate randomized conditions. Store as sealed truth."""
        magnet_states = list(self.rng.choice(magnet_options, size=n_trials))
        anomaly_injected = [self.rng.random() < inject_anomaly_frac for _ in range(n_trials)]
        artifact_params = [{} for _ in range(n_trials)]
        self._sealed = SealedTruth(
            magnet_states=magnet_states,
            anomaly_injected=anomaly_injected,
            artifact_params=artifact_params,
        )
        return self._sealed

    def get_condition_for_trial(self, trial_idx: int) -> dict[str, Any]:
        """Get condition for trial (without revealing truth). Returns only what runner needs."""
        if self._sealed is None:
            return {"magnet_state": "OFF", "inject_artifact": False}
        return {
            "magnet_state": self._sealed.magnet_states[trial_idx],
            "inject_artifact": self._sealed.anomaly_injected[trial_idx],
        }

    def save_sealed(self, path: Path) -> None:
        """Save sealed truth to JSON."""
        if self._sealed is None:
            return
        data = {
            "magnet_states": self._sealed.magnet_states,
            "anomaly_injected": self._sealed.anomaly_injected,
            "artifact_params": self._sealed.artifact_params,
        }
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)

    def load_sealed(self, path: Path) -> SealedTruth:
        """Load sealed truth (reveal)."""
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        self._sealed = SealedTruth(
            magnet_states=data["magnet_states"],
            anomaly_injected=data["anomaly_injected"],
            artifact_params=data["artifact_params"],
        )
        return self._sealed
