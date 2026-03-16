"""Simulated sensors for SIM mode."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Callable

from falcor.measurement.sensors.base_sensor import BaseSensor
from falcor.core.rng import get_rng


@dataclass
class CalibrationProfile:
    """One-sensor calibration: offset, scale, noise_sigma, lag."""

    offset: float = 0.0
    scale: float = 1.0
    noise_sigma: float = 0.0
    lag_s: float = 0.0


class SimTemperatureSensor(BaseSensor):
    """Simulated temperature sensor (IR or contact) with lag + noise."""

    def __init__(
        self,
        sensor_id: str,
        truth_fn: Callable[[], float],
        noise_sigma: float = 0.5,
        lag_s: float = 0.1,
        rng=None,
    ):
        self._id = sensor_id
        self._truth_fn = truth_fn
        self._noise = noise_sigma
        self._lag = lag_s
        self._rng = rng or get_rng()
        self._cal = CalibrationProfile(noise_sigma=noise_sigma, lag_s=lag_s)
        self._buffer: list[tuple[float, float]] = []

    @property
    def sensor_id(self) -> str:
        return self._id

    @property
    def unit(self) -> str:
        return "K"

    def read(self) -> tuple[float, float]:
        raw = self._truth_fn() + self._rng.normal(0, self._noise)
        return raw, self._noise

    def get_calibration(self) -> dict[str, Any]:
        return {
            "offset": self._cal.offset,
            "scale": self._cal.scale,
            "noise_sigma": self._cal.noise_sigma,
            "lag_s": self._cal.lag_s,
        }


class SimEMSensor(BaseSensor):
    """Simulated EM pickup (amplitude + noise floor)."""

    def __init__(self, sensor_id: str, truth_fn: Callable[[], float], noise_floor: float = 0.01, rng=None):
        self._id = sensor_id
        self._truth_fn = truth_fn
        self._noise = noise_floor
        self._rng = rng or get_rng()

    @property
    def sensor_id(self) -> str:
        return self._id

    @property
    def unit(self) -> str:
        return "T"

    def read(self) -> tuple[float, float]:
        raw = max(0, self._truth_fn() + self._rng.normal(0, self._noise))
        return raw, self._noise


class SimVibrationSensor(BaseSensor):
    """Simulated vibration (rms + max)."""

    def __init__(self, sensor_id: str, truth_fn: Callable[[], float], noise_sigma: float = 0.01, rng=None):
        self._id = sensor_id
        self._truth_fn = truth_fn
        self._noise = noise_sigma
        self._rng = rng or get_rng()

    @property
    def sensor_id(self) -> str:
        return self._id

    @property
    def unit(self) -> str:
        return "m/s^2"

    def read(self) -> tuple[float, float]:
        raw = max(0, self._truth_fn() + self._rng.normal(0, self._noise))
        return raw, self._noise


class SimMotorSensor(BaseSensor):
    """Simulated motor V, I, or P."""

    def __init__(self, sensor_id: str, truth_fn: Callable[[], float], noise_sigma: float = 0.1, rng=None):
        self._id = sensor_id
        self._truth_fn = truth_fn
        self._noise = noise_sigma
        self._rng = rng or get_rng()

    @property
    def sensor_id(self) -> str:
        return self._id

    @property
    def unit(self) -> str:
        if "v" in self._id.lower():
            return "V"
        if "i" in self._id.lower():
            return "A"
        return "W"

    def read(self) -> tuple[float, float]:
        raw = self._truth_fn() + self._rng.normal(0, self._noise)
        return raw, self._noise


class SimRPMSensor(BaseSensor):
    """Simulated RPM (target, actual, error)."""

    def __init__(self, sensor_id: str, truth_fn: Callable[[], float], noise_sigma: float = 1.0, rng=None):
        self._id = sensor_id
        self._truth_fn = truth_fn
        self._noise = noise_sigma
        self._rng = rng or get_rng()

    @property
    def sensor_id(self) -> str:
        return self._id

    @property
    def unit(self) -> str:
        return "rpm"

    def read(self) -> tuple[float, float]:
        raw = max(0, self._truth_fn() + self._rng.normal(0, self._noise))
        return raw, self._noise


class SimSensorHub:
    """Hub of simulated sensors."""

    def __init__(self):
        self.sensors: dict[str, BaseSensor] = {}

    def add(self, sensor: BaseSensor) -> None:
        self.sensors[sensor.sensor_id] = sensor

    def read_all(self) -> dict[str, tuple[float, float]]:
        return {sid: s.read() for sid, s in self.sensors.items()}
