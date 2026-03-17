"""Tests for archive search (Infold Search v0.1)."""

import json
import tempfile
from pathlib import Path

import pytest
import sys

sys.path.insert(0, str(Path(__file__).parent.parent))

from infold.archive import search_archive, create_archive
from infold.cli import load_config


@pytest.fixture
def sample_archive(tmp_path):
    """Create a sample archive for search tests."""
    config = load_config()
    dup = Path(__file__).parent.parent / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("duplicate_python fixture not found")
    archive_path = tmp_path / "sample.infold"
    create_archive(dup, archive_path, config)
    return archive_path


@pytest.fixture
def template_archive(tmp_path):
    """Create template-heavy archive."""
    config = load_config()
    tmpl = Path(__file__).parent.parent / "tests" / "fixtures" / "template_heavy"
    if not tmpl.exists():
        pytest.skip("template_heavy fixture not found")
    archive_path = tmp_path / "template.infold"
    create_archive(tmpl, archive_path, config)
    return archive_path


def test_search_by_operator(sample_archive):
    """Search by operator type."""
    r = search_archive(sample_archive, operator="exact_repetition")
    assert r["match_count"] >= 1
    assert all(m["operator_id"] == "exact_repetition" for m in r["matches"])


def test_search_by_operator_empty(sample_archive):
    """Search by non-matching operator returns empty."""
    r = search_archive(sample_archive, operator="template_skeleton")
    assert r["match_count"] == 0
    assert r["matches"] == []


def test_search_by_path(sample_archive):
    """Search by path (contains match)."""
    r = search_archive(sample_archive, path="dup")
    assert r["match_count"] >= 1
    for m in r["matches"]:
        paths = m.get("paths", []) + m.get("roots_or_paths", [])
        assert any("dup" in p.lower() for p in paths)


def test_search_by_path_no_match(sample_archive):
    """Search by path with no match returns empty."""
    r = search_archive(sample_archive, path="nonexistent_xyz_123")
    assert r["match_count"] == 0


def test_search_by_family(sample_archive):
    """Search by family type (duplicate -> exact_repetition)."""
    r = search_archive(sample_archive, family="duplicate")
    assert r["match_count"] >= 1
    assert all(m["operator_id"] == "exact_repetition" for m in r["matches"])


def test_search_by_family_template(template_archive):
    """Search by family type template."""
    r = search_archive(template_archive, family="template")
    assert r["match_count"] >= 1
    assert all(m["operator_id"] == "template_skeleton" for m in r["matches"])


def test_search_json_output(sample_archive):
    """Search result is valid JSON-serializable."""
    r = search_archive(sample_archive)
    s = json.dumps(r)
    parsed = json.loads(s)
    assert "match_count" in parsed
    assert "matches" in parsed
    assert isinstance(parsed["matches"], list)


def test_search_empty_result_structure(sample_archive):
    """Empty result has correct structure."""
    r = search_archive(sample_archive, operator="dependency_motif")
    assert r["match_count"] == 0
    assert r["matches"] == []
    assert "query" in r
    assert "path" in r


def test_search_by_artifact_id(sample_archive):
    """Search by artifact_id (index)."""
    r_all = search_archive(sample_archive)
    if r_all["match_count"] == 0:
        pytest.skip("no folds in archive")
    idx = r_all["matches"][0]["index"]
    r = search_archive(sample_archive, artifact_id=idx)
    assert r["match_count"] == 1
    assert r["matches"][0]["index"] == idx
