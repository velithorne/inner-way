"""
Phase 18A/18B: Anchor Files tests.
"""
from pathlib import Path

import pytest

from infold.engine.anchor_file import (
    find_anchor_files,
    _is_anchor_filename,
    build_path_to_anchor_scopes,
    paths_share_anchor,
    path_in_anchor_scope,
    anchor_relative_path,
    expand_anchor_relative_path,
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


def test_build_path_to_anchor_scopes():
    """Phase 18B: build_path_to_anchor_scopes maps paths to anchor ids."""
    anchors = [
        {"path": "package.json", "anchored_paths": ["src/a.py", "src/b.py"]},
        {"path": "other/README.md", "anchored_paths": ["other/x.py"]},
    ]
    scopes = build_path_to_anchor_scopes(anchors)
    assert "package.json" in scopes.get("src/a.py", set())
    assert "other/README.md" in scopes.get("other/x.py", set())
    assert "src/a.py" not in scopes.get("other/x.py", set())


def test_paths_share_anchor():
    """Phase 18B: paths_share_anchor returns common anchor or None."""
    scopes = {"a": {"pkg"}, "b": {"pkg"}, "c": {"other"}}
    assert paths_share_anchor(["a", "b"], scopes) == "pkg"
    assert paths_share_anchor(["a", "c"], scopes) is None


def test_anchor_relative_path_reversible():
    """Phase 18B: anchor_relative_path and expand_anchor_relative_path are reversible."""
    rel = anchor_relative_path("src/foo.py", Path("."))
    assert rel == "src/foo.py"
    expanded = expand_anchor_relative_path(rel, "package.json")
    assert "foo.py" in expanded


def test_path_in_anchor_scope():
    """Phase 18B: path_in_anchor_scope checks membership."""
    scopes = {"src/a.py": {"pkg"}, "src/b.py": {"pkg"}}
    assert path_in_anchor_scope("src/a.py", "pkg", scopes) is True
    assert path_in_anchor_scope("other/x.py", "pkg", scopes) is False
