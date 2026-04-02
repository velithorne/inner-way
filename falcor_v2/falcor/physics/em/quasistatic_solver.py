"""Quasi-static coil field solver (default, fast)."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from falcor.physics.em.coils import coil_B_axial, coil_B_magnitude


@dataclass
class CoilSpec:
    """Coil primitive specification."""

    radius_m: float
    turns: int
    height_m: float
    x: float = 0.0
    y: float = 0.0
    z: float = 0.0


class QuasistaticSolver:
    """Quasi-static coil field solver."""

    def __init__(self, coils: list[CoilSpec]):
        self.coils = coils

    def B_at(self, x: float, y: float, z: float, I_per_coil: float = 1.0) -> float:
        """Total |B| at point (T). Superposition of coils."""
        total = 0.0
        for c in self.coils:
            dx = x - c.x
            dy = y - c.y
            dz = z - c.z
            total += coil_B_magnitude(dx, dy, dz, c.radius_m, I_per_coil, c.turns)
        return total

    def B_axial(self, z: float, I_per_coil: float = 1.0) -> float:
        """B on axis at z (T)."""
        total = 0.0
        for c in self.coils:
            dz = z - c.z
            total += coil_B_axial(dz, c.radius_m, I_per_coil, c.turns)
        return total

    def gradient_B2_axial(self, z: float, dz: float, I: float = 1.0) -> float:
        """d(B^2)/dz along axis (T^2/m). For diamagnetic force proxy."""
        Bp = self.B_axial(z + dz, I)
        Bm = self.B_axial(z - dz, I)
        return (Bp**2 - Bm**2) / (2 * dz)
