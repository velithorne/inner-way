"""Conduction heat transfer. SI units."""

from __future__ import annotations


def conductive_flux(k: float, area: float, dT: float, length: float) -> float:
    """Q = k * A * dT / L (W)."""
    if length <= 0:
        return 0.0
    return k * area * dT / length


def thermal_resistance(length: float, k: float, area: float) -> float:
    """R_th = L / (k*A) (K/W)."""
    if k * area <= 0:
        return 1e12
    return length / (k * area)
