"""Tests for Phase 10C Tesseract Cooperation / Orchestration v0.1."""

import pytest

from infold.tesseract.execution_plan import (
    generate_execution_plan,
    execution_plan_allows_metadata_followup,
    execution_plan_summary,
    COOPERATION_MODES,
)
from infold.tesseract.planner import compute_tesseract_planner_profile


def test_execution_plan_deterministic():
    """Execution plan generation is deterministic."""
    profile = compute_tesseract_planner_profile({
        "structured_ratio": 0.8,
        "opaque_ratio": 0.1,
        "metadata_overhead_pressure": 0.5,
        "file_count": 50,
        "lineage_context": False,
    })
    p1 = generate_execution_plan(profile)
    p2 = generate_execution_plan(profile)
    assert p1 == p2


def test_structural_then_metadata_plan():
    """structure + metadata secondary with m>=0.25 -> structural_then_metadata."""
    profile = compute_tesseract_planner_profile({
        "structured_ratio": 0.9,
        "opaque_ratio": 0.05,
        "metadata_overhead_pressure": 0.6,
        "file_count": 80,
        "parser_confidence_mean": 0.9,
        "duplicate_density": 0.3,
        "template_density": 0.3,
        "lineage_context": False,
    })
    plan = generate_execution_plan(profile)
    assert plan["dominant_dimension"] == "structure"
    assert plan["secondary_dimension"] == "metadata"
    assert plan["cooperation_mode"] == "structural_then_metadata"
    assert "structural" in plan["execution_steps"]
    assert "metadata" in plan["execution_steps"]
    assert execution_plan_allows_metadata_followup(plan)


def test_byte_then_metadata_plan():
    """byte + metadata secondary with m>=0.25 -> byte_then_metadata."""
    profile = compute_tesseract_planner_profile({
        "structured_ratio": 0.2,
        "opaque_ratio": 0.8,
        "chunk_reuse_potential": 0.7,
        "metadata_overhead_pressure": 0.5,
        "file_count": 60,
        "parser_confidence_mean": 0.3,
        "lineage_context": False,
    })
    plan = generate_execution_plan(profile)
    assert plan["dominant_dimension"] == "byte"
    assert plan["secondary_dimension"] == "metadata"
    assert plan["cooperation_mode"] == "byte_then_metadata"
    assert "byte" in plan["execution_steps"]
    assert "metadata" in plan["execution_steps"]
    assert execution_plan_allows_metadata_followup(plan)


def test_primary_only_fallback():
    """structure + byte secondary -> primary_only, no chain."""
    profile = compute_tesseract_planner_profile({
        "structured_ratio": 0.7,
        "opaque_ratio": 0.5,
        "metadata_overhead_pressure": 0.1,
        "file_count": 20,
        "lineage_context": False,
    })
    plan = generate_execution_plan(profile)
    assert plan["cooperation_mode"] == "primary_only"
    assert not execution_plan_allows_metadata_followup(plan)
    assert len(plan["execution_steps"]) <= 2


def test_time_dominant_conservative():
    """time dominant -> primary_only, conservative."""
    profile = compute_tesseract_planner_profile({
        "structured_ratio": 0.5,
        "opaque_ratio": 0.3,
        "file_count": 10,
        "lineage_context": True,
    })
    plan = generate_execution_plan(profile)
    assert plan["cooperation_mode"] == "primary_only"
    assert plan["plan_reason"] == "time_dominant_conservative"


def test_execution_plan_summary():
    """Summary is human-readable."""
    profile = compute_tesseract_planner_profile({
        "structured_ratio": 0.8,
        "metadata_overhead_pressure": 0.5,
        "file_count": 50,
        "lineage_context": False,
    })
    plan = generate_execution_plan(profile)
    s = execution_plan_summary(plan)
    assert "steps=" in s
    assert "mode=" in s
    assert "reason=" in s


def test_cooperation_mode_valid():
    """Cooperation mode is one of allowed values."""
    for _ in range(5):
        profile = compute_tesseract_planner_profile({
            "structured_ratio": 0.5,
            "opaque_ratio": 0.3,
            "metadata_overhead_pressure": 0.3,
            "file_count": 30,
            "lineage_context": False,
        })
        plan = generate_execution_plan(profile)
        assert plan["cooperation_mode"] in COOPERATION_MODES


def test_explain_has_execution_plan(tmp_path):
    """Archive explain includes execution plan."""
    from infold.archive.operations import create_archive, explain_archive, explain_to_text
    from infold.cli import load_config

    src = tmp_path / "src"
    src.mkdir()
    (src / "a.py").write_text("x = 1\n")
    (src / "b.py").write_text("x = 1\n")
    cfg = load_config()
    archive = tmp_path / "out.infold"
    create_archive(src, archive, cfg, profile="fox")
    info = explain_archive(archive)
    assert "tesseract_execution_plan" in info
    ep = info.get("tesseract_execution_plan")
    assert ep is not None
    assert "execution_steps" in ep
    assert "cooperation_mode" in ep
    assert "plan_reason" in ep
    text = explain_to_text(info)
    assert "Tesseract Execution Plan" in text
    assert "cooperation_mode" in text


def test_no_regression_archive(tmp_path):
    """Archive create, validate, reconstruct still work."""
    from infold.archive.operations import create_archive, validate_archive, reconstruct_archive
    from infold.cli import load_config

    src = tmp_path / "src"
    src.mkdir()
    (src / "a.py").write_text("x = 1\n")
    cfg = load_config()
    archive = tmp_path / "out.infold"
    create_archive(src, archive, cfg, profile="fox")
    ok, _ = validate_archive(archive, mode="strict")
    assert ok
    out = tmp_path / "restored"
    reconstruct_archive(archive, out)
    assert (out / "a.py").read_text() == "x = 1\n"
