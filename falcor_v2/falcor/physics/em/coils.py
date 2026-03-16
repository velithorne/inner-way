"""Coil field approximations. Quasi-static, axisymmetric."""

from __future__ import annotations

import numpy as np

from falcor.core.units import MU_0


def coil_B_axial(z: float, R: float, I: float, N: int) -> float:
    """
    B field on axis of circular coil (T).
    B = mu0 * N * I * R^2 / (2 * (R^2 + z^2)^(3/2))
    """
    denom = (R**2 + z**2) ** 1.5
    if denom < 1e-20:
        return 0.0
    return MU_0 * N * I * (R**2) / (2 * denom)


def coil_B_magnitude(x: float, y: float, z: float, R: float, I: float, N: int) -> float:
    """
    Approximate |B| for single coil. On-axis formula extended; off-axis is approximation.
    For z on axis: use coil_B_axial. Off-axis: simplified dipole-like falloff.
    """
    r_axial = np.sqrt(x**2 + y**2)
    if r_axial < R * 0.1:
        return abs(coil_B_axial(z, R, I, N))
    # Off-axis: rough approximation
    dist = np.sqrt(r_axial**2 + z**2)
    if dist < 1e-10:
        return 0.0
    B_axis = coil_B_axial(z, R, I, N)
    return abs(B_axis) * (R / max(dist, R)) ** 2
