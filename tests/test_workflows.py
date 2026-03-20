"""Tests for Phase 8 workflow commands."""

from pathlib import Path

import pytest

from infold.workflows import (
    analyze_project,
    analyze_to_text,
    archive_workflow,
    sync_capture,
    real_project_showcase,
)
from infold.cli import load_config


def test_analyze_project():
    """analyze_project returns summary dict."""
    dup = Path(__file__).parent.parent / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("duplicate_python fixture not found")
    config = load_config()
    data = analyze_project(dup, config)
    assert "source_path" in data
    assert "fold_count" in data
    assert "fold_counts_by_operator" in data
    assert "fold_profile" in data
    assert "logical_gain_bytes" in data
    assert "physical_folded_size_bytes" in data


def test_analyze_to_text():
    """analyze_to_text produces readable output."""
    dup = Path(__file__).parent.parent / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("duplicate_python fixture not found")
    config = load_config()
    data = analyze_project(dup, config)
    text = analyze_to_text(data)
    assert "Project Analysis" in text
    assert "Fold counts by operator" in text
    assert "Profile:" in text


def test_archive_workflow(tmp_path):
    """archive_workflow creates, validates, summarizes."""
    dup = Path(__file__).parent.parent / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("duplicate_python fixture not found")
    out = tmp_path / "wf.infold"
    r = archive_workflow(dup, out)
    assert r["created"] is True
    assert r["validated"] is True
    assert r["validation_ok"] is True
    assert "summary" in r
    assert "Profile" in r["summary"] or "profile" in r["summary"].lower()


def test_sync_capture(tmp_path):
    """sync_capture adds snapshot and returns timeline."""
    from infold.sync import init_sync
    dup = Path(__file__).parent.parent / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("duplicate_python fixture not found")
    sync_dir = tmp_path / ".sync"
    init_sync(sync_dir, source_path=dup)
    r = sync_capture(sync_dir, dup)
    assert "snapshot" in r
    assert r["snapshot"].get("id") == "v1"
    assert "timeline_text" in r
    assert "Sync Timeline" in r["timeline_text"]


def test_real_project_showcase(tmp_path):
    """real_project_showcase saves results to output dir."""
    dup = Path(__file__).parent.parent / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("duplicate_python fixture not found")
    out_dir = tmp_path / "showcase"
    r = real_project_showcase(dup, out_dir)
    assert r["validation_ok"] is True
    assert r["exact_reconstruction_ok"] is True
    assert (out_dir / "archive.infold").exists()
    assert (out_dir / "restored").is_dir()
    assert (out_dir / "summary.md").exists()
    assert (out_dir / "summary.json").exists()
    assert (out_dir / "explain.txt").exists()
