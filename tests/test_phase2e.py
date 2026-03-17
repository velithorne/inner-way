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


def test_0fold_archive_validation(tmp_path):
    """0-fold archives pass strict validation (shared/ has .gitkeep when empty)."""
    from infold.engine import run_fold
    from infold.engine import export_package
    from infold.cli import load_config

    base = Path(__file__).parent.parent
    mixed = base / "tests" / "fixtures" / "mixed_project"
    if not mixed.exists():
        pytest.skip("mixed_project fixture not found")
    config = load_config()
    config["project"] = {**config.get("project", {}), "id": "mixed", "source_path": str(mixed)}
    result = run_fold(mixed, config)
    assert result.ledger.total_folds == 0
    pkg_dir = tmp_path / "pkg"
    export_package(result, config, pkg_dir)
    assert (pkg_dir / "shared").exists()
    assert (pkg_dir / "shared" / ".gitkeep").exists()
    # Create archive and validate
    import zipfile
    archive = tmp_path / "0fold.infold"
    with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as zf:
        for f in sorted(pkg_dir.rglob("*")):
            if f.is_file():
                zf.write(f, f.relative_to(pkg_dir))
    ok, errors = validate_archive(archive, mode="strict")
    assert ok, errors


def test_template_min_lines_configurable():
    """min_lines threshold is configurable via config."""
    from pathlib import Path
    from infold.engine import run_fold
    from infold.cli import load_config

    base = Path(__file__).parent.parent
    stress = base / "benchmark" / "synthetic" / "template_stress"
    if not stress.exists():
        pytest.skip("template_stress not found")
    config = load_config()
    config["project"] = {**config.get("project", {}), "id": "template-stress"}
    config.setdefault("_run_diagnostics", {})["template_rejected"] = []
    # With min_lines=5, 30 files skipped
    config["thresholds"]["template_skeleton"] = {**config["thresholds"]["template_skeleton"], "min_lines": 5}
    result5 = run_fold(stress, config)
    rejected5 = config["_run_diagnostics"].get("template_rejected", [])
    assert any("5 lines" in str(r.get("reject_reason", "")) for r in rejected5)
    # With min_lines=3, files are included (3-line files)
    config["_run_diagnostics"]["template_rejected"] = []
    config["thresholds"]["template_skeleton"]["min_lines"] = 3
    result3 = run_fold(stress, config)
    rejected3 = config["_run_diagnostics"].get("template_rejected", [])
    assert not any("3 lines" in str(r.get("reject_reason", "")) for r in rejected3)


def test_template_stress_diagnostics():
    """template_stress produces diagnostics explaining 0 folds (files < 5 lines)."""
    from pathlib import Path
    from infold.engine import run_fold
    from infold.cli import load_config

    base = Path(__file__).parent.parent
    stress = base / "benchmark" / "synthetic" / "template_stress"
    if not stress.exists():
        pytest.skip("template_stress not found")
    config = load_config()
    config["project"] = {**config.get("project", {}), "id": "template-stress"}
    config.setdefault("_run_diagnostics", {})["template_rejected"] = []
    result = run_fold(stress, config)
    rejected = config["_run_diagnostics"].get("template_rejected", [])
    assert len(rejected) >= 1
    r0 = rejected[0]
    assert "reject_reason" in r0
    assert "5 lines" in r0["reject_reason"] or "file_count" in r0["reject_reason"]


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
