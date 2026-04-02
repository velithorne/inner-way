"""Base device interface."""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any


class BaseDevice(ABC):
    """Abstract base for parametric virtual assemblies."""

    @abstractmethod
    def get_components(self) -> list[dict[str, Any]]:
        """Return list of component definitions."""
        ...

    @abstractmethod
    def get_materials(self) -> dict[str, dict[str, float]]:
        """Return material properties (SI units)."""
        ...

    @abstractmethod
    def get_geometry_params(self) -> dict[str, Any]:
        """Return geometry parameters for rendering."""
        ...
