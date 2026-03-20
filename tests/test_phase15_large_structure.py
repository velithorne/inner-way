"""
Phase 15: Large Structure-Heavy Dominance tests.

Tests for dragon profile improvements, metadata scale efficiency, and large-project audit.
"""

from pathlib import Path

import pytest

from infold.archive import create_archive, validate_archive, reconstruct_archive
from infold.profiles.definitions import get_profile_config


def test_dragon_profile_has_min_lines_four():
    """Dragon profile uses min_lines 4 for template (Phase 15)."""
    cfg = get_profile_config("dragon")
    ts = cfg.get("thresholds", {}).get("template_skeleton", {})
    assert ts.get("min_lines") == 4
    assert ts.get("min_scaffold_similarity") == 0.78
    assert ts.get("max_slot_ratio") == 0.38


def test_dragon_profile_has_hierarchy_similarity():
    """Dragon profile uses min_structure_similarity 0.82 for hierarchy (Phase 15)."""
    cfg = get_profile_config("dragon")
    hm = cfg.get("thresholds", {}).get("hierarchy_mirror", {})
    assert hm.get("min_structure_similarity") == 0.82


def test_archive_with_dragon_profile_reconstructs(tmp_path):
    """Archive created with dragon profile reconstructs exactly."""
    base = Path(__file__).parent.parent
    dup = base / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("fixture missing")
    archive = tmp_path / "dragon.infold"
    create_archive(dup, archive, {}, profile="dragon")
    ok, _ = validate_archive(archive, mode="strict")
    assert ok
    restored = tmp_path / "restored"
    reconstruct_archive(archive, restored)
    for f in dup.rglob("*"):
        if f.is_file():
            rel = f.relative_to(dup)
            rest = restored / rel
            assert rest.exists(), f"Missing {rel}"
            assert rest.read_bytes() == f.read_bytes(), f"Mismatch {rel}"


def test_large_project_audit_runs():
    """Large-project audit runs without error on benchmark datasets."""
    from infold.benchmark.large_project_audit import audit_large_project_opportunities

    base = Path(__file__).parent.parent
    result = audit_large_project_opportunities(base)
    assert "summary" in result
    assert "audit_results" in result
    assert "top_datasets_by_gain" in result.get("summary", {})


def test_large_project_comparison_runs():
    """Large-project comparison runs and produces output."""
    from infold.benchmark.large_project_audit import run_large_project_comparison

    base = Path(__file__).parent.parent
    result = run_large_project_comparison(base, output_dir=None)
    assert "matrix" in result
    assert "summary" in result
    assert "infold_wins" in result
