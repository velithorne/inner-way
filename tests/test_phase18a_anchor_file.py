"""
Phase 18A: Anchor Files v0.1 tests.
"""
from pathlib import Path

import pytest

from infold.engine.anchor_file import (
    find_anchor_files,
    _is_anchor_filename,
)
from infold.intake import scan_project
from infold.archive import create_archive, validate_archive, explain_archive
from infold.archive.operations import explain_to_text


def test_anchor_filename_detection():
    """Canonical anchor filenames are detected."""
    assert _is_anchor_filename("package.json")
    assert _is_anchor_filename("pyproject.toml")
    assert _is_anchor_filename("README.md")
    assert not _is_anchor_filename("foo.json")
    assert not _is_anchor_filename("package.json.bak")


def test_find_anchors_empty():
    """Empty project has no anchors."""
    from infold.models.project_sheet import ProjectSheet
    sheet = ProjectSheet(Path("."))
    sheet.file_nodes = {}
    anchors = find_anchor_files(sheet, Path("."))
    assert anchors == []


def test_find_anchors_mixed_project():
    """mixed_project has package.json and README."""
    fixture = Path(__file__).parent / "fixtures" / "mixed_project"
    if not fixture.exists():
        pytest.skip("mixed_project fixture not found")
    sheet = scan_project(fixture)
    anchors = find_anchor_files(sheet, fixture)
    paths = [a["path"] for a in anchors]
    assert "package.json" in paths or "README.md" in paths
    for a in anchors:
        assert "path" in a
        assert "anchor_type" in a
        assert "anchored_count" in a
        assert a["anchored_count"] >= 0


def test_archive_with_anchors():
    """Archive creation includes anchor metadata when anchors exist."""
    fixture = Path(__file__).parent / "fixtures" / "mixed_project"
    if not fixture.exists():
        pytest.skip("mixed_project fixture not found")
    out = Path(__file__).parent.parent / "results" / "anchor_test.infold"
    out.parent.mkdir(parents=True, exist_ok=True)
    create_archive(str(fixture), str(out), profile="fox")
    valid, _ = validate_archive(str(out), mode="strict")
    assert valid
    info = explain_archive(str(out))
    assert "anchor" in str(info).lower() or "Anchor" in str(info)


def test_explain_shows_anchors():
    """Explain output includes Anchor Files section when present."""
    fixture = Path(__file__).parent / "fixtures" / "mixed_project"
    if not fixture.exists():
        pytest.skip("mixed_project fixture not found")
    out = Path(__file__).parent.parent / "results" / "anchor_explain.infold"
    out.parent.mkdir(parents=True, exist_ok=True)
    create_archive(str(fixture), str(out), profile="fox")
    info = explain_archive(str(out))
    text = explain_to_text(info)
    assert "Archive Explain" in text
    if info.get("anchor_families"):
        assert "Anchor" in text
