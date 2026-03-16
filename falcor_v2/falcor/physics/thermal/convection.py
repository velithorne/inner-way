"""Convection to ambient. SI units."""

from __future__ import annotations


def convective_flux(h: float, area: float, T_surface: float, T_ambient: float) -> float:
    """Q = h * A * (T_surface - T_ambient) (W)."""
    return h * area * (T_surface - T_ambient)
