"""Tests for Phase 6D Adaptive Origami Creatures."""

from pathlib import Path

import pytest

from infold.profiles.creatures import (
    ADAPT_DELTA_MAX,
    SPECIES_BASE_TRAITS,
    TRAIT_NAMES,
    adapt_traits,
    compute_signals,
    run_creature,
    traits_to_behavior,
)


def test_species_base_traits_exist():
    """All species have base traits."""
    for species in ("sparrow", "fox", "dragon", "golem", "serpent"):
        assert species in SPECIES_BASE_TRAITS
        for t in TRAIT_NAMES:
            assert t in SPECIES_BASE_TRAITS[species]
            v = SPECIES_BASE_TRAITS[species][t]
            assert 0 <= v <= 1


def test_compute_signals_deterministic():
    """Signals are deterministic for same sheet."""
    class MockNode:
        def __init__(self, raw: str, conf: float):
            self.raw_text = raw
            self.parser_confidence = conf

    class MockSheet:
        file_nodes = {
            Path("a.py"): MockNode("x" * 100, 0.9),
            Path("b.py"): MockNode("y" * 50, 0.8),
            Path("c.bin"): MockNode("z" * 200, 0.2),
        }
        metrics = {
            "original_size_bytes": 350,
            "file_count": 3,
            "folder_count": 2,
        }

    s1 = compute_signals(MockSheet(), None)
    s2 = compute_signals(MockSheet(), None)
    assert s1 == s2
    assert s1["file_count"] == 3
    assert s1["structured_ratio"] > 0
    assert s1["opaque_ratio"] > 0
    assert "lineage_context" in s1


def test_compute_signals_lineage_context(tmp_path):
    """Lineage context True when .infold-sync/lineage.json exists."""
    sync_dir = tmp_path / ".infold-sync"
    sync_dir.mkdir()
    (sync_dir / "lineage.json").write_text("{}")
    class MockSheet:
        file_nodes = {Path("a.py"): type("N", (), {"raw_text": "x", "parser_confidence": 0.9})()}
        metrics = {"original_size_bytes": 10, "file_count": 1, "folder_count": 0}

    s = compute_signals(MockSheet(), tmp_path)
    assert s["lineage_context"] is True


def test_adapt_traits_bounded():
    """Adaptation produces traits in [0, 1]."""
    base = SPECIES_BASE_TRAITS["fox"]
    signals = {"opaque_ratio": 0.8, "structured_ratio": 0.5, "template_density": 0.3,
               "hierarchy_density": 0.2, "metadata_overhead_pressure": 0.5,
               "lineage_context": True, "parser_confidence_mean": 0.3, "chunk_reuse_potential": 0.6}
    traits, reasons = adapt_traits("fox", dict(base), signals)
    for k, v in traits.items():
        assert 0 <= v <= 1, f"{k}={v}"
    assert len(reasons) >= 1


def test_adapt_traits_deterministic():
    """Same inputs produce same outputs."""
    base = SPECIES_BASE_TRAITS["golem"]
    signals = {"opaque_ratio": 0.4, "structured_ratio": 0.6, "template_density": 0.2,
               "hierarchy_density": 0.1, "metadata_overhead_pressure": 0.2,
               "lineage_context": False, "parser_confidence_mean": 0.7, "chunk_reuse_potential": 0.3}
    t1, r1 = adapt_traits("golem", dict(base), signals)
    t2, r2 = adapt_traits("golem", dict(base), signals)
    assert t1 == t2
    assert r1 == r2


def test_traits_to_behavior_compact():
    """High compactness produces compact package overrides."""
    b = traits_to_behavior({"compactness_bias": 0.9, "risk_tolerance": 0.5, "metadata_tolerance": 0.5})
    assert b.get("package_export", {}).get("compact") is True


def test_traits_to_behavior_low_compact():
    """Low compactness produces non-compact package."""
    b = traits_to_behavior({"compactness_bias": 0.3, "risk_tolerance": 0.5, "metadata_tolerance": 0.5})
    assert b.get("package_export", {}).get("compact") is False


def test_run_creature_returns_full_info():
    """run_creature returns all required fields."""
    class MockSheet:
        file_nodes = {Path("a.py"): type("N", (), {"raw_text": "x", "parser_confidence": 0.9})()}
        metrics = {"original_size_bytes": 10, "file_count": 1, "folder_count": 0}

    info = run_creature("fox", MockSheet(), None, "manual", "user_selected")
    assert info["fold_species"] == "fox"
    assert info["fold_species_mode"] == "manual"
    assert info["fold_species_reason"] == "user_selected"
    assert "fold_creature_signals" in info
    assert "fold_creature_traits_initial" in info
    assert "fold_creature_traits_final" in info
    assert "fold_creature_adapt_reasons" in info
    assert "fold_creature_behavior_changes" in info
    assert "fold_creature_behavior_overrides" in info


def test_run_creature_deterministic():
    """run_creature is deterministic."""
    class MockSheet:
        file_nodes = {Path("a.py"): type("N", (), {"raw_text": "hello", "parser_confidence": 0.8})()}
        metrics = {"original_size_bytes": 5, "file_count": 1, "folder_count": 0}

    i1 = run_creature("golem", MockSheet(), None, "auto", "test")
    i2 = run_creature("golem", MockSheet(), None, "auto", "test")
    assert i1["fold_creature_traits_final"] == i2["fold_creature_traits_final"]
    assert i1["fold_creature_signals"] == i2["fold_creature_signals"]


def test_creature_in_manifest(tmp_path):
    """Archive manifest includes creature info when adaptive."""
    from infold.archive.operations import create_archive
    from infold.cli import load_config

    cfg = load_config()
    cfg["_fold_profile"] = "fox"
    cfg["_creature_adaptive"] = True
    src = tmp_path / "src"
    src.mkdir()
    (src / "a.py").write_text("x = 1\n")
    (src / "b.py").write_text("x = 1\n")
    out = tmp_path / "out.infold"
    create_archive(src, out, cfg, profile="fox")
    import zipfile
    import json
    with zipfile.ZipFile(out, "r") as zf:
        m = json.loads(zf.read("manifest.json").decode())
    assert "fold_species" in m
    assert m.get("fold_species") == "fox"
    assert "fold_creature_traits_final" in m or "fold_creature_adapt_reasons" in m


def test_creature_disabled_no_info(tmp_path):
    """When _creature_adaptive=False, no creature info in manifest."""
    from infold.archive.operations import create_archive
    from infold.cli import load_config

    cfg = load_config()
    cfg["_fold_profile"] = "fox"
    cfg["_creature_adaptive"] = False
    src = tmp_path / "src"
    src.mkdir()
    (src / "a.py").write_text("x = 1\n")
    out = tmp_path / "out.infold"
    create_archive(src, out, cfg, profile="fox")
    import zipfile
    import json
    with zipfile.ZipFile(out, "r") as zf:
        m = json.loads(zf.read("manifest.json").decode())
    assert "fold_species" not in m
