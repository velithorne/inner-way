"""Tests for manifest creation."""

import pytest
from falcor.core.manifest import create_manifest


def test_create_manifest():
    m = create_manifest("run123", {"seed": 42}, {"name": "test"})
    assert m.run_id == "run123"
    assert m.config_snapshot["seed"] == 42
    assert m.device_assembly["name"] == "test"
    assert len(m.manifest_hash) > 0


def test_manifest_hash_deterministic():
    m1 = create_manifest("r1", {"a": 1}, None)
    m2 = create_manifest("r1", {"a": 1}, None)
    assert m1.manifest_hash == m2.manifest_hash


def test_manifest_hash_changes_with_config():
    m1 = create_manifest("r1", {"a": 1}, None)
    m2 = create_manifest("r1", {"a": 2}, None)
    assert m1.manifest_hash != m2.manifest_hash
