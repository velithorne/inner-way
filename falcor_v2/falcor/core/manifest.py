"""Manifest creation and hashing for run reproducibility."""

from __future__ import annotations

import hashlib
import json
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any


@dataclass
class Manifest:
    """Immutable manifest for a run."""

    run_id: str
    config_snapshot: dict[str, Any]
    device_assembly: dict[str, Any] | None
    git_commit: str | None
    lib_versions: dict[str, str]
    manifest_hash: str

    def to_dict(self) -> dict[str, Any]:
        return {
            "run_id": self.run_id,
            "config_snapshot": self.config_snapshot,
            "device_assembly": self.device_assembly,
            "git_commit": self.git_commit,
            "lib_versions": self.lib_versions,
            "manifest_hash": self.manifest_hash,
        }


def _get_git_commit() -> str | None:
    """Get current git commit hash if in a repo."""
    try:
        result = subprocess.run(
            ["git", "rev-parse", "HEAD"],
            capture_output=True,
            text=True,
            timeout=5,
            cwd=Path(__file__).resolve().parent.parent.parent,
        )
        if result.returncode == 0:
            return result.stdout.strip()
    except (subprocess.TimeoutExpired, FileNotFoundError):
        pass
    return None


def _get_lib_versions() -> dict[str, str]:
    """Get versions of key libraries."""
    versions: dict[str, str] = {}
    for name in ["numpy", "scipy", "pydantic", "pyarrow", "pandas", "matplotlib"]:
        try:
            mod = __import__(name)
            versions[name] = getattr(mod, "__version__", "unknown")
        except ImportError:
            versions[name] = "not installed"
    versions["python"] = f"{sys.version_info.major}.{sys.version_info.minor}"
    return versions


def _compute_hash(data: dict[str, Any]) -> str:
    """Compute SHA256 hash of canonical JSON."""
    canonical = json.dumps(data, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode()).hexdigest()[:16]


def create_manifest(
    run_id: str,
    config_snapshot: dict[str, Any],
    device_assembly: dict[str, Any] | None = None,
) -> Manifest:
    """Create an immutable manifest for a run."""
    git_commit = _get_git_commit()
    lib_versions = _get_lib_versions()

    hash_data = {
        "run_id": run_id,
        "config": config_snapshot,
        "device": device_assembly or {},
        "git": git_commit or "",
        "libs": lib_versions,
    }
    manifest_hash = _compute_hash(hash_data)

    return Manifest(
        run_id=run_id,
        config_snapshot=config_snapshot,
        device_assembly=device_assembly,
        git_commit=git_commit,
        lib_versions=lib_versions,
        manifest_hash=manifest_hash,
    )
