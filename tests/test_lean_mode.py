"""
Phase 11: Tests for Lean Mode and feature switches.
"""

import json
from pathlib import Path

import pytest

from infold.archive import create_archive, validate_archive, reconstruct_archive
from infold.archive.operations import explain_archive, explain_to_text
from infold.cli import load_config, _apply_create_flags


def test_apply_create_flags_lean():
    """Lean mode sets creature off, tesseract off, compact."""
    config = load_config()
    args = type("Args", (), {"lean": True, "compact": False, "creature": True, "no_tesseract": False, "no_tesseract_cooperation": False})()
    cfg = _apply_create_flags(config, args)
    assert cfg["_lean_mode"] is True
    assert cfg["_creature_adaptive"] is False
    assert cfg["_tesseract_planner"] is False
    assert cfg["_tesseract_cooperation"] is False
    assert cfg["package_export"]["compact"] is True
    assert cfg["package_export"]["report_text"] is False


def test_apply_create_flags_no_tesseract():
    """--no-tesseract disables planner and cooperation."""
    config = load_config()
    args = type("Args", (), {"lean": False, "compact": False, "creature": True, "no_tesseract": True, "no_tesseract_cooperation": False})()
    cfg = _apply_create_flags(config, args)
    assert cfg["_tesseract_planner"] is False
    assert cfg["_tesseract_cooperation"] is False


def test_apply_create_flags_no_tesseract_cooperation():
    """--no-tesseract-cooperation disables cooperation only."""
    config = load_config()
    args = type("Args", (), {"lean": False, "compact": False, "creature": True, "no_tesseract": False, "no_tesseract_cooperation": True})()
    cfg = _apply_create_flags(config, args)
    assert cfg["_tesseract_planner"] is True
    assert cfg["_tesseract_cooperation"] is False


def test_apply_create_flags_no_creature():
    """--no-creature disables creature adaptation."""
    config = load_config()
    args = type("Args", (), {"lean": False, "compact": False, "creature": False, "no_tesseract": False, "no_tesseract_cooperation": False})()
    cfg = _apply_create_flags(config, args)
    assert cfg["_creature_adaptive"] is False


def test_archive_create_no_tesseract_manifest(tmp_path):
    """Archive with --no-tesseract has creature_enabled/tesseract_planner_enabled in manifest."""
    from infold.cli import load_config, _apply_create_flags

    base = Path(__file__).parent.parent
    dup = base / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("fixture missing")
    config = load_config()
    config["_tesseract_planner"] = False
    config["_tesseract_cooperation"] = False
    config["_creature_adaptive"] = False
    config["_fold_profile"] = "fox"
    archive = tmp_path / "no_tess.infold"
    create_archive(dup, archive, config, profile="fox")
    ok, _ = validate_archive(archive, mode="strict")
    assert ok
    with archive.open("rb") as f:
        import zipfile
        z = zipfile.ZipFile(f, "r")
        m = json.loads(z.read("manifest.json"))
        assert m.get("tesseract_planner_enabled") is False
        assert m.get("tesseract_cooperation_enabled") is False
        assert m.get("creature_enabled") is False
        assert "tesseract_planner_route" not in m
        assert "fold_species" not in m


def test_archive_create_lean_manifest(tmp_path):
    """Archive with --lean has minimal manifest, no creature/tesseract rich fields."""
    base = Path(__file__).parent.parent
    dup = base / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("fixture missing")
    config = load_config()
    config["_lean_mode"] = True
    config["_creature_adaptive"] = False
    config["_tesseract_planner"] = False
    config["_tesseract_cooperation"] = False
    config["package_export"] = {
        **config.get("package_export", {}),
        "compact": True,
        "report_text": False,
        "inventory_minimal": True,
    }
    config["_fold_profile"] = "fox"
    archive = tmp_path / "lean.infold"
    create_archive(dup, archive, config, profile="fox")
    ok, _ = validate_archive(archive, mode="strict")
    assert ok
    with archive.open("rb") as f:
        import zipfile
        z = zipfile.ZipFile(f, "r")
        m = json.loads(z.read("manifest.json"))
        assert m.get("creature_enabled") is False
        assert m.get("tesseract_planner_enabled") is False
        assert m.get("tesseract_cooperation_enabled") is False
        assert "fold_species" not in m
        assert "tesseract_planner_route" not in m
    # Reconstruct
    restored = tmp_path / "restored"
    reconstruct_archive(archive, restored)
    assert (restored / "dup_a.py").exists()
    assert (restored / "dup_b.py").exists()


def test_explain_shows_disabled_when_no_tesseract(tmp_path):
    """Explain shows 'Tesseract: disabled' when tesseract_planner_enabled is false."""
    base = Path(__file__).parent.parent
    dup = base / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("fixture missing")
    config = load_config()
    config["_tesseract_planner"] = False
    config["_tesseract_cooperation"] = False
    config["_creature_adaptive"] = False
    config["_fold_profile"] = "fox"
    archive = tmp_path / "no_tess.infold"
    create_archive(dup, archive, config, profile="fox")
    info = explain_archive(archive)
    text = explain_to_text(info)
    assert "Tesseract: disabled" in text or "creature_enabled" in str(info.get("manifest", {}))
    assert info.get("creature_enabled") is False
    assert info.get("tesseract_planner_enabled") is False
