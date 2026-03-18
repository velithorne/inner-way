"""
Phase 12: Tests for Competitive Benchmark Matrix.
"""

import json
from pathlib import Path

import pytest

from infold.benchmark.competitive_matrix import (
    get_matrix_category,
    _check_tool_available,
    _raw_size_path,
    TOOL_FEATURES,
    run_competitive_matrix,
    export_matrix_csv,
    export_matrix_json,
    export_matrix_markdown,
    build_competitive_summary,
    build_feature_matrix,
    export_competitive_summary_markdown,
    get_research_slots_status,
)


def test_get_matrix_category():
    """Matrix category mapping is deterministic."""
    assert get_matrix_category("template-heavy", "correctness") == "template_heavy"
    assert get_matrix_category("config-heavy", "correctness") == "config_heavy"
    assert get_matrix_category("byte-fold-opaque", "byte_fold") == "opaque_heavy"
    assert get_matrix_category("unknown", "stress") == "stress"


def test_check_tool_available():
    """Tool availability check."""
    zip_av = _check_tool_available("zip")
    gzip_av = _check_tool_available("gzip")
    zstd_av = _check_tool_available("zstd")
    assert isinstance(zip_av, bool)
    assert isinstance(gzip_av, bool)
    assert isinstance(zstd_av, bool)


def test_raw_size_path(tmp_path):
    """Raw size computation excludes patterns."""
    (tmp_path / "a.txt").write_text("hello")
    (tmp_path / "b.txt").write_text("world")
    (tmp_path / "__pycache__").mkdir()
    (tmp_path / "__pycache__" / "x.pyc").write_bytes(b"x")
    total = _raw_size_path(tmp_path, [])
    assert total > 0
    total_excl = _raw_size_path(tmp_path, ["__pycache__"])
    assert total_excl < total


def test_tool_features():
    """Feature matrix has expected tools."""
    assert "zip" in TOOL_FEATURES
    assert "gzip" in TOOL_FEATURES
    assert "infold_lean" in TOOL_FEATURES
    assert TOOL_FEATURES["zip"]["searchable"] is False
    assert TOOL_FEATURES["infold_default"]["searchable"] is True


def test_run_competitive_matrix_small(tmp_path):
    """Competitive matrix runs on small dataset set."""
    from infold.benchmark.pack import ensure_stress_datasets, get_benchmark_datasets

    base = Path(__file__).parent.parent
    ensure_stress_datasets(base)
    datasets = get_benchmark_datasets(base)[:2]
    if not datasets:
        pytest.skip("No benchmark datasets")
    config = {"project": {"exclude_patterns": [".git", "__pycache__", "*.pyc"]}}
    matrix = run_competitive_matrix(datasets, config, base)
    assert "matrix" in matrix
    assert "tools_available" in matrix
    assert "research_slots" in matrix
    assert len(matrix["matrix"]) > 0
    for r in matrix["matrix"]:
        assert "dataset_id" in r
        assert "tool" in r
        assert "compressed_size_bytes" in r or r.get("status") in ("not_available", "error")


def test_export_matrix_csv(tmp_path):
    """CSV export produces valid file."""
    matrix = {"matrix": [{"dataset_id": "x", "tool": "zip", "compressed_size_bytes": 100}], "tools_available": {}}
    out = tmp_path / "m.csv"
    export_matrix_csv(matrix, out)
    assert out.exists()
    assert "dataset_id" in out.read_text()


def test_export_matrix_json(tmp_path):
    """JSON export produces valid file."""
    matrix = {"matrix": [], "tools_available": {"zip": True}}
    out = tmp_path / "m.json"
    export_matrix_json(matrix, out)
    assert out.exists()
    assert json.loads(out.read_text())["tools_available"]["zip"] is True


def test_export_matrix_markdown(tmp_path):
    """Markdown export produces valid file."""
    matrix = {"matrix": [{"dataset_id": "x", "tool": "zip", "compressed_size_bytes": 100, "raw_bytes": 200}], "tools_available": {"zip": True}}
    out = tmp_path / "m.md"
    export_matrix_markdown(matrix, out)
    assert out.exists()
    assert "Competitive" in out.read_text()


def test_build_competitive_summary():
    """Summary builds from matrix."""
    matrix = {
        "matrix": [
            {"dataset_id": "a", "tool": "infold_lean", "compressed_size_bytes": 50, "matrix_category": "small"},
            {"dataset_id": "a", "tool": "gzip", "compressed_size_bytes": 100},
            {"dataset_id": "b", "tool": "gzip", "compressed_size_bytes": 80, "matrix_category": "medium"},
            {"dataset_id": "b", "tool": "infold_lean", "compressed_size_bytes": 120},
        ],
        "tools_available": {"zip": True, "gzip": True, "zstd": False},
    }
    summary = build_competitive_summary(matrix)
    assert "infold_wins_count" in summary
    assert "gzip_wins_count" in summary
    assert "best_infold_by_category" in summary


def test_build_feature_matrix():
    """Feature matrix builds."""
    matrix = {"matrix": []}
    feat_dict, feat_md = build_feature_matrix(matrix)
    assert "matrix" in feat_dict
    assert "Feature-Value" in feat_md
    assert "infold" in feat_md
    assert "zip" in feat_md


def test_export_competitive_summary_markdown():
    """Summary markdown exports."""
    summary = {"tools_available": {"zip": True}, "infold_wins_count": 1, "gzip_wins_count": 2, "best_infold_by_category": {"small": "infold_lean"}}
    md = export_competitive_summary_markdown(summary, {"matrix": []})
    assert "Competitive" in md
    assert "Infold wins" in md


def test_research_slots_status():
    """Research slots return future_comparison."""
    status = get_research_slots_status()
    assert "cmix" in status
    assert status.get("cmix") == "future_comparison"
