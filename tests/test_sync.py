"""Tests for Infold Sync v0.1."""

import json
from pathlib import Path

import pytest

from infold.sync import (
    init_sync,
    add_snapshot,
    create_and_add_snapshot,
    list_snapshots,
    sync_compare,
    sync_report,
    sync_report_to_text,
    sync_list_to_text,
)
from infold.archive import create_archive
from infold.cli import load_config


@pytest.fixture
def sync_dir(tmp_path):
    """Initialize sync directory."""
    dup = Path(__file__).parent.parent / "tests" / "fixtures" / "duplicate_python"
    init_sync(tmp_path / ".sync", source_path=dup)
    return tmp_path / ".sync"


@pytest.fixture
def sync_with_snapshots(sync_dir, tmp_path):
    """Sync dir with two snapshots."""
    config = load_config()
    dup = Path(__file__).parent.parent / "tests" / "fixtures" / "duplicate_python"
    tmpl = Path(__file__).parent.parent / "tests" / "fixtures" / "template_heavy"
    create_and_add_snapshot(sync_dir, dup, config=config)
    create_and_add_snapshot(sync_dir, tmpl, snapshot_id="v2", config=config)
    return sync_dir


def test_sync_init(sync_dir):
    """init_sync creates lineage and snapshots dir."""
    assert (sync_dir / "lineage.json").exists()
    assert (sync_dir / "snapshots").is_dir()
    lineage = json.loads((sync_dir / "lineage.json").read_text())
    assert "snapshots" in lineage
    assert lineage["snapshots"] == []


def test_sync_add_snapshot(sync_dir, tmp_path):
    """add_snapshot registers archive in lineage."""
    config = load_config()
    dup = Path(__file__).parent.parent / "tests" / "fixtures" / "duplicate_python"
    r = create_and_add_snapshot(sync_dir, dup, config=config)
    assert "snapshot" in r
    assert r["snapshot"]["id"] == "v1"
    assert r["snapshot"]["fold_count"] >= 1
    lineage = json.loads((sync_dir / "lineage.json").read_text())
    assert len(lineage["snapshots"]) == 1


def test_sync_list(sync_with_snapshots):
    """list_snapshots returns all snapshots."""
    r = list_snapshots(sync_with_snapshots)
    assert r["snapshot_count"] == 2
    assert len(r["snapshots"]) == 2


def test_sync_compare(sync_with_snapshots):
    """sync_compare compares two snapshots."""
    snaps = list_snapshots(sync_with_snapshots)["snapshots"]
    r = sync_compare(snaps[0]["path"], snaps[1]["path"])
    assert "archive_a" in r
    assert "archive_b" in r
    assert "logical_gain" in r
    assert "template_families_changed" in r


def test_sync_report(sync_with_snapshots):
    """sync_report returns added/removed/changed."""
    snaps = list_snapshots(sync_with_snapshots)["snapshots"]
    r = sync_report(snaps[0]["path"], snaps[1]["path"])
    assert "added" in r
    assert "removed" in r
    assert "template_families" in r["added"]
    text = sync_report_to_text(r)
    assert "Sync Report" in text
    assert "Added:" in text


def test_sync_list_to_text(sync_with_snapshots):
    """sync_list_to_text produces readable output."""
    r = list_snapshots(sync_with_snapshots)
    text = sync_list_to_text(r)
    assert "Sync Snapshots" in text
    assert "v1" in text or "v2" in text


def test_sync_reconstruct(sync_with_snapshots, tmp_path):
    """Reconstruct snapshot (via archive reconstruct)."""
    from infold.archive import reconstruct_archive
    snaps = list_snapshots(sync_with_snapshots)["snapshots"]
    out = tmp_path / "restored"
    reconstruct_archive(snaps[0]["path"], out)
    assert out.exists()
    assert any(out.iterdir())
