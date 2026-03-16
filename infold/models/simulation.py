"""Dry-run output for fold and unfold attempts."""

from dataclasses import dataclass
from typing import Any


@dataclass
class FoldSimulationResult:
    """Dry-run output for the fold and unfold attempt."""

    folded_state: Any  # simulated folded representation
    unfold_attempt: Any  # reconstructed output from dry unfold
    success: bool
    error: str | None  # if success is False
