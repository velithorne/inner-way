"""Tests for Metadata Table Fold (Phase 5D)."""

from pathlib import Path

import pytest

from infold.archive import create_archive, validate_archive, reconstruct_archive
from infold.engine.metadata_table_fold import (
    apply_metadata_table_fold,
    load_path_table,
    _collect_paths_from_package,
    _build_path_table,
)
from infold.cli import load_config


def test_metadata_table_fold_collects_paths(tmp_path):
    """_collect_paths_from_package collects path strings from maps and snapshots."""
    (tmp_path / "maps").mkdir(parents=True)
    (tmp_path / "snapshots").mkdir(parents=True)
    (tmp_path / "maps" / "reconstruction.json").write_text(
        '{"records":[{"targets":["a/b.py","c/d.py"]}]}',
        encoding="utf-8",
    )
    (tmp_path / "snapshots" / "passthrough.json").write_text(
        '{"x/y.txt":"content"}',
        encoding="utf-8",
    )
    paths = _collect_paths_from_package(tmp_path)
    assert "a/b.py" in paths
    assert "c/d.py" in paths
    assert "x/y.txt" in paths


def test_build_path_table_deterministic():
    """_build_path_table produces deterministic ordered list."""
    paths = {"z/a.py", "a/b.py", "m/n.py"}
    table = _build_path_table(paths)
    assert table == ["a/b.py", "m/n.py", "z/a.py"]


def test_load_path_table_missing_returns_none(tmp_path):
    """load_path_table returns None when path_table.json does not exist."""
    assert load_path_table(tmp_path) is None


def test_metadata_table_fold_creates_path_table(tmp_path):
    """apply_metadata_table_fold creates path_table when gain is positive."""
    (tmp_path / "maps").mkdir(parents=True)
    (tmp_path / "snapshots").mkdir(parents=True)
    (tmp_path / "shared").mkdir(parents=True)
    (tmp_path / "reports").mkdir(parents=True)
    (tmp_path / "manifest.json").write_text('{"fold_count":1}', encoding="utf-8")
    (tmp_path / "maps" / "reconstruction.json").write_text(
        '{"records":[{"index":0,"operator_id":"exact_repetition","targets":["a/b.py","a/b.py","c/d.py"],"gain":0}]}',
        encoding="utf-8",
    )
    (tmp_path / "snapshots" / "passthrough.json").write_text(
        '{"a/b.py":"x","c/d.py":"y"}',
        encoding="utf-8",
    )
    (tmp_path / "snapshots" / "inventory.json").write_text(
        '{"files":[{"path":"a/b.py","size":1},{"path":"c/d.py","size":1}]}',
        encoding="utf-8",
    )
    config = load_config()
    config["thresholds"] = config.get("thresholds", {})
    config["thresholds"]["metadata_table_fold"] = {"min_net_gain_bytes": 0}
    result = apply_metadata_table_fold(tmp_path, config)
    assert result is not None
    assert result["metadata_table_unique_paths"] >= 2
    assert result["metadata_table_net_bytes_saved"] >= 0
    pt_path = tmp_path / "shared" / "metadata_tables" / "path_table.json"
    assert pt_path.exists()
    pt = load_path_table(tmp_path)
    assert pt is not None
    assert "a/b.py" in pt
    assert "c/d.py" in pt


def test_metadata_table_fold_skips_low_gain(tmp_path):
    """apply_metadata_table_fold returns None when net gain is below threshold."""
    (tmp_path / "maps").mkdir(parents=True)
    (tmp_path / "snapshots").mkdir(parents=True)
    (tmp_path / "shared").mkdir(parents=True)
    (tmp_path / "reports").mkdir(parents=True)
    (tmp_path / "maps" / "reconstruction.json").write_text(
        '{"records":[{"targets":["a"]}]}',
        encoding="utf-8",
    )
    (tmp_path / "snapshots" / "passthrough.json").write_text('{}', encoding="utf-8")
    config = load_config()
    config["thresholds"] = config.get("thresholds", {})
    config["thresholds"]["metadata_table_fold"] = {"min_net_gain_bytes": 99999}
    result = apply_metadata_table_fold(tmp_path, config)
    assert result is None
    pt_path = tmp_path / "shared" / "metadata_tables" / "path_table.json"
    assert not pt_path.exists()


def test_archive_with_metadata_table_fold_reconstructs(tmp_path):
    """Archive created with metadata table fold reconstructs exactly."""
    config = load_config()
    dup = Path(__file__).parent.parent / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("duplicate_python fixture not found")
    archive_path = tmp_path / "test.infold"
    create_archive(dup, archive_path, config)
    ok, errors = validate_archive(archive_path, mode="strict")
    assert ok, errors
    out = tmp_path / "restored"
    reconstruct_archive(archive_path, out)
    import filecmp
    assert filecmp.dircmp(dup, out).diff_files == []
    assert filecmp.dircmp(dup, out).left_only == []
    assert filecmp.dircmp(dup, out).right_only == []


def test_archive_validate_strict_with_metadata_table_fold(tmp_path):
    """Strict validation passes for archive with metadata table fold."""
    config = load_config()
    dup = Path(__file__).parent.parent / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("duplicate_python fixture not found")
    archive_path = tmp_path / "test.infold"
    create_archive(dup, archive_path, config)
    ok, errors = validate_archive(archive_path, mode="strict")
    assert ok, errors


def test_metadata_table_fold_disabled(tmp_path):
    """When metadata_table_fold is disabled, no path_table is created."""
    config = load_config()
    config["operators"] = config.get("operators", {})
    config["operators"]["metadata_table_fold"] = {"enabled": False}
    dup = Path(__file__).parent.parent / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("duplicate_python fixture not found")
    from infold.engine import run_fold
    from infold.engine import export_package
    import tempfile
    result = run_fold(dup, config)
    pkg_dir = Path(tempfile.mkdtemp(prefix="infold_mtf_"))
    try:
        export_package(result, config, pkg_dir)
        apply_metadata_table_fold(pkg_dir, config)
        pt_path = pkg_dir / "shared" / "metadata_tables" / "path_table.json"
        assert not pt_path.exists()
    finally:
        import shutil
        if pkg_dir.exists():
            shutil.rmtree(pkg_dir)
