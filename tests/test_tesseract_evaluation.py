"""
Phase 10D: Tests for Tesseract Evaluation and Tuning.
"""

from pathlib import Path

import pytest

from infold.benchmark.tesseract_evaluation import (
    MODE_BASELINE,
    MODE_PLANNER_ONLY,
    MODE_PLANNER_COOPERATION,
    get_tesseract_category,
    _config_for_mode,
    run_tesseract_comparison,
    compute_cooperation_wins,
    build_recommendation,
    run_threshold_tuning,
    export_evaluation_csv,
    export_evaluation_markdown,
)


def test_get_tesseract_category():
    """Tesseract category mapping is deterministic."""
    assert get_tesseract_category("template-heavy") == "structure_heavy"
    assert get_tesseract_category("config-heavy") == "metadata_heavy"
    assert get_tesseract_category("byte-fold-opaque") == "byte_heavy"
    assert get_tesseract_category("byte-fold-version-like") == "version_like"
    assert get_tesseract_category("infold-workspace") == "realistic_large"
    assert get_tesseract_category("unknown-dataset") == "balanced"


def test_config_for_mode():
    """Mode config sets Tesseract flags correctly."""
    base = {"foo": 1}
    # Baseline: no Tesseract
    cfg = _config_for_mode(MODE_BASELINE, base)
    assert cfg["_tesseract_planner"] is False
    assert cfg["_tesseract_cooperation"] is False
    assert cfg["_fold_profile"] == "fox"
    # Planner only
    cfg = _config_for_mode(MODE_PLANNER_ONLY, base)
    assert cfg["_tesseract_planner"] is True
    assert cfg["_tesseract_cooperation"] is False
    # Cooperation
    cfg = _config_for_mode(MODE_PLANNER_COOPERATION, base)
    assert cfg["_tesseract_planner"] is True
    assert cfg["_tesseract_cooperation"] is True


def test_run_tesseract_comparison_small(tmp_path):
    """Comparison runs on small dataset set and returns expected structure."""
    from infold.benchmark.pack import ensure_stress_datasets, get_benchmark_datasets

    base = Path(__file__).parent.parent
    ensure_stress_datasets(base)
    datasets = get_benchmark_datasets(base)[:2]
    if not datasets:
        pytest.skip("No benchmark datasets")
    config = {"project": {"source_path": "."}}
    comparison = run_tesseract_comparison(
        datasets, config, base, create_archives=True
    )
    assert "datasets" in comparison
    assert "modes" in comparison
    assert comparison["modes"] == [MODE_BASELINE, MODE_PLANNER_ONLY, MODE_PLANNER_COOPERATION]
    for ds in comparison["datasets"]:
        assert "dataset_id" in ds
        assert "by_mode" in ds
        for mode in comparison["modes"]:
            assert mode in ds["by_mode"]
            m = ds["by_mode"][mode]
            assert "infold_physical_folded_size" in m or "archive_size_bytes" in m
            assert "fold_count" in m


def test_compute_cooperation_wins():
    """Cooperation wins computed from comparison."""
    comparison = {
        "datasets": [
            {
                "dataset_id": "a",
                "tesseract_category": "structure_heavy",
                "by_mode": {
                    MODE_BASELINE: {"archive_size_bytes": 1000},
                    MODE_PLANNER_COOPERATION: {"archive_size_bytes": 900},
                },
            },
            {
                "dataset_id": "b",
                "tesseract_category": "byte_heavy",
                "by_mode": {
                    MODE_BASELINE: {"archive_size_bytes": 500},
                    MODE_PLANNER_COOPERATION: {"archive_size_bytes": 510},
                },
            },
        ],
    }
    wins = compute_cooperation_wins(comparison)
    assert wins["cooperation_improved_count"] == 1
    assert wins["cooperation_hurt_count"] == 1
    assert wins["total_bytes_saved_by_cooperation"] == 100
    assert wins["total_bytes_lost_by_cooperation"] == 10
    assert "by_category" in wins


def test_build_recommendation():
    """Recommendation built from wins."""
    comparison = {"datasets": []}
    cooperation_wins = {
        "cooperation_improved_count": 2,
        "cooperation_hurt_count": 1,
        "total_bytes_saved_by_cooperation": 200,
        "total_bytes_lost_by_cooperation": 50,
    }
    rec = build_recommendation(comparison, cooperation_wins)
    assert rec["structural_metadata_cooperation_worthwhile"] is True
    assert rec["byte_metadata_cooperation_worthwhile"] is True
    assert rec["structure_byte_should_remain_deferred"] is True
    assert "cooperation_should_remain_enabled_by_default" in rec


def test_export_evaluation_csv(tmp_path):
    """CSV export produces valid file."""
    comparison = {
        "datasets": [
            {
                "dataset_id": "x",
                "tesseract_category": "balanced",
                "by_mode": {
                    MODE_BASELINE: {"archive_size_bytes": 100, "fold_count": 1},
                    MODE_PLANNER_ONLY: {"archive_size_bytes": 95, "fold_count": 1},
                    MODE_PLANNER_COOPERATION: {"archive_size_bytes": 90, "fold_count": 1},
                },
            },
        ],
    }
    out = tmp_path / "eval.csv"
    export_evaluation_csv(comparison, out)
    assert out.exists()
    content = out.read_text()
    assert "dataset_id" in content
    assert "mode" in content
    assert "x" in content


def test_export_evaluation_markdown():
    """Markdown export produces readable summary."""
    comparison = {
        "datasets": [
            {
                "dataset_id": "y",
                "tesseract_category": "structure_heavy",
                "by_mode": {
                    MODE_BASELINE: {"archive_size_bytes": 200},
                    MODE_PLANNER_ONLY: {"archive_size_bytes": 190},
                    MODE_PLANNER_COOPERATION: {"archive_size_bytes": 185},
                },
            },
        ],
    }
    cooperation_wins = {
        "cooperation_improved_count": 1,
        "cooperation_hurt_count": 0,
        "cooperation_neutral_count": 0,
        "total_bytes_saved_by_cooperation": 15,
        "total_bytes_lost_by_cooperation": 0,
    }
    recommendation = {
        "structural_metadata_cooperation_worthwhile": True,
        "byte_metadata_cooperation_worthwhile": True,
        "current_thresholds_adequate": True,
        "structure_byte_should_remain_deferred": True,
        "cooperation_should_remain_enabled_by_default": True,
    }
    md = export_evaluation_markdown(comparison, cooperation_wins, recommendation)
    assert "Tesseract Evaluation" in md
    assert "Cooperation Win Summary" in md
    assert "Recommendation" in md
    assert "improved" in md or "datasets" in md


def test_run_threshold_tuning_small(tmp_path):
    """Threshold tuning runs with minimal param set."""
    from infold.benchmark.pack import ensure_stress_datasets, get_benchmark_datasets

    base = Path(__file__).parent.parent
    ensure_stress_datasets(base)
    datasets = get_benchmark_datasets(base)[:1]
    if not datasets:
        pytest.skip("No benchmark datasets")
    config = {"project": {"source_path": "."}}
    tuning = run_threshold_tuning(
        datasets,
        config,
        base,
        metadata_strength_values=(0.25,),
        metadata_lower_by_values=(8,),
        compactness_bias_values=(0.03,),
    )
    assert "tuning_results" in tuning
    assert len(tuning["tuning_results"]) >= 1
