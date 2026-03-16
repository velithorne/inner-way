"""Infold Archive v0.1 tests."""

import tempfile
from pathlib import Path

import pytest

import sys
sys.path.insert(0, str(Path(__file__).parent.parent))

from infold.archive import create_archive, inspect_archive, validate_archive, reconstruct_archive


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
