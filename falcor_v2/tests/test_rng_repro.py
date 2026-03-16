"""Tests for RNG reproducibility."""

import pytest
import numpy as np
from falcor.core.rng import get_rng, create_run_rng, set_default_seed


def test_get_rng_reproducible():
    r1 = get_rng(42)
    r2 = get_rng(42)
    a1 = r1.random(10)
    a2 = r2.random(10)
    np.testing.assert_array_almost_equal(a1, a2)


def test_create_run_rng_reproducible():
    r1 = create_run_rng("run1", 42)
    r2 = create_run_rng("run1", 42)
    np.testing.assert_array_almost_equal(r1.random(5), r2.random(5))


def test_different_runs_different_sequences():
    r1 = create_run_rng("run1", 42)
    r2 = create_run_rng("run2", 42)
    assert not np.allclose(r1.random(10), r2.random(10))
