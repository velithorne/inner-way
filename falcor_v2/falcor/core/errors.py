"""FALCOR exception hierarchy."""

from __future__ import annotations


class FalcorError(Exception):
    """Base exception for FALCOR."""


class ConfigError(FalcorError):
    """Configuration validation error."""


class DeviceError(FalcorError):
    """Device/virtual assembly error."""


class PhysicsError(FalcorError):
    """Physics solver error."""


class SensorError(FalcorError):
    """Sensor or measurement error."""


class StorageError(FalcorError):
    """Storage/IO error."""


class RunError(FalcorError):
    """Experiment run error."""
