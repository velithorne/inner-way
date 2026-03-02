"""Tests for artifact injection."""

import pytest
import numpy as np
from falcor.measurement.artifacts.artifact_library import inject_artifact, ArtifactType
from falcor.measurement.artifacts.detectors import detect_artifacts, artifact_likelihood


def test_inject_thermal_drift():
    ts = np.zeros(100)
    out = inject_artifact(ts, ArtifactType.THERMAL_DRIFT, {"drift_rate": 0.01}, seed=42)
    assert not np.allclose(out, ts)
    assert out[-1] > out[0]


def test_inject_artifact_deterministic():
    ts = np.ones(50)
    out1 = inject_artifact(ts, ArtifactType.EMI_BURST, {"amplitude": 1.0}, seed=123)
    out2 = inject_artifact(ts, ArtifactType.EMI_BURST, {"amplitude": 1.0}, seed=123)
    np.testing.assert_array_almost_equal(out1, out2)


def test_detect_artifacts():
    ts = np.ones(100)
    ts[50] = 100.0  # Single large spike
    segments = detect_artifacts(ts, threshold_std=2.0)
    assert len(segments) >= 1


def test_artifact_likelihood():
    ts = np.random.randn(100)
    lik = artifact_likelihood(ts)
    assert 0 <= lik <= 1
