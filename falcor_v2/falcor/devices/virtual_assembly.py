"""Virtual assembly model: parametric components, materials, transforms."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from pydantic import BaseModel, Field

from falcor.core.errors import DeviceError


class Transform(BaseModel):
    """Coordinate transform (translation + rotation)."""

    x: float = 0.0
    y: float = 0.0
    z: float = 0.0
    rx_deg: float = 0.0
    ry_deg: float = 0.0
    rz_deg: float = 0.0


class Component(BaseModel):
    """Parametric component in a virtual assembly."""

    id: str
    shape: str = Field(..., description="cylinder, disk, sphere, box, coil")
    material: str = "aluminum"
    transform: Transform = Field(default_factory=Transform)
    params: dict[str, float] = Field(default_factory=dict)


class VirtualAssembly(BaseModel):
    """Virtual assembly: components, materials, metadata."""

    name: str = "assembly"
    version: str = "1.0"
    components: list[Component] = Field(default_factory=list)
    materials: dict[str, dict[str, float]] = Field(default_factory=dict)

    def get_components(self) -> list[dict[str, Any]]:
        """Return component definitions as dicts."""
        return [c.model_dump() for c in self.components]

    def get_materials(self) -> dict[str, dict[str, float]]:
        """Return merged material properties."""
        from falcor.devices.materials import MATERIALS

        out: dict[str, dict[str, float]] = {}
        for c in self.components:
            mat = c.material
            if mat in self.materials:
                out[mat] = self.materials[mat]
            elif mat in MATERIALS:
                out[mat] = MATERIALS[mat].copy()
        return out

    def get_geometry_params(self) -> dict[str, Any]:
        """Return geometry for rendering."""
        return {
            "components": [c.model_dump() for c in self.components],
        }


def load_assembly(path: Path | str) -> VirtualAssembly:
    """Load virtual assembly from JSON file."""
    p = Path(path)
    if not p.exists():
        raise DeviceError(f"Assembly file not found: {p}")
    with open(p, encoding="utf-8") as f:
        data = json.load(f)
    return VirtualAssembly.model_validate(data)
