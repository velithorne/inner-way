"""SI unit conversions and constants. All internal computation uses SI."""

from __future__ import annotations

# Thermal
KELVIN_OFFSET = 273.15


def celsius_to_kelvin(T_c: float) -> float:
    """Convert Celsius to Kelvin."""
    return T_c + KELVIN_OFFSET


def kelvin_to_celsius(T_k: float) -> float:
    """Convert Kelvin to Celsius."""
    return T_k - KELVIN_OFFSET


# Rotation
def rpm_to_rad_s(rpm: float) -> float:
    """Convert RPM to rad/s."""
    return rpm * (2 * 3.141592653589793) / 60.0


def rad_s_to_rpm(rad_s: float) -> float:
    """Convert rad/s to RPM."""
    return rad_s * 60.0 / (2 * 3.141592653589793)


# EM constants (SI)
MU_0 = 4e-7 * 3.141592653589793  # H/m
EPS_0 = 8.854187817e-12  # F/m

# Thermal
STEFAN_BOLTZMANN = 5.670374419e-8  # W/(m^2 K^4)
