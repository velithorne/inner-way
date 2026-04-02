"""Base sensor interface."""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any


class BaseSensor(ABC):
    """Abstract sensor interface."""

    @property
    @abstractmethod
    def sensor_id(self) -> str:
        ...

    @property
    @abstractmethod
    def unit(self) -> str:
        ...

    @abstractmethod
    def read(self) -> tuple[float, float]:
        """Return (value, uncertainty)."""
        ...

    def get_calibration(self) -> dict[str, Any]:
        """Return calibration profile."""
        return {}
