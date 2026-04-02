"""Deterministic RNG for reproducible runs."""

from __future__ import annotations

from typing import TYPE_CHECKING

import numpy as np

if TYPE_CHECKING:
    from numpy.random import Generator


_default_seed: int | None = None


def set_default_seed(seed: int) -> None:
    """Set default RNG seed."""
    global _default_seed
    _default_seed = seed


def get_rng(seed: int | None = None) -> Generator:
    """Get a deterministic numpy Generator."""
    s = seed if seed is not None else _default_seed
    if s is None:
        s = 42
    return np.random.default_rng(s)


def create_run_rng(run_id: str, config_seed: int | None = None) -> Generator:
    """Create RNG seeded from run_id and optional config seed."""
    base = config_seed if config_seed is not None else 42
    combined = hash(run_id) % (2**32) + base
    return np.random.default_rng(abs(combined))
