"""Radiation to ambient. SI units."""

from __future__ import annotations

from falcor.core.units import STEFAN_BOLTZMANN


def radiative_flux(
    epsilon: float,
    area: float,
    T_surface: float,
    T_ambient: float,
) -> float:
    """Q = epsilon * sigma * A * (T_s^4 - T_a^4) (W)."""
    return epsilon * STEFAN_BOLTZMANN * area * (T_surface**4 - T_ambient**4)
