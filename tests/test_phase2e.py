"""Phase 2E tests: benchmark campaign, validation modes, bad archives."""

import json
import zipfile
from pathlib import Path

import pytest
import sys

sys.path.insert(0, str(Path(__file__).parent.parent))

from infold.benchmark import get_benchmark_datasets, run_benchmark_campaign
from infold.benchmark.pack import ensure_stress_datasets
from infold.archive import validate_archive
from infold.archive.operations import VALIDATION_MODES


def test_validation_modes():
    """Validation modes are defined."""
    assert "basic" in VALIDATION_MODES
    assert "strict" in VALIDATION_MODES
    assert "integrity-only" in VALIDATION_MODES
    assert "schema-only" in VALIDATION_MODES


def test_validate_invalid_mode():
    """Invalid mode returns error."""
    ok, errors = validate_archive("/nonexistent.infold", mode="invalid")
    assert not ok
    assert any("Invalid validation mode" in e for e in errors)


def test_validate_nonexistent():
    """Nonexistent archive fails."""
    ok, errors = validate_archive("/tmp/nonexistent_archive_xyz.infold")
    assert not ok
    assert any("not found" in e.lower() for e in errors)


def test_validate_bad_zip(tmp_path):
    """Invalid zip fails."""
    bad = tmp_path / "bad.infold"
    bad.write_text("not a zip file")
    ok, errors = validate_archive(bad)
    assert not ok
    assert any("zip" in e.lower() for e in errors)


def test_validate_corrupt_manifest(tmp_path):
    """Archive with corrupt manifest fails in strict mode."""
    archive = tmp_path / "corrupt.infold"
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("manifest.json", "{ invalid json")
        zf.writestr("ledger.json", "{}")
        zf.writestr("shared/.gitkeep", "")
        zf.writestr("maps/.gitkeep", "")
        zf.writestr("reports/.gitkeep", "")
        zf.writestr("snapshots/.gitkeep", "")
    ok, errors = validate_archive(archive, mode="strict")
    assert not ok
    assert any("JSON" in e or "manifest" in e.lower() for e in errors)


def test_validate_missing_required(tmp_path):
    """Archive missing required files fails basic."""
    archive = tmp_path / "minimal.infold"
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("manifest.json", "{}")
        # no ledger
    ok, errors = validate_archive(archive, mode="basic")
    assert not ok
    assert any("ledger" in e.lower() for e in errors)


def test_benchmark_pack_datasets():
    """Benchmark pack returns datasets."""
    base = Path(__file__).parent.parent
    ensure_stress_datasets(base)
    datasets = get_benchmark_datasets(base)
    assert len(datasets) >= 6
    ids = [d[1] for d in datasets]
    assert "duplicate-heavy-python" in ids or "infold-workspace" in ids


def test_benchmark_campaign_no_archives():
    """Campaign runs without archive creation."""
    base = Path(__file__).parent.parent
    config_path = base / "infold" / "config.json"
    with open(config_path, encoding="utf-8") as f:
        config = json.load(f)
    config["project"] = config.get("project", {})
    datasets = get_benchmark_datasets(base)
    if not datasets:
        pytest.skip("No datasets")
    results = run_benchmark_campaign(datasets[:2], config, base, create_archives=False)
    assert len(results) >= 1
    r = results[0]
    assert "raw_bytes" in r
    assert "fold_count" in r
    assert "exact_reconstruction_status" in r
