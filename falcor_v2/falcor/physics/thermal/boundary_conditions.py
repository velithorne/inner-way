"""Boundary condition helpers."""

from __future__ import annotations


def ambient_temperature() -> float:
    """Default ambient in Kelvin."""
    return 293.15  # 20 C


def convective_coefficient_natural_air() -> float:
    """Approximate h for natural convection in air (W/(m^2 K))."""
    return 10.0
