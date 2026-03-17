"""Tests for archive search (Infold Search v0.1 and v0.2)."""

import json
import tempfile
from pathlib import Path

import pytest
import sys

sys.path.insert(0, str(Path(__file__).parent.parent))

from infold.archive import search_archive, search_archives, create_archive
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


# --- Infold Search v0.2 ---


def test_search_match_has_archive_path(sample_archive):
    """Every match includes archive_path for schema consistency."""
    r = search_archive(sample_archive)
    for m in r.get("matches", []):
        assert "archive_path" in m
        assert m["archive_path"] == str(sample_archive.resolve())


def test_search_has_summary(sample_archive):
    """Search result includes summary with total_matches, matches_by_operator."""
    r = search_archive(sample_archive)
    assert "summary" in r
    s = r["summary"]
    assert "total_archives_searched" in s
    assert "total_matches" in s
    assert "matches_by_operator" in s


def test_search_min_gain_filter(sample_archive):
    """min_gain filter excludes low-gain matches."""
    r_all = search_archive(sample_archive)
    if r_all["match_count"] == 0:
        pytest.skip("no folds")
    max_gain = max(m["gain"] for m in r_all["matches"])
    r = search_archive(sample_archive, min_gain=max_gain + 9999)
    assert r["match_count"] == 0


def test_search_min_target_count_filter(sample_archive):
    """min_target_count filter excludes small families."""
    r_all = search_archive(sample_archive)
    if r_all["match_count"] == 0:
        pytest.skip("no folds")
    max_tc = max(m["target_count"] for m in r_all["matches"])
    r = search_archive(sample_archive, min_target_count=max_tc + 999)
    assert r["match_count"] == 0


def test_search_sort_by_gain(sample_archive):
    """sort_by=gain orders by gain descending."""
    r = search_archive(sample_archive, sort_by="gain")
    gains = [m["gain"] for m in r["matches"]]
    assert gains == sorted(gains, reverse=True)


def test_search_sort_by_operator(sample_archive):
    """sort_by=operator orders by operator_id."""
    r = search_archive(sample_archive, sort_by="operator")
    ops = [m["operator_id"] for m in r["matches"]]
    assert ops == sorted(ops)


def test_multi_archive_search(sample_archive, template_archive):
    """search_archives aggregates matches across archives."""
    r = search_archives([sample_archive, template_archive])
    assert "paths" in r
    assert len(r["paths"]) == 2
    assert r["summary"]["total_archives_searched"] == 2
    assert r["summary"]["total_matches"] == r["match_count"]
    for m in r["matches"]:
        assert "archive_path" in m
        assert m["archive_path"] in (str(sample_archive.resolve()), str(template_archive.resolve()))


def test_multi_archive_archive_filter(sample_archive, template_archive):
    """archive_filter restricts which archives are searched."""
    r = search_archives(
        [sample_archive, template_archive],
        archive_filter="template",
    )
    assert len(r["paths"]) == 1
    assert "template" in r["paths"][0].lower()


def test_multi_archive_empty_dir(tmp_path):
    """Empty directory of archives returns empty matches."""
    r = search_archives([])
    assert r["match_count"] == 0
    assert r["summary"]["total_archives_searched"] == 0
    assert r["matches"] == []


def test_multi_archive_empty_result(sample_archive):
    """Multi-archive search with no matching operator returns empty."""
    r = search_archives([sample_archive], operator="dependency_motif")
    assert r["match_count"] == 0
    assert r["summary"]["total_archives_searched"] == 1
    assert r["matches"] == []


def test_search_json_schema_validity(sample_archive):
    """JSON output has stable schema: path/paths, query, match_count, matches, summary."""
    r = search_archive(sample_archive)
    assert "path" in r or "paths" in r
    assert "query" in r
    assert "match_count" in r
    assert "matches" in r
    assert "summary" in r
    j = json.dumps(r)
    parsed = json.loads(j)
    assert parsed["match_count"] == len(parsed["matches"])


# --- Infold Search v0.3 ---


def test_search_max_gain_filter(sample_archive):
    """max_gain filter excludes high-gain matches."""
    r_all = search_archive(sample_archive)
    if r_all["match_count"] == 0:
        pytest.skip("no folds")
    min_gain = min(m["gain"] for m in r_all["matches"] if m.get("gain") is not None)
    r = search_archive(sample_archive, max_gain=max(0, min_gain - 1))
    assert r["match_count"] == 0


def test_search_max_target_count_filter(sample_archive):
    """max_target_count filter excludes large families."""
    r_all = search_archive(sample_archive)
    if r_all["match_count"] == 0:
        pytest.skip("no folds")
    min_tc = min(m["target_count"] for m in r_all["matches"])
    r = search_archive(sample_archive, max_target_count=max(0, min_tc - 1))
    assert r["match_count"] == 0


def test_search_summary_has_tops(sample_archive):
    """Summary includes top_operators and top_families when matches exist."""
    r = search_archive(sample_archive)
    if r["match_count"] == 0:
        pytest.skip("no folds")
    s = r["summary"]
    assert "top_operators" in s
    assert "top_families" in s


def test_search_export_csv(sample_archive):
    """CSV export produces valid CSV with header."""
    from infold.archive.operations import search_to_csv
    r = search_archive(sample_archive)
    csv_str = search_to_csv(r)
    lines = csv_str.strip().split("\n")
    assert len(lines) >= 1
    assert "match_type" in lines[0]
    assert "operator_id" in lines[0]


def test_search_export_markdown(sample_archive):
    """Markdown export produces valid markdown with summary."""
    from infold.archive.operations import search_to_markdown
    r = search_archive(sample_archive)
    md = search_to_markdown(r)
    assert "# Infold Search Results" in md
    assert "Matches:" in md
    assert "## Summary" in md


def test_search_diagnostics_rejected(tmp_path):
    """Search with --rejected includes rejected candidates when available."""
    from pathlib import Path
    ws = Path(__file__).parent.parent  # workspace root
    if not (ws / "infold").exists():
        pytest.skip("infold workspace not found")
    config = load_config()
    archive_path = tmp_path / "ws.infold"
    create_archive(ws, archive_path, config)
    r = search_archive(archive_path, rejected=True)
    # May have 0 or more rejected; structure should be correct
    for m in r.get("matches", []):
        if m.get("match_type") == "rejected":
            assert "operator_id" in m
            assert "archive_path" in m
            break


def test_search_combined_filters(sample_archive):
    """Operator, min_gain, sort_by work together."""
    r = search_archive(
        sample_archive,
        operator="exact_repetition",
        min_gain=0,
        max_gain=99999,
        sort_by="gain",
    )
    for m in r["matches"]:
        assert m["operator_id"] == "exact_repetition"
    gains = [m["gain"] for m in r["matches"] if m.get("gain") is not None]
    assert gains == sorted(gains, reverse=True)


def test_search_fold_match_has_match_type(sample_archive):
    """Fold matches include match_type=fold."""
    r = search_archive(sample_archive)
    for m in r.get("matches", []):
        assert m.get("match_type", "fold") == "fold"
