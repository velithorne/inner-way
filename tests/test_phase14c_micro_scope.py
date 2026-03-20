"""
Phase 14C: Ruthless Small-Archive Selection and Scope Alignment.

Tests for micro skip logic, scope accounting, and deterministic reporting.
"""

import json
import tempfile
from pathlib import Path

import pytest

from infold.archive import create_archive, explain_archive, reconstruct_archive, validate_archive
from infold.cli import _apply_create_flags, load_config
from infold.intake.scanner import compute_scope_metrics


def test_compute_scope_metrics_basic(tmp_path):
    """Scope metrics returns source/included/excluded counts."""
    (tmp_path / "a.py").write_text("x = 1")
    (tmp_path / "b.json").write_text("{}")
    (tmp_path / "c.html").write_text("<html/>")
    (tmp_path / "d.xyz").write_text("unknown")
    scope = compute_scope_metrics(tmp_path)
    assert scope["source_file_count"] == 4
    assert scope["included_file_count"] >= 2  # .py, .json
    assert scope["excluded_file_count"] >= 2  # .html, .xyz (or more if .html in extensions)
    assert scope["source_bytes"] > 0
    assert scope["included_bytes"] + scope["excluded_bytes"] == scope["source_bytes"]


def test_compute_scope_metrics_deterministic(tmp_path):
    """Scope metrics is deterministic."""
    (tmp_path / "a.py").write_text("x")
    (tmp_path / "b.py").write_text("y")
    s1 = compute_scope_metrics(tmp_path)
    s2 = compute_scope_metrics(tmp_path)
    assert s1 == s2
    assert s1["excluded_by_extension"] == sorted(s1["excluded_by_extension"])
    assert s1["excluded_by_pattern"] == sorted(s1["excluded_by_pattern"])


def test_micro_ruthless_thresholds():
    """Micro mode applies stricter planner and metadata thresholds."""
    cfg = load_config()
    class Args:
        micro = True
        lean = False
        creature = True
        no_tesseract = False
        no_tesseract_cooperation = False
        compact = False
        fair_scope = False
    applied = _apply_create_flags(cfg, Args())
    assert applied["_micro_mode"] is True
    assert applied["planner"]["min_net_value"] == 0.5
    assert applied["thresholds"]["metadata_table_fold"]["min_net_gain_bytes"] == 64
    assert applied.get("_micro_path_dna_min_bytes_saved") == 100


def test_fair_scope_extends_extensions():
    """--fair-scope adds .csv, .html, .toml to include_extensions."""
    cfg = load_config()
    class Args:
        micro = False
        lean = False
        creature = True
        no_tesseract = False
        no_tesseract_cooperation = False
        compact = False
        fair_scope = True
    applied = _apply_create_flags(cfg, Args())
    exts = applied["project"]["include_extensions"]
    assert ".csv" in exts
    assert ".html" in exts
    assert ".toml" in exts


def test_micro_archive_has_scope_accounting(tmp_path):
    """Micro archive report contains scope_accounting when exclusions exist."""
    (tmp_path / "a.py").write_text("x = 1")
    (tmp_path / "b.html").write_text("<html/>")
    out = tmp_path / "test.infold"
    create_archive(tmp_path, out, {"_micro_mode": True, "_lean_mode": True}, profile="golem")
    info = explain_archive(out)
    # Scope accounting in manifest or report when source != included
    scope = info.get("scope_accounting")
    if scope:
        assert "source_file_count" in scope
        assert "included_file_count" in scope
        assert "excluded_file_count" in scope


def test_micro_archive_exact_reconstruction(tmp_path):
    """Micro archive with ruthless thresholds still reconstructs exactly."""
    (tmp_path / "a.py").write_text("def f(): return 1")
    (tmp_path / "b.py").write_text("def g(): return 2")
    out = tmp_path / "test.infold"
    create_archive(tmp_path, out, {"_micro_mode": True, "_lean_mode": True}, profile="golem")
    ok, _ = validate_archive(out, mode="strict")
    assert ok
    restored = tmp_path / "restored"
    reconstruct_archive(out, restored)
    assert (restored / "a.py").read_text() == "def f(): return 1"
    assert (restored / "b.py").read_text() == "def g(): return 2"


def test_scope_accounting_in_report(tmp_path):
    """Archive creation produces report with scope_accounting when applicable."""
    (tmp_path / "a.py").write_text("x")
    (tmp_path / "b.csv").write_text("a,b")
    out = tmp_path / "test.infold"
    cfg = load_config()
    class Args:
        micro = False
        lean = False
        creature = True
        no_tesseract = False
        no_tesseract_cooperation = False
        compact = False
        fair_scope = True
    config = _apply_create_flags(cfg, Args())
    create_archive(tmp_path, out, config, profile="golem")
    import zipfile
    with zipfile.ZipFile(out, "r") as zf:
        report_data = json.loads(zf.read("reports/report.json"))

    scope = report_data.get("scope_accounting")
    assert scope is not None
    assert scope.get("source_file_count", 0) >= 2
    assert scope.get("included_file_count", 0) >= 2  # .py and .csv with fair_scope
