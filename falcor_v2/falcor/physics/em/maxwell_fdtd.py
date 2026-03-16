"""Minimal FDTD placeholder. Smoke test level only."""

from __future__ import annotations

import numpy as np


class MaxwellFDTD:
    """Minimal FDTD placeholder. Interface only; minimal stepping."""

    def __init__(self, nx: int = 10, ny: int = 10, nz: int = 10, dx: float = 0.01):
        self.nx, self.ny, self.nz = nx, ny, nz
        self.dx = dx
        self.E = np.zeros((nx, ny, nz, 3))
        self.H = np.zeros((nx, ny, nz, 3))

    def step(self, dt: float) -> None:
        """Minimal step: zero update (placeholder)."""
        pass

    def get_B_at(self, i: int, j: int, k: int) -> float:
        """|B| at cell (placeholder)."""
        return 0.0
