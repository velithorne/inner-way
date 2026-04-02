"""Rotation utilities."""

from __future__ import annotations

import numpy as np

from falcor.core.units import rpm_to_rad_s


def angular_velocity_to_linear(r: float, omega_rad_s: float) -> float:
    """v = r * omega (m/s)."""
    return r * omega_rad_s
