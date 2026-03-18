"""Tests for Phase 10B Tesseract Planner v0.1."""

import pytest

from infold.tesseract.planner import (
    compute_dimension_strengths,
    compute_tesseract_planner_profile,
    tesseract_planner_summary,
    OPERATOR_TO_FAMILY,
    TESSERACT_ROUTES,
)


def test_dimension_strengths_deterministic():
    """Dimension strengths are deterministic."""
    signals = {
        "structured_ratio": 0.7,
        "parser_confidence_mean": 0.8,
        "opaque_ratio": 0.2,
        "chunk_reuse_potential": 0.3,
        "metadata_overhead_pressure": 0.4,
        "file_count": 50,
        "lineage_context": False,
        "duplicate_density": 0.2,
        "template_density": 0.3,
        "hierarchy_density": 0.1,
        "dependency_density": 0.2,
    }
    s1 = compute_dimension_strengths(signals)
    s2 = compute_dimension_strengths(signals)
    assert s1 == s2
    for k, v in s1.items():
        assert 0 <= v <= 1


def test_dimension_strengths_lineage():
    """Lineage context increases time_strength."""
    base = {"structured_ratio": 0.5, "opaque_ratio": 0.2, "file_count": 10, "parser_confidence_mean": 0.6}
    s_no = compute_dimension_strengths({**base, "lineage_context": False})
    s_yes = compute_dimension_strengths({**base, "lineage_context": True})
    assert s_yes["time_strength"] > s_no["time_strength"]


def test_dominant_secondary_selection():
    """Dominant and secondary dimension selected correctly."""
    signals = {"structured_ratio": 0.9, "opaque_ratio": 0.1, "parser_confidence_mean": 0.9, "file_count": 5}
    profile = compute_tesseract_planner_profile(signals)
    assert profile["dominant_dimension"] == "structure"
    assert profile["secondary_dimension"] in ("byte", "metadata", "time")


def test_route_structural_first():
    """High structure, low byte -> structural_first."""
    signals = {
        "structured_ratio": 0.9,
        "opaque_ratio": 0.05,
        "parser_confidence_mean": 0.9,
        "duplicate_density": 0.4,
        "template_density": 0.4,
        "hierarchy_density": 0.2,
        "dependency_density": 0.2,
        "file_count": 20,
        "lineage_context": False,
        "metadata_overhead_pressure": 0.2,
    }
    profile = compute_tesseract_planner_profile(signals)
    assert profile["route"] == "structural_first"
    assert profile["route_reason"] == "structure_dominant"


def test_route_byte_first():
    """High byte, low structure -> byte_first."""
    signals = {
        "structured_ratio": 0.2,
        "opaque_ratio": 0.8,
        "chunk_reuse_potential": 0.7,
        "parser_confidence_mean": 0.3,
        "file_count": 30,
        "lineage_context": False,
    }
    profile = compute_tesseract_planner_profile(signals)
    assert profile["route"] == "byte_first"
    assert profile["route_reason"] == "byte_dominant"


def test_route_lineage_sensitive():
    """Lineage context -> lineage_sensitive."""
    signals = {
        "structured_ratio": 0.5,
        "opaque_ratio": 0.3,
        "file_count": 10,
        "lineage_context": True,
    }
    profile = compute_tesseract_planner_profile(signals)
    assert profile["route"] == "lineage_sensitive"


def test_operator_family_priority():
    """Operator family priority matches dominant dimension."""
    profile = compute_tesseract_planner_profile({
        "structured_ratio": 0.8,
        "opaque_ratio": 0.1,
        "file_count": 10,
        "lineage_context": False,
    })
    assert "structural" in profile["operator_family_priority"]
    assert profile["operator_family_priority"][0] == "structural"


def test_planner_bias_generation():
    """Planner bias has expected keys and small values."""
    profile = compute_tesseract_planner_profile({
        "structured_ratio": 0.5,
        "metadata_overhead_pressure": 0.6,
        "file_count": 80,
        "lineage_context": False,
    })
    bias = profile.get("planner_bias", {})
    for k in ("favor_physical_size", "favor_logical_gain", "favor_compactness", "favor_lineage_continuity"):
        assert k in bias
        assert 0 <= bias[k] <= 0.2


def test_tesseract_planner_summary():
    """Summary is human-readable."""
    profile = compute_tesseract_planner_profile({
        "structured_ratio": 0.6,
        "opaque_ratio": 0.2,
        "file_count": 20,
        "lineage_context": False,
    })
    s = tesseract_planner_summary(profile)
    assert "route=" in s
    assert "dominant=" in s
    assert "priority=" in s


def test_operator_to_family():
    """OPERATOR_TO_FAMILY covers main operators."""
    assert OPERATOR_TO_FAMILY.get("exact_repetition") == "structural"
    assert OPERATOR_TO_FAMILY.get("byte_fold") == "byte"
    assert OPERATOR_TO_FAMILY.get("template_skeleton") == "structural"


def test_tesseract_planner_in_report(tmp_path):
    """Report includes tesseract_planner when archive created."""
    from infold.archive.operations import create_archive, explain_archive
    from infold.cli import load_config

    src = tmp_path / "src"
    src.mkdir()
    (src / "a.py").write_text("x = 1\n")
    (src / "b.py").write_text("x = 1\n")
    cfg = load_config()
    archive = tmp_path / "out.infold"
    create_archive(src, archive, cfg, profile="fox")
    info = explain_archive(archive)
    assert "tesseract_planner" in info
    tp = info.get("tesseract_planner")
    assert tp is not None
    assert "route" in tp
    assert "dominant_dimension" in tp
    assert tp["route"] in TESSERACT_ROUTES


def test_no_regression_archive_create(tmp_path):
    """Archive create still works with tesseract planner."""
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
