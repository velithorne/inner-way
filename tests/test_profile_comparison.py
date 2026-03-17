"""Tests for Phase 6B — Profile comparison benchmark."""

from pathlib import Path

import pytest

from infold.benchmark.profile_comparison import (
    get_profile_category,
    run_profile_comparison,
    build_comparison_summary,
    compute_auto_vs_best,
    compute_profile_wins,
    export_comparison_csv,
    export_comparison_markdown,
    export_auto_vs_best_csv,
)
from infold.cli import load_config


def test_get_profile_category():
    """Profile category mapping is deterministic."""
    assert get_profile_category("template-heavy") == "tiny"
    assert get_profile_category("infold-workspace") == "large_structured"
    assert get_profile_category("byte-fold-opaque") == "opaque_heavy"
    assert get_profile_category("unknown-dataset") == "balanced"


def test_profile_comparison_deterministic(tmp_path):
    """Profile comparison produces deterministic output."""
    config = load_config()
    base = Path(__file__).parent.parent
    dup = base / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("duplicate_python fixture not found")
    datasets = [(dup, "duplicate-heavy-python", "correctness")]
    comparison = run_profile_comparison(datasets, config, base, create_archives=False)
    assert "datasets" in comparison
    assert len(comparison["datasets"]) == 1
    ds = comparison["datasets"][0]
    assert "by_profile" in ds
    profiles = set(ds["by_profile"].keys())
    assert "auto" in profiles
    assert "sparrow" in profiles
    assert "fox" in profiles
    assert "dragon" in profiles
    assert "golem" in profiles
    assert "serpent" in profiles


def test_auto_vs_best_summary():
    """Auto-vs-best summary has expected structure."""
    comparison = {
        "datasets": [
            {
                "dataset_id": "test",
                "profile_category": "balanced",
                "by_profile": {
                    "auto": {"infold_physical_folded_size": 100, "infold_logical_gain": 50, "fold_profile": "fox", "exact_reconstruction_status": "ok"},
                    "fox": {"infold_physical_folded_size": 100, "infold_logical_gain": 50, "exact_reconstruction_status": "ok"},
                    "sparrow": {"infold_physical_folded_size": 100, "infold_logical_gain": 50, "exact_reconstruction_status": "ok"},
                    "dragon": {"infold_physical_folded_size": 100, "infold_logical_gain": 50, "exact_reconstruction_status": "ok"},
                    "golem": {"infold_physical_folded_size": 100, "infold_logical_gain": 50, "exact_reconstruction_status": "ok"},
                    "serpent": {"infold_physical_folded_size": 100, "infold_logical_gain": 50, "exact_reconstruction_status": "ok"},
                },
            },
        ],
    }
    avb = compute_auto_vs_best(comparison)
    assert "auto_vs_best" in avb
    assert len(avb["auto_vs_best"]) == 1
    row = avb["auto_vs_best"][0]
    assert "auto_selected" in row
    assert "best_physical_profile" in row
    assert "best_logical_gain_profile" in row


def test_profile_wins():
    """Profile wins computation."""
    comparison = {
        "datasets": [
            {
                "dataset_id": "a",
                "profile_category": "tiny",
                "by_profile": {
                    "sparrow": {"infold_physical_folded_size": 100, "infold_logical_gain": 50, "exact_reconstruction_status": "ok"},
                    "dragon": {"infold_physical_folded_size": 100, "infold_logical_gain": 50, "exact_reconstruction_status": "ok"},
                },
            },
        ],
    }
    wins = compute_profile_wins(comparison)
    assert "physical_wins" in wins
    assert "logical_gain_wins" in wins


def test_export_comparison_csv(tmp_path):
    """Export comparison CSV writes valid file."""
    summary = {
        "comparison": {
            "datasets": [
                {
                    "dataset_id": "test",
                    "profile_category": "balanced",
                    "by_profile": {
                        "auto": {"infold_physical_folded_size": 100, "infold_logical_gain": 50, "fold_count": 1, "exact_reconstruction_status": "ok", "fold_profile": "fox"},
                        "fox": {"infold_physical_folded_size": 100, "infold_logical_gain": 50, "fold_count": 1, "exact_reconstruction_status": "ok"},
                    },
                },
            ],
        },
    }
    out = tmp_path / "out.csv"
    export_comparison_csv(summary, out)
    assert out.exists()
    content = out.read_text()
    assert "dataset_id" in content
    assert "profile" in content


def test_export_auto_vs_best_csv(tmp_path):
    """Export auto-vs-best CSV writes valid file."""
    summary = {
        "auto_vs_best": {
            "auto_vs_best": [
                {"dataset_id": "test", "profile_category": "balanced", "auto_selected": "fox", "auto_reason": "balanced", "best_physical_profile": "fox", "best_logical_gain_profile": "fox"},
            ],
        },
    }
    out = tmp_path / "avb.csv"
    export_auto_vs_best_csv(summary, out)
    assert out.exists()
    content = out.read_text()
    assert "auto_selected" in content


def test_export_comparison_markdown(tmp_path):
    """Export comparison markdown writes valid file."""
    summary = {
        "comparison": {"datasets": []},
        "auto_vs_best": {"auto_vs_best": []},
        "profile_wins": {"physical_wins": {}, "logical_gain_wins": {}},
        "best_by_category": {},
        "summary": {"total_datasets": 0, "auto_matched_physical_pct": 0, "auto_matched_gain_pct": 0},
    }
    out = tmp_path / "out.md"
    export_comparison_markdown(summary, out)
    assert out.exists()
    content = out.read_text()
    assert "Profile Comparison" in content


def test_build_comparison_summary():
    """Full summary has all sections."""
    comparison = {
        "datasets": [
            {
                "dataset_id": "test",
                "profile_category": "tiny",
                "by_profile": {
                    "auto": {"infold_physical_folded_size": 100, "infold_logical_gain": 50, "fold_profile": "sparrow", "exact_reconstruction_status": "ok"},
                    "sparrow": {"infold_physical_folded_size": 100, "infold_logical_gain": 50, "exact_reconstruction_status": "ok"},
                    "fox": {"infold_physical_folded_size": 100, "infold_logical_gain": 50, "exact_reconstruction_status": "ok"},
                    "dragon": {"infold_physical_folded_size": 100, "infold_logical_gain": 50, "exact_reconstruction_status": "ok"},
                    "golem": {"infold_physical_folded_size": 100, "infold_logical_gain": 50, "exact_reconstruction_status": "ok"},
                    "serpent": {"infold_physical_folded_size": 100, "infold_logical_gain": 50, "exact_reconstruction_status": "ok"},
                },
            },
        ],
    }
    summary = build_comparison_summary(comparison)
    assert "comparison" in summary
    assert "auto_vs_best" in summary
    assert "profile_wins" in summary
    assert "best_by_category" in summary
    assert "summary" in summary
    s = summary["summary"]
    assert "total_datasets" in s
    assert "auto_matched_physical_pct" in s


def test_profile_comparison_no_regression(tmp_path):
    """Profile comparison produces valid archives for all profiles."""
    config = load_config()
    base = Path(__file__).parent.parent
    dup = base / "tests" / "fixtures" / "duplicate_python"
    if not dup.exists():
        pytest.skip("duplicate_python fixture not found")
    datasets = [(dup, "duplicate-heavy-python", "correctness")]
    comparison = run_profile_comparison(datasets, config, base, create_archives=True)
    for ds in comparison["datasets"]:
        for prof, res in ds.get("by_profile", {}).items():
            assert res.get("exact_reconstruction_status") == "ok", f"{ds['dataset_id']}.{prof}"
