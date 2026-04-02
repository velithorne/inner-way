"""Thermodynamic relations. SI units."""

from __future__ import annotations


def heat_capacity(mass: float, cp: float) -> float:
    """C = m * cp (J/K)."""
    return mass * cp


def dT_from_energy(dQ: float, C: float) -> float:
    """dT = dQ / C (K)."""
    if C <= 0:
        return 0.0
    return dQ / C
