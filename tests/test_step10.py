"""Step 10 verification tests: sweep datasets, invariants, template skeleton."""

import json
from pathlib import Path

import pytest

# Add project root
import sys
sys.path.insert(0, str(Path(__file__).parent.parent))

from infold.engine import run_fold
from infold.reporting import export_report, run_benchmark


def load_config():
    config_path = Path(__file__).parent.parent / "infold" / "config.json"
    with open(config_path, encoding="utf-8") as f:
        return json.load(f)


@pytest.fixture
def config():
    cfg = load_config()
    cfg["project"] = cfg.get("project", {}).copy()
    cfg["project"]["exclude_patterns"] = cfg["project"].get("exclude_patterns", []) + ["infold_sweep_report"]
    return cfg


def test_duplicate_heavy(config):
    """Duplicate-heavy Python: exact repetition should find and fold duplicates."""
    path = Path(__file__).parent / "fixtures" / "duplicate_python"
    if not path.exists():
        pytest.skip("fixtures not found")
    result = run_fold(path, config)
    assert result.ledger.total_folds >= 1
    assert result.ledger.total_bytes_saved > 0
    assert result.exact_reconstruction_ok


def test_template_heavy(config):
    """Template-heavy: template skeleton should find and fold template family."""
    path = Path(__file__).parent / "fixtures" / "template_heavy"
    if not path.exists():
        pytest.skip("fixtures not found")
    result = run_fold(path, config)
    assert result.ledger.total_folds >= 1
    assert result.ledger.total_bytes_saved > 0
    assert result.exact_reconstruction_ok


def test_engine_invariant_candidate_to_ledger(config):
    """Candidate -> validate -> apply -> ledger -> report."""
    path = Path(__file__).parent / "fixtures" / "duplicate_python"
    if not path.exists():
        pytest.skip("fixtures not found")
    result = run_fold(path, config)
    assert result.ledger.total_folds >= 0
    assert result.exact_reconstruction_ok
    json_path, text_path = export_report(result, config, output_dir=path / "test_report")
    assert Path(json_path).exists()
    assert Path(text_path).exists()


def test_logical_vs_physical_in_report(config):
    """Report distinguishes logical gain vs physical folded size."""
    path = Path(__file__).parent / "fixtures" / "duplicate_python"
    if not path.exists():
        pytest.skip("fixtures not found")
    result = run_fold(path, config)
    bench = run_benchmark(result, config)
    assert "raw_bytes" in bench
    assert "bytes_saved" in bench or "logical_gain_bytes" in bench
    assert "folded_bytes" in bench or "physical_folded_size_bytes" in bench
