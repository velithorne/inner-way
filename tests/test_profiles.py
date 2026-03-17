"""Tests for Adaptive Origami Profiles v0.1."""

from pathlib import Path

import pytest

from infold.profiles.definitions import VALID_PROFILES, get_profile_config, apply_profile
from infold.profiles.selector import select_profile_auto, _compute_metrics
from infold.archive import create_archive, validate_archive, reconstruct_archive
from infold.intake import scan_project
from infold.parsers import parse_project
from infold.cli import load_config


def test_valid_profiles():
    """Valid profiles are defined."""
    assert "sparrow" in VALID_PROFILES
    assert "fox" in VALID_PROFILES
    assert "dragon" in VALID_PROFILES
    assert "golem" in VALID_PROFILES
    assert "serpent" in VALID_PROFILES


def test_invalid_profile_raises():
    """Invalid profile name raises ValueError."""
    with pytest.raises(ValueError, match="Invalid profile"):
        get_profile_config("invalid")


def test_fox_profile_empty_overrides():
    """Fox profile has no overrides (baseline)."""
    assert get_profile_config("fox") == {}


def test_sparrow_profile_has_compact():
    """Sparrow profile sets compact package export."""
    cfg = get_profile_config("sparrow")
    assert cfg.get("package_export", {}).get("compact") is True


def test_apply_profile_merges():
    """apply_profile merges overrides into config."""
    config = {"planner": {"min_net_value": -0.5}, "x": 1}
    out = apply_profile(config, "sparrow")
    assert out["planner"]["min_net_value"] == 0.5
    assert out["x"] == 1


def test_auto_select_sparrow_tiny(tmp_path):
    """Auto selects sparrow for tiny archive."""
    (tmp_path / "a.py").write_text("x" * 100)
    (tmp_path / "b.py").write_text("y" * 100)
    sheet = scan_project(tmp_path)
    parse_project(sheet)
    name, reason, factors = select_profile_auto(sheet, {}, tmp_path)
    assert name == "sparrow"
    assert reason == "tiny_archive"


def test_auto_select_fox_balanced():
    """Auto selects fox for balanced project."""
    dup = Path(__file__).parent.parent / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("duplicate_python fixture not found")
    sheet = scan_project(dup)
    parse_project(sheet)
    name, reason, factors = select_profile_auto(sheet, {}, dup)
    assert name == "fox"
    assert reason == "balanced"


def test_manual_profile_in_manifest(tmp_path):
    """Manual profile is recorded in manifest."""
    config = load_config()
    dup = Path(__file__).parent.parent / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("duplicate_python fixture not found")
    archive_path = tmp_path / "test.infold"
    create_archive(dup, archive_path, config, profile="sparrow")
    import zipfile
    import json
    with zipfile.ZipFile(archive_path, "r") as zf:
        manifest = json.loads(zf.read("manifest.json").decode())
    assert manifest.get("fold_profile") == "sparrow"
    assert manifest.get("fold_profile_mode") == "manual"
    assert manifest.get("fold_profile_reason") == "user_selected"


def test_auto_profile_in_manifest(tmp_path):
    """Auto-selected profile is recorded in manifest."""
    config = load_config()
    dup = Path(__file__).parent.parent / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("duplicate_python fixture not found")
    archive_path = tmp_path / "test.infold"
    create_archive(dup, archive_path, config, profile="auto")
    import zipfile
    import json
    with zipfile.ZipFile(archive_path, "r") as zf:
        manifest = json.loads(zf.read("manifest.json").decode())
    assert manifest.get("fold_profile") in VALID_PROFILES
    assert manifest.get("fold_profile_mode") == "auto"
    assert manifest.get("fold_profile_reason")


def test_profile_exact_reconstruction(tmp_path):
    """Profile does not break exact reconstruction."""
    config = load_config()
    dup = Path(__file__).parent.parent / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("duplicate_python fixture not found")
    archive_path = tmp_path / "test.infold"
    create_archive(dup, archive_path, config, profile="dragon")
    ok, _ = validate_archive(archive_path, mode="strict")
    assert ok
    out = tmp_path / "restored"
    reconstruct_archive(archive_path, out)
    import filecmp
    assert filecmp.dircmp(dup, out).diff_files == []


def test_compute_metrics():
    """_compute_metrics returns deterministic dict."""
    from infold.models.project_sheet import ProjectSheet
    sheet = ProjectSheet(source_path=Path("."))
    sheet.file_nodes = {}
    sheet.metrics = {"original_size_bytes": 0, "file_count": 0, "folder_count": 0}
    m = _compute_metrics(sheet)
    assert "raw_size" in m
    assert "file_count" in m
    assert "structured_ratio" in m
