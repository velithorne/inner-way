"""Tests for energy audit."""

import pytest
from falcor.experiments.energy_audit import EnergyAudit, EnergyAuditResult, EnergyBucket
from falcor.core.config import AuditConfig


def test_energy_audit_basic():
    audit = EnergyAudit()
    result = audit.compute(
        motor_power=10.0,
        motor_power_sigma=0.5,
        coil_power=5.0,
        coil_power_sigma=0.2,
        dT_dt_aggregate=0.01,
        heat_capacity_aggregate=100.0,
        P_thermal_sigma=0.1,
    )
    assert result.Pin.value == 15.0
    assert result.Pthermal.value == 1.0  # C * dT_dt
    assert result.Pin.uncertainty > 0


def test_energy_audit_flags_negative_ploss():
    audit = EnergyAudit(AuditConfig(flag_impossible=True))
    result = audit.compute(
        motor_power=0.1,
        motor_power_sigma=0.01,
        coil_power=0.0,
        coil_power_sigma=0.0,
        dT_dt_aggregate=10.0,
        heat_capacity_aggregate=100.0,
        P_thermal_sigma=0.1,
    )
    # Pin=0.1, Pthermal=1000 -> Ploss very negative
    assert not result.is_stable
    assert any("negative" in f.lower() for f in result.flags)
