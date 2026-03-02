"""Energy audit: Pin, Pmech, Pthermal, PEM, losses with uncertainty."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

from falcor.core.config import AuditConfig


@dataclass
class EnergyBucket:
    """Energy/power bucket with uncertainty."""

    name: str
    value: float
    uncertainty: float
    label: str = ""  # e.g. "estimate", "approximation", "unknown/unmodeled"


@dataclass
class EnergyAuditResult:
    """Full energy audit result."""

    Pin: EnergyBucket = field(default_factory=lambda: EnergyBucket("Pin", 0.0, 0.0))
    Pmech: EnergyBucket = field(default_factory=lambda: EnergyBucket("Pmech", 0.0, 0.0))
    Pthermal: EnergyBucket = field(default_factory=lambda: EnergyBucket("Pthermal", 0.0, 0.0))
    PEM: EnergyBucket = field(default_factory=lambda: EnergyBucket("PEM", 0.0, 0.0))
    Ploss: EnergyBucket = field(default_factory=lambda: EnergyBucket("Ploss", 0.0, 0.0))
    unknown: EnergyBucket = field(
        default_factory=lambda: EnergyBucket("unknown", 0.0, 0.0, "unknown/unmodeled")
    )
    flags: list[str] = field(default_factory=list)
    is_stable: bool = True

    def to_dict(self) -> dict[str, Any]:
        return {
            "Pin": {"value": self.Pin.value, "uncertainty": self.Pin.uncertainty, "label": self.Pin.label},
            "Pmech": {"value": self.Pmech.value, "uncertainty": self.Pmech.uncertainty, "label": self.Pmech.label},
            "Pthermal": {"value": self.Pthermal.value, "uncertainty": self.Pthermal.uncertainty, "label": self.Pthermal.label},
            "PEM": {"value": self.PEM.value, "uncertainty": self.PEM.uncertainty, "label": self.PEM.label},
            "Ploss": {"value": self.Ploss.value, "uncertainty": self.Ploss.uncertainty, "label": self.Ploss.label},
            "unknown": {"value": self.unknown.value, "uncertainty": self.unknown.uncertainty, "label": self.unknown.label},
            "flags": self.flags,
            "is_stable": self.is_stable,
        }


class EnergyAudit:
    """Compute energy accounting with uncertainty."""

    def __init__(self, config: AuditConfig | None = None):
        self.config = config or AuditConfig()

    def compute(
        self,
        motor_power: float,
        motor_power_sigma: float,
        coil_power: float,
        coil_power_sigma: float,
        dT_dt_aggregate: float,
        heat_capacity_aggregate: float,
        P_thermal_sigma: float,
        dE_em_dt: float = 0.0,
        torque: float | None = None,
        omega: float | None = None,
    ) -> EnergyAuditResult:
        """
        Compute audit. Labels estimates and unknown buckets.
        """
        result = EnergyAuditResult()
        flags: list[str] = []

        # Pin = motor + coil
        pin_sigma = (motor_power_sigma**2 + coil_power_sigma**2) ** 0.5 if self.config.compute_uncertainty else 0.0
        result.Pin = EnergyBucket(
            "Pin",
            motor_power + coil_power,
            pin_sigma,
        )

        # Pmech = torque * omega (or estimate)
        if torque is not None and omega is not None:
            result.Pmech = EnergyBucket("Pmech", torque * omega, 0, "computed")
        else:
            result.Pmech = EnergyBucket("Pmech", 0.0, 0.0, "estimate (torque not modeled)")

        # Pthermal = C * dT/dt
        P_thermal = heat_capacity_aggregate * dT_dt_aggregate
        result.Pthermal = EnergyBucket("Pthermal", P_thermal, P_thermal_sigma)

        # PEM
        result.PEM = EnergyBucket("PEM", dE_em_dt, 0, "delta EM stored energy" if dE_em_dt != 0 else "0 (not computed)")

        # Ploss = remainder
        result.Ploss = EnergyBucket(
            "Ploss",
            result.Pin.value - result.Pmech.value - result.Pthermal.value - result.PEM.value,
            0,
        )

        if result.Ploss.value < -0.01:
            flags.append("negative Ploss: possible model/data error")
            result.is_stable = False
        if result.Pthermal.value < 0 and abs(result.Pthermal.value) > 0.01:
            flags.append("negative Pthermal: check temperature data")
        if self.config.flag_impossible:
            result.flags = flags

        return result
