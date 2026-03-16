"""Tests for blind trials."""

import pytest
import tempfile
from pathlib import Path
from falcor.measurement.pipelines.blind_trials import BlindTrialRunner, SealedTruth


def test_randomize_conditions():
    runner = BlindTrialRunner(seed=42)
    truth = runner.randomize_conditions(5, ["OFF", "ON"], inject_anomaly_frac=0.2)
    assert len(truth.magnet_states) == 5
    assert len(truth.anomaly_injected) == 5


def test_get_condition():
    runner = BlindTrialRunner(seed=42)
    runner.randomize_conditions(3, ["OFF", "ON"])
    c = runner.get_condition_for_trial(0)
    assert "magnet_state" in c
    assert "inject_artifact" in c


def test_save_load_sealed():
    runner = BlindTrialRunner(seed=42)
    runner.randomize_conditions(2, ["OFF", "ON"])
    with tempfile.TemporaryDirectory() as d:
        path = Path(d) / "sealed.json"
        runner.save_sealed(path)
        assert path.exists()
        loaded = runner.load_sealed(path)
        assert loaded.magnet_states == runner._sealed.magnet_states
