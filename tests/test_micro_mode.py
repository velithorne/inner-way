"""
Phase 13A: Tests for micro-archive mode.
"""

import json
from pathlib import Path

import pytest

from infold.archive import create_archive, validate_archive, reconstruct_archive
from infold.archive.operations import explain_archive
from infold.cli import load_config, _apply_create_flags
from infold.benchmark.overhead_audit import audit_archive_overhead, overhead_audit_to_text


def test_apply_create_flags_micro():
    """Micro implies lean and sets _micro_mode."""
    config = load_config()
    args = type("Args", (), {"micro": True, "lean": False, "compact": False, "creature": True, "no_tesseract": False, "no_tesseract_cooperation": False})()
    cfg = _apply_create_flags(config, args)
    assert cfg["_micro_mode"] is True
    assert cfg["_lean_mode"] is True
    assert cfg["_creature_adaptive"] is False
    assert cfg["_tesseract_planner"] is False


def test_archive_create_micro_manifest(tmp_path):
    """Micro archive has minimal manifest (no required_files, required_dirs, fold_profile_mode/reason)."""
    base = Path(__file__).parent.parent
    dup = base / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("fixture missing")
    config = load_config()
    config["_micro_mode"] = True
    config["_lean_mode"] = True
    config["_creature_adaptive"] = False
    config["_tesseract_planner"] = False
    config["_tesseract_cooperation"] = False
    config["package_export"] = {"compact": True, "report_text": False, "inventory_minimal": True}
    config["_fold_profile"] = "fox"
    archive = tmp_path / "micro.infold"
    create_archive(dup, archive, config, profile="fox")
    ok, _ = validate_archive(archive, mode="strict")
    assert ok
    with archive.open("rb") as f:
        import zipfile
        z = zipfile.ZipFile(f, "r")
        m = json.loads(z.read("manifest.json"))
        if m.get("_m"):
            from infold.engine.compact_micro import decode_manifest_micro
            m = decode_manifest_micro(m)
        assert "required_files" not in m
        assert "required_dirs" not in m
        assert "fold_profile_mode" not in m
        assert "fold_profile_reason" not in m
        assert "fold_profile" in m
        assert "creature_enabled" in m


def test_micro_smaller_than_lean(tmp_path):
    """Micro archive is smaller than lean for small dataset."""
    base = Path(__file__).parent.parent
    dup = base / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("fixture missing")
    config = load_config()
    cfg_lean = dict(config)
    cfg_lean["_lean_mode"] = True
    cfg_lean["_creature_adaptive"] = False
    cfg_lean["_tesseract_planner"] = False
    cfg_lean["_tesseract_cooperation"] = False
    cfg_lean["package_export"] = {"compact": True, "report_text": False, "inventory_minimal": True}
    cfg_lean["_fold_profile"] = "fox"
    cfg_micro = dict(cfg_lean)
    cfg_micro["_micro_mode"] = True
    arc_lean = tmp_path / "lean.infold"
    arc_micro = tmp_path / "micro.infold"
    create_archive(dup, arc_lean, cfg_lean, profile="fox")
    create_archive(dup, arc_micro, cfg_micro, profile="fox")
    assert arc_micro.stat().st_size <= arc_lean.stat().st_size


def test_micro_exact_reconstruction(tmp_path):
    """Micro archive reconstructs exactly."""
    base = Path(__file__).parent.parent
    dup = base / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("fixture missing")
    config = load_config()
    config["_micro_mode"] = True
    config["_lean_mode"] = True
    config["_creature_adaptive"] = False
    config["_tesseract_planner"] = False
    config["_tesseract_cooperation"] = False
    config["package_export"] = {"compact": True, "report_text": False, "inventory_minimal": True}
    config["_fold_profile"] = "fox"
    archive = tmp_path / "micro.infold"
    create_archive(dup, archive, config, profile="fox")
    restored = tmp_path / "restored"
    reconstruct_archive(archive, restored)
    for f in dup.rglob("*"):
        if f.is_file():
            rel = f.relative_to(dup)
            r = restored / rel
            assert r.exists(), f"Missing {rel}"
            assert r.read_bytes() == f.read_bytes(), f"Mismatch {rel}"


def test_overhead_audit(tmp_path):
    """Overhead audit returns breakdown."""
    base = Path(__file__).parent.parent
    dup = base / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("fixture missing")
    config = load_config()
    config["_micro_mode"] = True
    config["_lean_mode"] = True
    config["_creature_adaptive"] = False
    config["_tesseract_planner"] = False
    config["_tesseract_cooperation"] = False
    config["package_export"] = {"compact": True, "report_text": False, "inventory_minimal": True}
    config["_fold_profile"] = "fox"
    archive = tmp_path / "micro.infold"
    create_archive(dup, archive, config, profile="fox")
    audit = audit_archive_overhead(archive)
    assert "breakdown" in audit
    assert "overhead_bytes" in audit
    assert "total_archive_bytes" in audit
    assert audit["total_archive_bytes"] > 0
    text = overhead_audit_to_text(audit)
    assert "Overhead" in text
    assert "Breakdown" in text
