"""Infold Archive v0.1 tests."""

import tempfile
from pathlib import Path

import pytest

import sys
sys.path.insert(0, str(Path(__file__).parent.parent))

from infold.archive import create_archive, inspect_archive, validate_archive, reconstruct_archive
from infold.archive import explain_archive, compare_archives


def test_archive_create_inspect_validate_reconstruct():
    """Create archive, inspect, validate, reconstruct."""
    fixtures = Path(__file__).parent / "fixtures" / "duplicate_python"
    if not fixtures.exists():
        pytest.skip("fixtures not found")
    with tempfile.TemporaryDirectory() as tmp:
        archive = Path(tmp) / "test.infold"
        create_archive(fixtures, archive)
        assert archive.exists()
        info = inspect_archive(archive)
        assert info["fold_count"] >= 1
        assert info["file_count"] >= 1
        ok, errors = validate_archive(archive)
        assert ok, errors
        out_dir = Path(tmp) / "restored"
        result = reconstruct_archive(archive, out_dir)
        assert len(result) >= 1
        assert (out_dir / "dup_a.py").exists()


def test_archive_explain():
    """Explain returns package summary, fold counts, gain contributors, guarantees."""
    fixtures = Path(__file__).parent / "fixtures" / "duplicate_python"
    if not fixtures.exists():
        pytest.skip("fixtures not found")
    with tempfile.TemporaryDirectory() as tmp:
        archive = Path(tmp) / "test.infold"
        create_archive(fixtures, archive)
        info = explain_archive(archive)
        assert "package_summary" in info
        assert "fold_counts_by_operator" in info
        assert "biggest_gain_contributors" in info
        assert "reconstruction_guarantees" in info
        assert info["package_summary"]["fold_count"] >= 1


def test_archive_compare():
    """Compare two archives returns diff summary."""
    fixtures = Path(__file__).parent / "fixtures" / "duplicate_python"
    if not fixtures.exists():
        pytest.skip("fixtures not found")
    with tempfile.TemporaryDirectory() as tmp:
        a = Path(tmp) / "a.infold"
        b = Path(tmp) / "b.infold"
        create_archive(fixtures, a)
        create_archive(fixtures, b)
        diff = compare_archives(a, b)
        assert "logical_gain" in diff
        assert "physical_folded_size" in diff
        assert diff["logical_gain"]["diff"] == 0
        assert diff["physical_folded_size"]["diff"] == 0
