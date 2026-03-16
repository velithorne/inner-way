"""FIELD mode sensors. Stub when hardware unavailable."""

from __future__ import annotations

from falcor.core.logging import get_logger

logger = get_logger("field_sensors")


def create_field_sensor_stub(sensor_id: str, default_value: float = 0.0):
    """Create stub sensor for FIELD mode when hardware unavailable."""
    logger.warning("FIELD mode: sensor %s unavailable, using stub (value=%.3f)", sensor_id, default_value)

    class StubSensor:
        @property
        def sensor_id(self):
            return sensor_id

        @property
        def unit(self):
            return ""

        def read(self):
            return default_value, 0.0

    return StubSensor()
