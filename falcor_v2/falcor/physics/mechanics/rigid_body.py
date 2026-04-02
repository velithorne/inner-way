"""Rigid body rotational kinematics. SI units."""

from __future__ import annotations

from falcor.core.units import rpm_to_rad_s, rad_s_to_rpm


class RigidBodyKinematics:
    """Rotational kinematics: omega, rpm, acceleration."""

    def __init__(self, omega_rad_s: float = 0.0):
        self.omega = omega_rad_s

    @property
    def rpm(self) -> float:
        return rad_s_to_rpm(self.omega)

    @rpm.setter
    def rpm(self, value: float) -> None:
        self.omega = rpm_to_rad_s(value)

    def set_rpm(self, rpm: float) -> None:
        self.omega = rpm_to_rad_s(rpm)

    def step(self, alpha_rad_s2: float, dt: float) -> None:
        """Integrate angular acceleration."""
        self.omega += alpha_rad_s2 * dt

    def limit_acceleration(self, alpha_max_rad_s2: float, target_omega: float) -> float:
        """Return clamped alpha to respect limit."""
        delta = target_omega - self.omega
        if abs(delta) <= alpha_max_rad_s2:
            return delta
        return alpha_max_rad_s2 if delta > 0 else -alpha_max_rad_s2
