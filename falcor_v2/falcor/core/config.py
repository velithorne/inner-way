"""Pydantic configuration models for FALCOR v2."""

from __future__ import annotations

from pathlib import Path
from typing import Literal

from pydantic import BaseModel, Field


class PhysicsConfig(BaseModel):
    """Physics solver settings."""

    thermal_solver: Literal["lumped", "voxel"] = "lumped"
    thermal_dt_max: float = Field(0.1, gt=0, description="Max thermal timestep (s)")
    em_solver: Literal["quasistatic", "fdtd"] = "quasistatic"
    mechanics_enabled: bool = True
    coupling_enabled: bool = True


class SensorConfig(BaseModel):
    """Sensor configuration."""

    sample_rate_hz: float = Field(10.0, gt=0)
    temperature_sensors: list[str] = Field(default_factory=lambda: ["temp_ir", "temp_contact"])
    em_sensors: list[str] = Field(default_factory=lambda: ["em_pickup"])
    vibration_sensors: list[str] = Field(default_factory=lambda: ["vib_rms"])
    motor_sensors: list[str] = Field(default_factory=lambda: ["motor_v", "motor_i"])
    rpm_sensors: list[str] = Field(default_factory=lambda: ["rpm_target", "rpm_actual"])


class AuditConfig(BaseModel):
    """Energy audit and reporting settings."""

    compute_uncertainty: bool = True
    flag_impossible: bool = True
    unknown_bucket_label: str = "unknown/unmodeled"


class ExperimentConfig(BaseModel):
    """Experiment plan configuration."""

    mode: Literal["SIM", "FIELD"] = "SIM"
    frequency_sweep_start_hz: float = Field(1.0, ge=0)
    frequency_sweep_stop_hz: float = Field(100.0, ge=0)
    frequency_sweep_steps: int = Field(10, ge=1)
    rpm_target_schedule: list[float] = Field(default_factory=list)
    magnet_state_schedule: list[Literal["OFF", "ON", "PROFILE"]] = Field(
        default_factory=lambda: ["OFF", "ON"]
    )
    dwell_time_s: float = Field(5.0, gt=0)
    settle_time_s: float = Field(2.0, ge=0)
    sample_rate_hz: float = Field(10.0, gt=0)
    seed: int | None = Field(default=None, description="RNG seed for reproducibility")


class DeviceConfig(BaseModel):
    """Device/virtual assembly reference."""

    path: Path | str = Field(..., description="Path to virtual assembly JSON")
    name: str = ""


class AppConfig(BaseModel):
    """Top-level application configuration."""

    lab_results_dir: Path | str = Field(default="lab_results")
    device: DeviceConfig | None = None
    experiment: ExperimentConfig = Field(default_factory=ExperimentConfig)
    physics: PhysicsConfig = Field(default_factory=PhysicsConfig)
    sensors: SensorConfig = Field(default_factory=SensorConfig)
    audit: AuditConfig = Field(default_factory=AuditConfig)

    model_config = {"arbitrary_types_allowed": True}
